package lt.indrasius.nbuilder.it

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.http._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, promise}

import scala.util.{Success, Try}

/**
 * Created by mantas on 14.12.22.
 */
abstract class EmbeddedServer(port: Int) { server =>
  implicit val system = ActorSystem()

  type RequestHandler = HttpRequest => HttpResponse

  val host = "localhost"

  def start: Try[Unit] = Try {
    implicit val listener = system.actorOf(Props(Listener), name = "listener")

    IO(Http) ! Http.Bind(listener, interface = host, port = port)

    Await.result(Listener.started, Duration.Inf)

    println(s"Server started on $port")
  }

  def receive: RequestHandler

  private object Listener extends Actor with ActorLogging {
    val idGen = new AtomicInteger()
    private val startPromise = promise[Unit]()

    def started = startPromise.future

    def receive = {
      case c: Http.Connected =>
        val id = idGen.incrementAndGet()
        log.info(s"Connected #$id")

        val handler = context.actorOf(Props(new ConnectionHandler(id, c)))

        sender ! Http.Register(handler)
      case b: Http.Bound =>
        startPromise.complete(Success())
      case b: Http.Unbind => log.info(s"Unbinding")
      case c: Http.ConnectionClosed => log.info("Connection closed")
    }
  }

  private class ConnectionHandler(id: Int, ev: Http.Connected) extends Actor with ActorLogging {
    def receive: Receive = {
      case r: HttpRequest =>
        Try { server.receive.apply(r) } recover {
          case e: MatchError =>
            HttpResponse(StatusCodes.NotFound, HttpEntity("Not found"))
          case e: Throwable =>
            log.error(e, "Internal server error")

            HttpResponse(StatusCodes.InternalServerError, HttpEntity("Internal server error"))
        } map {
          sender ! _
        }
    }
  }

  implicit class RichPath(path: Uri.Path) {
    def relativize(full: Uri.Path): Uri.Path =
      if (full.startsWith(path))
        dropSegments(full: Uri.Path, path.length)
      else
        full

    private def dropSegments(current: Uri.Path, c: Int): Uri.Path =
      if (c > 0)
        dropSegments(current.tail, c - 1)
      else
        current
  }
}

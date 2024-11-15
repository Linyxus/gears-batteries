import gears.async.*
import gears.async.default.given
import util.boundary
import asynchttp.*

val API_KEY = ???

val ANTHROPIC_HEADERS = Seq(
  "x-api-key" -> API_KEY,
  "anthropic-version" -> "2023-06-01",
)

def askClaude(msg: String): Unit =
  val r = requests.post(
    "https://api.anthropic.com/v1/messages",
    headers = ANTHROPIC_HEADERS,
    data = ujson.Obj(
      "model" -> "claude-3-5-sonnet-20241022",
      "max_tokens" -> 1024,
      "messages" -> ujson.Arr(
        ujson.Obj("role" -> "user", "content" -> msg)
      )
    )
  )
  println(r.text())

// def askClaudeStreaming(msg: String) =
//   val r = requests.post.stream(
//     "https://api.anthropic.com/v1/messages",
//     headers = ANTHROPIC_HEADERS,
//     data = ujson.Obj(
//       "model" -> "claude-3-5-sonnet-20241022",
//       "max_tokens" -> 1024,
//       "messages" -> ujson.Arr(
//         ujson.Obj("role" -> "user", "content" -> msg)
//       ),
//       "stream" -> true,
//     )
//   )
//   r

def askClaudeStreaming(msg: String)(using Async.Spawn): SSEClient.Response =
  SSEClient.post(
    "https://api.anthropic.com/v1/messages",
    headers = ANTHROPIC_HEADERS,
    data = ujson.Obj(
      "model" -> "claude-3-5-sonnet-20241022",
      "max_tokens" -> 1024,
      "messages" -> ujson.Arr(
        ujson.Obj("role" -> "user", "content" -> msg)
      ),
      "stream" -> true,
    )
  )

@main def hello(): Unit =
  Async.blocking:
    val resp = askClaudeStreaming("Write two poems about Martin Odersky, each wrapped in a code block")
    println(resp.statusCode())
    def go(): Unit =
      resp.next() match
        case None => println("END")
        case Some(event) =>
          println(event)
          go()
    go()
  // Async.blocking:
  //   val chan = BufferedChannel[String]()
  //   val fut = Future:
  //     r.readBytesThrough: stream =>
  //       val buffer = new Array[Byte](1024)
  //       var bytesRead = stream.read(buffer)
  //       while bytesRead != -1 do
  //         val str = new String(buffer, 0, bytesRead)
  //         //println(s"RECEIVE $str")
  //         chan.send(str)
  //         bytesRead = stream.read(buffer)
  //       chan.close()
  //     println("DONE")
  //   boundary:
  //     while true do
  //       chan.read() match
  //         case Right(msg) =>
  //           println(s"RECEIVE $msg")
  //         case Left(exc) =>
  //           println("CLOSED")
  //           boundary.break()

import gears.async.*
import gears.async.default.given
import util.boundary
import asynchttp.*


val ANTHROPIC_HEADERS = Seq(
  "x-api-key" -> Secrets.API_KEY,
  "anthropic-version" -> "2023-06-01",
)

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
    val resp = askClaudeStreaming("Write two poems about async programming, each wrapped in a code block")
    println(resp.statusCode())
    def go(): Unit =
      resp.next() match
        case Left(err) => println(err)
        case Right(event) =>
          println(event)
          go()
    go()

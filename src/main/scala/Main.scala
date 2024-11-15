import gears.async.*
import gears.async.default.given
import asynchttp.*
import anthropic.*
import pprint.pprintln
import Streaming.Event

@main def hello(): Unit =
  Async.blocking:
    val client = Client(Secrets.API_KEY)
    val resp = client.ask(
      List(
        UserMessage("How to turn Scala into a theorem proving language like Lean 4?"),
        AssistantMessage("Definitely. I'm going to make a detailed plan, starting from the theory to practice:"),
      ),
      maxTokens = 2048,
    )
    def go(): Unit =
      resp.next() match
        case Left(err) =>
          println()
          pprintln(err)
        case Right(event) =>
          event match
            case Event.ContentBlockDelta(text) => print(text)
            case _ =>
          go()
    go()

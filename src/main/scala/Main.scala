import pprint.pprintln
import util.boundary

import gears.async.*
import gears.async.default.given

import anthropic.*
import Streaming.Event
import asynchttp.SSEClient.Failure

import org.jline.reader.{LineReader, LineReaderBuilder, EndOfFileException, UserInterruptException}
import org.jline.terminal.{Terminal, TerminalBuilder}

@main def repl(): Unit =
  val terminal: Terminal = TerminalBuilder.builder().system(true).build()
  val reader: LineReader =
    LineReaderBuilder.builder().terminal(terminal).build()

  println("Claude greets you.")
  val client = Client(Secrets.API_KEY)
  var history: List[Message] = List()

  // the REPL loop
  while true do
    val input = reader.readLine("user> ")
    history = history.appended(UserMessage(input))
    Async.blocking:
      val resp = client.ask(
        history,
        maxTokens = 2048,
      )
      var response: String = ""
      boundary:
        while true do
          resp.next() match
            case Left(err) =>
              err match
                case Failure.Closed => println()
                case e => pprintln(e)
              history = history.appended(AssistantMessage(response))
              boundary.break()
            case Right(Event.ContentBlockDelta(text)) =>
              print(text)
              response += text
            case Right(_) =>

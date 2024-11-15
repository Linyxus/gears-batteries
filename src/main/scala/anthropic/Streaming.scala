package anthropic

import gears.async.*
import gears.async.default.given
import asynchttp.SSEClient

object Streaming:
  enum Event:
    case Ping
    case Error(kind: String, message: String)
    case MessageStart
    case ContentBlockStart
    case ContentBlockDelta(text: String)
    case ContentBlockStop
    case MessageDelta
    case MessageStop

  object Event:
    def fromJson(event: ujson.Value): Event =
      import Event.*
      event("type").str match
        case "ping" => Ping
        case "error" => Error(event("error")("type").str, event("error")("message").str)
        case "message_start" => MessageStart
        case "content_block_start" => ContentBlockStart
        case "content_block_delta" => ContentBlockDelta(event("delta")("text").str)
        case "content_block_stop" => ContentBlockStop
        case "message_delta" => MessageDelta
        case "message_stop" => MessageStop
        case _ => assert(false)

    def fromString(data: String): Event =
      fromJson(ujson.read(data))

  type Res = Either[SSEClient.Failure, Event]

  class Response(private val resp: SSEClient.Response):
    def statusCode()(using Async): Int = resp.statusCode()
    def next()(using Async): Res = resp.next() match
      case Left(failure) => Left(failure)
      case Right(SSEClient.Event(kind, data)) => Right(Event.fromString(data))

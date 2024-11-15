package asynchttp

import gears.async.*
import gears.async.default.given
import requests.RequestBlob
import util.boundary

object SSEClient:
  enum Shard:
    case StatusCode(code: Int)
    case Data(data: String)

  enum Failure:
    case BadStatusCode(code: Int)
    case Closed
    case ParsingError(buffer: String)

  case class Event(kind: String, data: String)

  type Res = Either[Failure, Event]

  class Response(private val chan: Channel[Shard], private val fut: Future[Unit]):
    import Failure.*
    private var _statusCode: Int | Null = null
    private var _buffer: String = ""

    enum ParseResult:
      case NotEnough
      case Invalid
      case Ok(event: Event)

    def parse(): ParseResult =
      var lines = _buffer.linesWithSeparators.toList
      lines = lines.map(_.stripLeading).filter(_.nonEmpty)
      lines match
        case event :: data :: rest if data.endsWith("\n") || data.endsWith("\r") =>
          val eventHeader = "event:"
          val dataHeader = "data:"
          if event.startsWith(eventHeader) && data.startsWith(dataHeader) then
            _buffer = rest.mkString
            ParseResult.Ok(Event(event.drop(eventHeader.length).strip, data.drop(dataHeader.length).strip))
          else ParseResult.Invalid
        case _ => ParseResult.NotEnough

    def statusCode()(using Async): Int =
      if _statusCode != null then _statusCode.nn
      else chan.read() match
        case Left(exc) =>
          _statusCode = -1
          cancel()
          statusCode()
        case Right(Shard.StatusCode(code)) =>
          _statusCode = code
          if !isOk() then cancel()
          statusCode()
        case Right(Shard.Data(data)) =>
         _buffer += data
         _statusCode = 200
         statusCode()

    def cancel(): Unit =
      fut.cancel()
      chan.close()

    def isOk()(using Async): Boolean = statusCode().toString.charAt(0) == '2'

    def next()(using Async): Res =
      if !isOk() then Left(BadStatusCode(_statusCode.nn))
      else parse() match
        case ParseResult.Ok(event) => Right(event)
        case ParseResult.Invalid => Left(ParsingError(_buffer))
        case ParseResult.NotEnough =>
          chan.read() match
            case Left(_) => Left(Closed)
            case Right(Shard.StatusCode(_)) =>
              Left(Closed)
            case Right(Shard.Data(data)) =>
              _buffer += data
              next()

  def post(url: String, headers: Seq[(String, String)], data: RequestBlob)(using Async.Spawn): Response =
    val chan = BufferedChannel[Shard]()
    val fut = Future:
      val r = requests.post.stream(
        url,
        headers = headers,
        data = data,
        onHeadersReceived = header => chan.send(Shard.StatusCode(header.statusCode))
      )
      r.readBytesThrough: stream =>
        val buffer = new Array[Byte](1024)
        var bytesRead = stream.read(buffer)
        while bytesRead != -1 do
          val str = new String(buffer, 0, bytesRead)
          chan.send(Shard.Data(str))
          bytesRead = stream.read(buffer)
        chan.close()
    Response(chan, fut)

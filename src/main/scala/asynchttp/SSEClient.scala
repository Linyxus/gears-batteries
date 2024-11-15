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

  class Response(private val chan: Channel[Shard], private val fut: Future[Unit]):
    private var _statusCode: Int | Null = null
    private var _buffer: String = ""

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

    def next()(using Async): Option[Event] =
      if !isOk() then None
      else if _buffer.nonEmpty then
        val res = _buffer
        _buffer = ""
        Some(Event("test", res))
      else chan.read() match
        case Left(exc) =>
          None
        case Right(Shard.StatusCode(code)) =>
          None
        case Right(Shard.Data(data)) =>
          Some(Event("test", data))

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

package anthropic

import gears.async.*
import gears.async.default.given
import asynchttp.SSEClient

class Client(api_key: String):
  private val API_BASE = "https://api.anthropic.com/v1/messages"

  private def buildHeader = Seq(
    "x-api-key" -> api_key,
    "anthropic-version" -> "2023-06-01",
  )

  def ask(messages: List[Message], model: String = "claude-3-5-sonnet-20241022", maxTokens: Int = 1024)(using Async.Spawn): Streaming.Response =
    val data: ujson.Obj = ujson.Obj(
      "model" -> model,
      "max_tokens" -> maxTokens,
      "messages" -> ujson.Arr(messages.map(_.toJson)*),
      "stream" -> true
    )
    val resp = SSEClient.post(API_BASE, headers = buildHeader, data = data)
    Streaming.Response(resp)

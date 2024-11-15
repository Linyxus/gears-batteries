package anthropic

trait Message:
  def role: String
  def content: String

  def toJson: ujson.Obj = ujson.Obj(
    "role" -> role,
    "content" -> content,
  )

case class UserMessage(content: String) extends Message:
  def role = "user"

case class AssistantMessage(content: String) extends Message:
  def role = "assistant"

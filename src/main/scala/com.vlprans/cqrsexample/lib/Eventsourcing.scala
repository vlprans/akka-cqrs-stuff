package com.vlprans.cqrsexample
package lib

import scalaz._, Scalaz._


trait Command extends Serializable
trait Event extends Serializable


trait ESProcessorDSL {
  type EventSeq = List[Event]
  type EventSeqWriter[Value] = Writer[EventSeq, Value]
  type EventProducer[Value] = EitherT[EventSeqWriter, DomainError, Value]

  implicit class EventProducerOps[Value](ep: EventProducer[Value]) {
    def producing[E <: Event](f: Value => E) =
      ep >>= { (x: Value) => EitherT.right(x.set(f(x) :: Nil) : EventSeqWriter[Value]) }

    def producing[E <: Event](evt: E): EventProducer[Value] = producing(_ => evt)

    def events: EventSeq = ep.run.written
    def value: DomainError \/ Value = ep.run.value
  }

  def accept[Value](x: Value): EventProducer[Value] =
    EitherT.right(x.set(Nil) : EventSeqWriter[Value])

  def reject[Value](error: DomainError): EventProducer[Value] =
    EitherT.left(error.set(Nil) : EventSeqWriter[DomainError])

  def reject[Value](errMsg: String): EventProducer[Value] =
    reject(DomainError(errMsg))
}
object ESProcessorDSL extends ESProcessorDSL


trait ESCommandProcessor extends ESProcessorDSL {
  type CmdHandlerPF = PartialFunction[Command, EventProducer[_]]

  def commandHandler: CmdHandlerPF
  def unknownCmdHandler: CmdHandlerPF = {
    case cmd => reject(s"Unknown command $cmd")
  }

  def handleCommandPF: CmdHandlerPF = commandHandler orElse unknownCmdHandler
}

trait ESEventProcessor extends ESProcessorDSL {
  type EventHandler = PartialFunction[Event, DomainValidation[Any]]

  def eventHandler: EventHandler
  def unknownEventHandler: EventHandler = {
    case ev => DomainError(s"Unknown event $ev").left
  }

  def handleEvent(ev: Event) =
    (eventHandler orElse unknownEventHandler)(ev)
}

package com.vlprans.cqrsexample
package lib

import scalaz._, Scalaz._
import akka.actor._
import akka.persistence.EventsourcedProcessor


trait ESProcessor extends EventsourcedProcessor with ActorLogging {
  this: ESCommandProcessor with ESEventProcessor =>

  def processCommand(cmd: Command) = processPipeline(handleCommandPF(cmd))

  protected def processPipeline[A](prod: EventProducer[A]) = {
    prod.events.foreach { ev => persist(ev)(handleEvent) }
    sender ! prod.value
  }
}

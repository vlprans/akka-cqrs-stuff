package com.vlprans.cqrsexample
package service

import scalaz._, Scalaz._
import akka.actor._
import domain._
import lib._


class InvoiceService extends ESProcessor with ActorLogging
    with InvoiceCmdProcessor with InvoiceEventProcessor {
  override val invoicesRepo = STMRepository[Invoice.Id, Invoice]

  val receiveRecover: Receive = {
    case ev: Event => handleEvent(ev)
  }

  val receiveCommand: Receive = {
    case cmd: Command => processCommand(cmd)
  }
}

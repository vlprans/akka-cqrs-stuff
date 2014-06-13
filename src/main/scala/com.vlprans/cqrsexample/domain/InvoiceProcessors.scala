package com.vlprans.cqrsexample
package domain

import scalaz._, Scalaz._
import lib._


trait InvoiceRepo {
  def invoicesRepo: Repository[Invoice.Id, Invoice]
}

trait InvoiceCmdProcessor extends ESCommandProcessor with InvoiceRepo {
  override val commandHandler: CmdHandler = {
    case CreateInvoice(invoiceId) => invoicesRepo.getOpt(invoiceId) match {
      case Some(_) => reject(s"${invoiceId} already exists")
      case None => take(Invoice.create(invoiceId)) producing InvoiceCreated(invoiceId)
    }
  }
}

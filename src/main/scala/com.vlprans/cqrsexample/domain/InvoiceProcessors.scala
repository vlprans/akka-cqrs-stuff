package com.vlprans.cqrsexample
package domain

import scalaz._, Scalaz._
import lib._


trait InvoiceRepo {
  def invoicesRepo: Repository[Invoice.Id, Invoice]
}


trait InvoiceCmdProcessor extends ESCommandProcessor with InvoiceRepo {
  import InvoiceCmdProcessor._

  override val commandHandler: CmdHandler = {
    case CreateInvoice(invoiceId) => invoicesRepo.getOpt(invoiceId) match {
      case Some(_) => reject(s"${invoiceId} already exists")
      case None => Invoice.create(invoiceId) producing InvoiceCreated(invoiceId)
    }

    case AddInvoiceItem(invoiceId, expectedVersion, item) =>
      onlyDraft(expectedVersion) =<< invoicesRepo.get(invoiceId) producing InvoiceItemAdded(invoiceId, item)
  }

  def onlyDraft(expectedVersion: Option[Long]) = eventProducer[Invoice, Invoice] {
    case i: DraftInvoice => accept(i) >>= requireVersion(expectedVersion)
    case i => reject(notDraftError(i.id))
  }

  def requireVersion(expectedVersion: Option[Long]) = eventProducer[Invoice, Invoice] { invoice =>
    expectedVersion match {
      case Some(exp) if (invoice.version != exp) => reject(invalidVersion(invoice.id, exp, invoice.version))
      case Some(exp) if (invoice.version == exp) => accept(invoice)
      case None => accept(invoice)
    }}
}

trait InvoiceEventProcessor extends ESEventProcessor with InvoiceRepo {
  override val eventHandler: EventHandler = {
    case _ => ().right
  }
}

object InvoiceCmdProcessor {
  private[domain] def invalidVersion(id: Invoice.Id, expected: Long, current: Long) =
    DomainError(s"Invoice ${id}: expected version ${expected} doesn't match current version ${current}")

  private[domain] def notDraftError(id: Invoice.Id) =
    DomainError(s"Invoice ${id}: not a draft invoice")
}

package com.vlprans.cqrsexample
package domain

import scalaz._, Scalaz._
import lib._


sealed abstract class Invoice extends AggregateRoot[Invoice.Id] {
  def version: Long
  def items: List[InvoiceItem]
  def discount: BigDecimal

  def total: BigDecimal = sum - discount

  def sum: BigDecimal = items.foldLeft(BigDecimal(0)) {
    (sum, item) => sum + item.amount * item.count
  }
}

object Invoice extends InvoiceActions {
  sealed trait InvoiceIdTag

  type Id = String @@ InvoiceIdTag
  def Id(id: String) = Tag[String, InvoiceIdTag](id)
}

case class DraftInvoice(
  id: Invoice.Id,
  version: Long = -1,
  items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0) extends Invoice

case class SentInvoice(
  id: Invoice.Id,
  version: Long = -1,
  items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0,
  address: InvoiceAddress) extends Invoice

case class PaidInvoice(
  id: Invoice.Id,
  version: Long = -1,
  items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0,
  address: InvoiceAddress) extends Invoice

case class InvoiceItem(description: String, count: Int, amount: BigDecimal)

case class InvoiceItemVersioned(
  description: String,
  count: Int,
  amount: BigDecimal,
  invoiceVersion: Long = -1)

case class InvoiceAddress(name: String, street: String, city: String, country: String)


trait InvoiceActions extends ESProcessorDSL {
  def create(id: Invoice.Id) = accept(DraftInvoice(id, version = 0L))

  def addItem(item: InvoiceItem) = eventProducer[DraftInvoice, DraftInvoice] { invoice =>
    accept(invoice.copy(version = invoice.version + 1, items = invoice.items :+ item))
  }

  def setDiscount(discount: BigDecimal) = eventProducer[DraftInvoice, DraftInvoice] { invoice =>
    if (invoice.sum <= 100) reject("discount only on orders with sum > 100")
    else accept(invoice.copy(version = invoice.version + 1, discount = discount))
  }

  def sendTo(address: InvoiceAddress) = eventProducer[DraftInvoice, SentInvoice] { invoice =>
    if (invoice.items.isEmpty) reject("cannot send empty invoice")
    else accept(SentInvoice(invoice.id, invoice.version + 1, invoice.items, invoice.discount, address))
  }

  def pay(amount: BigDecimal) = eventProducer[SentInvoice, PaidInvoice] { invoice =>
    if (amount < invoice.total) reject("paid amount less than total amount")
    else accept(PaidInvoice(invoice.id, invoice.version + 1, invoice.items, invoice.discount, invoice.address))
  }
}

// Events
case class InvoiceCreated(invoiceId: Invoice.Id) extends Event
case class InvoiceItemAdded(invoiceId: Invoice.Id, item: InvoiceItem) extends Event
case class InvoiceDiscountSet(invoiceId: Invoice.Id, discount: BigDecimal)
case class InvoiceSent(invoiceId: Invoice.Id, invoice: Invoice, to: InvoiceAddress)
case class InvoicePaid(invoiceId: Invoice.Id)

case class InvoicePaymentRequested(invoiceId: Invoice.Id, amount: BigDecimal, to: InvoiceAddress)
case class InvoicePaymentReceived(invoiceId: Invoice.Id, amount: BigDecimal)

// Commands
case class CreateInvoice(invoiceId: Invoice.Id) extends Command
case class AddInvoiceItem(invoiceId: Invoice.Id, expectedVersion: Option[Long], invoiceItem: InvoiceItem) extends Command
case class SetInvoiceDiscount(invoiceId: Invoice.Id, expectedVersion: Option[Long], discount: BigDecimal) extends Command
case class SendInvoiceTo(invoiceId: Invoice.Id, expectedVersion: Option[Long], to: InvoiceAddress) extends Command

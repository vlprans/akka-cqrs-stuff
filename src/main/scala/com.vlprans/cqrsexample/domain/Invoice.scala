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

object Invoice {
  sealed trait InvoiceIdTag

  type Id = String @@ InvoiceIdTag
  def Id(id: String) = Tag[String, InvoiceIdTag](id)

  def create(id: Invoice.Id): DomainValidation[DraftInvoice] =
    DraftInvoice(id, version = 0L).right
}

case class DraftInvoice(
  id: Invoice.Id,
  version: Long = -1,
  items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0) extends Invoice {

  def addItem(item: InvoiceItem): DomainValidation[DraftInvoice] =
    copy(version = version + 1, items = items :+ item).right

  def setDiscount(discount: BigDecimal): DomainValidation[DraftInvoice] =
    if (sum <= 100) DomainError("discount only on orders with sum > 100").left
    else copy(version = version + 1, discount = discount).right

  def sendTo(address: InvoiceAddress): DomainValidation[SentInvoice] =
    if (items.isEmpty) DomainError("cannot send empty invoice").left
    else SentInvoice(id, version + 1, items, discount, address).right
}

case class SentInvoice(
  id: Invoice.Id,
  version: Long = -1,
  items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0,
  address: InvoiceAddress) extends Invoice {

  def pay(amount: BigDecimal): DomainValidation[PaidInvoice] =
    if (amount < total) DomainError("paid amount less than total amount").left
    else                PaidInvoice(id, version + 1, items, discount, address).right
}

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
case class AddInvoiceItem(invoiceId: Invoice.Id, expectedVersion: Option[Long], invoiceItem: InvoiceItem)
case class SetInvoiceDiscount(invoiceId: Invoice.Id, expectedVersion: Option[Long], discount: BigDecimal)
case class SendInvoiceTo(invoiceId: Invoice.Id, expectedVersion: Option[Long], to: InvoiceAddress)

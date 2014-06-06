package com.vlprans.cqrsexample
package domain

import scalaz._, Scalaz._
import lib._


sealed abstract class Invoice {
  def id: String
  def version: Long
  def items: List[InvoiceItem]
  def discount: BigDecimal

  def total: BigDecimal = sum - discount

  def sum: BigDecimal = items.foldLeft(BigDecimal(0)) {
    (sum, item) => sum + item.amount * item.count
  }
}

case class DraftInvoice(
  id: String,
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
  id: String,
  version: Long = -1,
  items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0,
  address: InvoiceAddress) extends Invoice {

  def pay(amount: BigDecimal): DomainValidation[PaidInvoice] =
    if (amount < total) DomainError("paid amount less than total amount").left
    else                PaidInvoice(id, version + 1, items, discount, address).right
}

case class PaidInvoice(
  id: String,
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
case class InvoiceCreated(invoiceId: String) extends Event
case class InvoiceItemAdded(invoiceId: String, item: InvoiceItem) extends Event
case class InvoiceDiscountSet(invoiceId: String, discount: BigDecimal)
case class InvoiceSent(invoiceId: String, invoice: Invoice, to: InvoiceAddress)
case class InvoicePaid(invoiceId: String)

case class InvoicePaymentRequested(invoiceId: String, amount: BigDecimal, to: InvoiceAddress)
case class InvoicePaymentReceived(invoiceId: String, amount: BigDecimal)

// Commands
case class CreateInvoice(invoiceId: String) extends Command
case class AddInvoiceItem(invoiceId: String, expectedVersion: Option[Long], invoiceItem: InvoiceItem)
case class SetInvoiceDiscount(invoiceId: String, expectedVersion: Option[Long], discount: BigDecimal)
case class SendInvoiceTo(invoiceId: String, expectedVersion: Option[Long], to: InvoiceAddress)

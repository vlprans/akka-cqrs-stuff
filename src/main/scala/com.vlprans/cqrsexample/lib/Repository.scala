package com.vlprans.cqrsexample
package lib


trait Repository[Id, A <: AggregateRoot[Id]] {
  import ESProcessorDSL._

  def get(id: Id): EventProducer[A]
  def getOpt(id: Id): Option[A] = get(id).value.toOption
  def put(x: A): Unit
  def delete(id: Id): Unit // Should perform soft-delete
}

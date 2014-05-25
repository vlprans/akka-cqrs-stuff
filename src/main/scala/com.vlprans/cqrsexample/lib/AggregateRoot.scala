package com.vlprans.cqrsexample
package lib


trait AggregateRoot[Id] {
  def id: Id
}

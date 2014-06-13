package com.vlprans.cqrsexample
package lib

import scala.concurrent.stm.Ref


trait STMRepository[Id, A <: AggregateRoot[Id]] extends Repository[Id, A] {
  import ESProcessorDSL._, STMRepository._

  type DB = Map[Id, A]
  protected val dbRef: Ref[DB]

  private def readDB = dbRef.single.get
  private def updateDB(x: A) =
    dbRef.single.transform(xs => xs + (x.id -> x))

  def get(id: Id) = readDB get id match {
    case Some(x) => accept(x)
    case None => reject(s"No such object ${id}")
  }

  def put(x: A) = updateDB(x)

  def delete(id: Id) = dbRef.single.transform(_ - id)
}

object STMRepository {
  type DB[Id, A <: AggregateRoot[Id]] = Map[Id, A]
  object DB {
    def empty[Id, A <: AggregateRoot[Id]]: DB[Id, A] = Map[Id, A]()
  }

  def apply[Id, A <: AggregateRoot[Id]]: STMRepository[Id, A] =
    this(DB.empty[Id, A])

  def apply[Id, A <: AggregateRoot[Id]](db: DB[Id, A]): STMRepository[Id, A] =
    new STMRepository[Id, A] {
      protected val dbRef = Ref(db)
    }
}

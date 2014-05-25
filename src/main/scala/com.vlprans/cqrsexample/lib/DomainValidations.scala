package com.vlprans.cqrsexample
package lib

import scalaz._, Scalaz._


trait DomainValidations {
  type DomainValidation[+α] = ({type λ[α]= DomainError \/ α})#λ[α]
  type DomainError = List[String]

  object DomainError {
    def apply(msg: String): DomainError = List(msg)
  }
}

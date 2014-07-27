package com.vlprans.cqrsexample
package lib

import scalaz._, Scalaz._


trait DomainValidations {
  type DomainError = List[String]
  type ValidationStatus[+α] = DomainError \/ α

  object DomainError {
    def apply(msg: String): DomainError = List(msg)
  }
}

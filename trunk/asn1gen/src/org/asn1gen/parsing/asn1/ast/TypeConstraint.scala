package org.asn1gen.parsing.asn1.ast

import org.asn1gen.parsing.asn1.ast.kind._

case class TypeConstraint(
  type_ : Type_
) extends Node with SubtypeElementsKind {
}


package org.asn1gen.parsing.asn1.ast

case class SingleTypeConstraint(
  constraint: Constraint
) extends Node with InnerTypeConstraints {
}


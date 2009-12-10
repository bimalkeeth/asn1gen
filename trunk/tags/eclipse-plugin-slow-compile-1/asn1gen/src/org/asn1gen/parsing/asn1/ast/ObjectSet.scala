package org.asn1gen.parsing.asn1.ast

case class ObjectSet(
  objectSetSpec: ObjectSetSpec
) extends Node
  with Setting
  with ActualParameter
  with GovernorConstraintParameterValue {
}

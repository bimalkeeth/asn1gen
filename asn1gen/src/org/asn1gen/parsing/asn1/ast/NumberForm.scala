package org.asn1gen.parsing.asn1.ast

import org.asn1gen.parsing.asn1.ast.kind._

case class NumberForm(
  kind: NumberFormKind
) extends Node
  with ObjIdComponents
  with RelativeOidComponentsKind
{
}

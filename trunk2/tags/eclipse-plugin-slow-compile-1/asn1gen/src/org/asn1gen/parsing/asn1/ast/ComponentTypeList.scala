package org.asn1gen.parsing.asn1.ast

case class ComponentTypeList(
  componentTypes: List[ComponentType]
) extends Node with RootComponentTypeList {
}
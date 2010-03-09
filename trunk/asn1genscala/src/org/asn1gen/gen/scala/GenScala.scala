package org.asn1gen.gen.scala

import java.io.PrintWriter
import org.asn1gen.extra.Extras._
import org.asn1gen.io._
import org.asn1gen.parsing.asn1.{ast => ast}
import scala.collection.immutable.Set

class GenScala(packageName: String, out: IndentWriter) {
  val keywords = Set("yield", "type", "null", "final")
  
  def safeId(id: String): String = {
    if (keywords contains id) {
      return "`" + id + "`"
    } else {
      return id.replaceAll("-", "_")
    }
  }
  
  def generate(module: Module): Unit = {
    out.println("/* This file was generated by asn1gen */")
    out.println()
    out.println("package " + packageName)
    out.println()
    out.println("import org.asn1gen.{runtime => _rt_}")
    out.println("import " + packageName + ".{meta => _meta_}")
    out.println()
    out.println("object " + safeId(module.name) + " {")
    out.indent(2) {
      out.println("import " + packageName + ".meta.{" + safeId(module.name) + " => _meta_}")
      module.imports foreach { symbolsFromModule =>
        out.println("import " + symbolsFromModule.module + "._")
      }
      out.println()
      module.values foreach { value =>
        out.println("/*")
        out.println(value)
        out.println("*/")
      }
      module.types.foreach { case (_, namedType: NamedType) =>
        generate(namedType)
      }
    }
    out.println("}")
  }
  
  def generate(namedType: NamedType): Unit = {
    namedType._type match {
      case ast.Type(builtinType: ast.BuiltinType, _) => {
        generate(builtinType, namedType.name)
      }
      case t@ast.Type(referencedType: ast.ReferencedType, _) => {
        referencedType match {
          case ast.TypeReference(name) => {
            out.ensureEmptyLines(1)
            out.println("type " + safeId(namedType.name) + " = " + safeId(name))
            out.println("lazy val " + safeId(namedType.name) + " = " + safeId(name))
          }
          case _ => {
            out.println("/* referencedType")
            out.println(referencedType)
            out.println("*/")
          }
        }
      }
      case t@ast.Type(_, _) => {
        out.println("/* unknown: " + namedType.name)
        out.println(t)
        out.println("*/")
      }
    }
  }
  
  def generate(builtinType: ast.BuiltinType, assignmentName: String): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    builtinType match {
      case ast.ChoiceType(
        ast.AlternativeTypeLists(rootAlternativeTypeList, _, _, _))
      => {
        out.println(
            "abstract class " + safeAssignmentName +
            "(_element: Any) extends _rt_.AsnChoice {")
        out.indent(2) {
          out.println("def _choice: Int")
          generateSimpleGetters(rootAlternativeTypeList)
          generateChoiceFieldTransformers(assignmentName, rootAlternativeTypeList)
        }
        out.println("}")
        generateChoices(assignmentName, rootAlternativeTypeList)
        val firstNamedType =
          rootAlternativeTypeList.alternativeTypeList.namedTypes(0)
        out.println()
        out.println(
          "object " + safeAssignmentName + " extends " +
          safeId(assignmentName + "_" + firstNamedType.name) +
          "(" + typeNameOf(firstNamedType._type) + ") {")
        out.println("}")
      }
      case ast.SequenceType(ast.Empty) => {
        out.ensureEmptyLines(1)
        out.print("class " + safeAssignmentName + " extends _rt_.AsnSequence {")
        out.indent(2) {
          out.println("override def _desc: _meta_." + safeAssignmentName + " = _meta_." + safeAssignmentName)
          out.println()
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName + " {")
        out.println("}")
      }
      case ast.SequenceType(ast.ComponentTypeLists(list1, extension, list2)) => {
        val list = (list1.toList:::list2.toList).map { componentTypeList =>
          componentTypeList.componentTypes
        }.flatten
        out.ensureEmptyLines(1)
        out.print("class " + safeAssignmentName + "(")
        out.println()
        out.indent(2) {
          generateSequenceFieldDefines(assignmentName, list)
          out.println()
        }
        out.println(") extends _rt_.AsnSequence {")
        out.indent(2) {
          out.println("override def _desc: _meta_." + safeAssignmentName + " = _meta_." + safeAssignmentName)
          out.println()
          out.println("def copy(")
          out.indent(2) {
            out.indent(2) {
              generateSequenceCopyParameters(list)
              out.println(") = {")
            }
            out.print(safeAssignmentName + "(")
            out.println()
            out.indent(2) {
              list1 match {
                case Some(ast.ComponentTypeList(list)) => {
                  generateSequenceFieldValues(assignmentName, list)
                }
                case None => ()
              }
              out.println(")")
            }
          }
          out.println("}")
          out.println()
          out.println("override def equals(that: Any): Boolean = {")
          out.indent(2) {
            out.println("val other = try {")
            out.indent(2) {
              out.println("that.asInstanceOf[" + safeAssignmentName + "]")
            }
            out.println("} catch {")
            out.indent(2) {
              out.println("case e: ClassCastException => return false")
            }
            out.println("}")
            out.println("this.equals(other: " + safeAssignmentName + ")")
          }
          out.println("}")
          out.println()
          out.println("def equals(that: " + safeAssignmentName + "): Boolean = {")
          out.indent(2) {
            list foreach {
              case ast.NamedComponentType(ast.NamedType(ast.Identifier(identifier), _), value) => {
                out.println("if (this." + safeId(identifier) + " != that." + safeId(identifier) + ")")
                out.indent(2) {
                  out.println("return false")
                }
              }
            }
            out.println("return true")
          }
          out.println("}")
          out.println()
          out.println("override def hashCode(): Int = return (")
          out.indent(2) {
            out.println("0")
            list foreach {
              case ast.NamedComponentType(ast.NamedType(ast.Identifier(identifier), _), value) => {
                out.println("^ this." + safeId(identifier) + ".hashCode")
              }
            }
          }
          out.println(")")
          out.println()
          generateSequenceImmutableSetters(assignmentName, list)
          out.println()
          out.println("override def _child(name: String): Any = name match {")
          out.indent(2) {
            list foreach {
              case ast.NamedComponentType(ast.NamedType(ast.Identifier(identifier), _), value) => {
                out.println("case \"" + safeId(identifier) + "\" => " + safeId(identifier))
              }
            }
            out.println("case _ => throw new Exception(\"Member '\" + name + \"' does not exist.\")")
          }
          out.println("}")
        }
        out.println("}")
        out.println()
        out.print("object " + safeAssignmentName + " extends " + safeAssignmentName + "(")
        out.indent(2) {
          out.println()
          var firstItem = true
          list.map {
            case ast.NamedComponentType(
              ast.NamedType(_, _type),
              optionalDefault)
            => {
              if (!firstItem) {
                out.println(",")
              }
              optionalDefault match {
                case ast.Empty => {
                  out.print(safeId(defaultNameOf(_type)))
                }
                case ast.Optional => {
                  out.print("None")
                }
                case ast.Default(value) => {
                  out.print("/* Default(" + value + ") */")
                }
              }
              firstItem = false
            }
          }
          out.println()
        }
        out.println(") {")
        out.indent(2) {
          out.println("def apply(")
          out.indent(2) {
            out.indent(2) {
              generateSequenceFieldParameters(assignmentName, list)
              out.println("): " + safeAssignmentName + " = {")
            }
            out.print("new " + safeAssignmentName +"(")
            out.indent(2) {
              generateSequenceFieldValues(assignmentName, list)
              out.println(")")
            }
          }
          out.println("}")
        }
        out.println("}")
        out.println()
      }
      case ast.EnumeratedType(enumerations)
      => {
        var firstIndex: Option[Long] = None
        out.ensureEmptyLines(1)
        out.println("case class " + safeAssignmentName + "(_value: Long) extends _rt_.AsnEnumeration {")
        out.indent(2) {
          out.println("override def _desc: _meta_." + safeAssignmentName + " = _meta_." + safeAssignmentName)
          out.println
          out.println("override def _shortName: Option[String] = {")
          out.indent(2) {
            out.println("_value match {")
            out.indent(2) {
              enumerations match {
                case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
                => {
                  var index: Long = 0
                  items foreach {
                    case ast.Identifier(item) => {
                      out.println("case " + index + " => Some(" + safeId(item).inspect() + ")")
                      if (firstIndex == None) {
                        firstIndex = Some(index)
                      }
                      index = index + 1
                    }
                    case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                      val number = java.lang.Long.parseLong(n)
                      val value = if (sign) number * -1 else number
                      out.println("case " + value + " => Some(" + safeId(item).inspect() + ")")
                      if (firstIndex == None) {
                        firstIndex = Some(value)
                      }
                      index = index + 1
                    }
                  }
                }
              }
              out.println("case _ => None")
            }
            out.println("}")
          }
          out.println("}")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName + "(" + firstIndex.getOrElse(0) + ") {")
        out.indent(2) {
          enumerations match {
            case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
            => {
              var index = 0
              items foreach {
                case ast.Identifier(item) => {
                  out.print("def " + safeId(item) + ": " + safeId(assignmentName) + " = ")
                  out.println(safeId(assignmentName) + "(" + index + ")")
                  index = index + 1
                }
                case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                  val value = if (sign) n * -1 else n
                  out.print("def " + safeId(item) + ": " + safeId(assignmentName) + " = ")
                  out.println(safeId(assignmentName) + "(" + value + ")")
                  index = index + 1
                }
              }
              extension match {
                case None => {}
                case _ => out.println(extension)
              }
            }
          }
          out.println
          out.println("def of(name: String): " + safeId(assignmentName) + " = {")
          out.indent(2) {
            out.println("name match {")
            out.indent(2) {
              enumerations match {
                case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
                => {
                  var index = 0
                  items foreach {
                    case ast.Identifier(item) => {
                      out.println("case " + safeId(item).inspect + " => " + safeId(item))
                      index = index + 1
                    }
                    case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                      out.println("case " + safeId(item).inspect + " => " + safeId(item))
                      index = index + 1
                    }
                  }
                  extension match {
                    case None => {}
                    case _ => out.println(extension)
                  }
                }
              }
              out.println("case _ => throw _rt_.BadEnumerationException(")
              out.indent(2) {
                out.println("\"Unrecogonised enumeration value + '\" + name + \"'\")")
              }
            }
            out.println("}")
          }
          out.println("}")
          out.println
          out.println("def of(value: Int): " + safeId(assignmentName) + " = {")
          out.indent(2) {
            out.println("value match {")
            out.indent(2) {
              enumerations match {
                case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
                => {
                  var index = 0
                  items foreach {
                    case ast.Identifier(item) => {
                      out.println("case " + index + " => " + safeId(item))
                      index = index + 1
                    }
                    case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                      val value = if (sign) n * -1 else n
                      out.println("case " + value + " => " + safeId(item))
                      index = index + 1
                    }
                  }
                  extension match {
                    case None => {}
                    case _ => out.println(extension)
                  }
                }
              }
              out.println("case _ => " + safeId(assignmentName) + "(value)")
            }
            out.println("}")
          }
          out.println("}")
        }
        out.println("}")
      }
      case setOfType: ast.SetOfType => {
        generate(assignmentName, setOfType)
      }
      case bitStringType: ast.BitStringType => {
        out.ensureEmptyLines(1)
        out.println("type " + safeAssignmentName + " = _rt_.AsnBitString")
        out.println()
        out.println("lazy val " + safeAssignmentName + " = _rt_.AsnBitString")
      }
      case ast.INTEGER(None) => {
        out.ensureEmptyLines(1)
        out.println("type " + safeAssignmentName + " = Long")
        out.println()
        out.println("lazy val " + safeAssignmentName + " = 0L")
      }
      case ast.BOOLEAN => {
        out.ensureEmptyLines(1)
        out.println("type " + safeAssignmentName + " = Boolean")
        out.println()
        out.println("lazy val " + safeAssignmentName + " = false")
      }
      case ast.OctetStringType => {
        out.ensureEmptyLines(1)
        out.println("type " + safeAssignmentName + " = _rt_.AsnOctetString")
        out.println()
        out.println("lazy val " + safeAssignmentName + " = _rt_.AsnOctetString")
      }
      case ast.REAL => {
        out.ensureEmptyLines(1)
        out.println("type " + safeAssignmentName + " = Double")
        out.println()
        out.println("lazy val " + safeAssignmentName + " = 0.0")
      }
      case unmatched => {
        out.ensureEmptyLines(1)
        out.println("// Unmatched " + safeAssignmentName + ": " + unmatched)
      }
    }
  }
  
  def generate(assignmentName: String, setOfType: ast.SetOfType): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    setOfType match {
      case ast.SetOfType(ast.Type(ast.TypeReference(referencedType), _)) => {
        val safeReferenceType = safeId(referencedType)
        out.ensureEmptyLines(1)
        out.print("case class " + safeAssignmentName)
        out.print("(items: List[" + safeReferenceType + "]) ")
        out.println("extends _rt_.AsnList {")
        out.indent(2) {
          out.println("override def _desc: _meta_." + safeAssignmentName + " = _meta_." + safeAssignmentName)
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName + "(Nil) {")
        out.indent(2) {
          out.print("def apply(items: ")
          out.print(safeReferenceType)
          out.print("*): ")
          out.print(safeAssignmentName)
          out.print(" = ")
          out.print(safeAssignmentName)
          out.println("(items.toList)")
        }
        out.println("}")
      }
      case ast.SetOfType(ast.Type(sequenceType: ast.SequenceType, _)) => {
        assert(false)
        out.ensureEmptyLines(1)
        out.println("type " + safeAssignmentName + " = List[" + safeId(assignmentName + "_element") + "]")
        out.println("lazy val " + safeAssignmentName + " = Nil: List[" + safeId(assignmentName + "_element") + "]")
        generate(sequenceType, assignmentName + "_element")
      }
    }
  }
  
  def typeNameOf(namedComponentType: ast.NamedComponentType): String = {
    namedComponentType match {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        typeNameOf(_type, value)
      }
    }
  }
  
  def typeNameOf(_type: ast.Type, value: ast.OptionalDefault[ast.Value]): String = {
    value match {
      case ast.Empty =>
        return typeNameOf(_type)
      case ast.Default(value) =>
        return typeNameOf(_type)
      case ast.Optional =>
        return "Option[" + typeNameOf(_type) + "]"
    }
  }
  
  def defaultNameOf(namedComponentType: ast.NamedComponentType): String = {
    namedComponentType match {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        defaultNameOf(_type, value)
      }
    }
  }
  
  def defaultNameOf(_type: ast.Type, value: ast.OptionalDefault[ast.Value]): String = {
    value match {
      case ast.Empty =>
        return typeNameOf(_type)
      case ast.Default(value) =>
        return defaultNameOf(_type)
      case ast.Optional =>
        return "None"
    }
  }
  
  def generateSequenceFieldDefines(
      sequenceName: String, list: List[ast.ComponentType]): Unit = {
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out.println(",")
        }
        out.print("val " + safeId(identifier) + ": " + safeId(typeNameOf(_type, value)))
        firstTime = false
      }
    }
  }
  
  def generateSequenceFieldParameters(
      sequenceName: String, list: List[ast.ComponentType]): Unit = {
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out.println(",")
        }
        out.print(safeId(identifier) + ": " + safeId(typeNameOf(_type, value)))
        firstTime = false
      }
    }
  }
  
  def generateSequenceFieldValues(
      sequenceName: String, list: List[ast.ComponentType]): Unit = {
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out.println(",")
        }
        out.print(safeId(identifier))
        firstTime = false
      }
    }
  }
  
  def generateSequenceCopyParameters(
      list: List[ast.ComponentType]): Unit = {
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out.println(",")
        }
        out.print(safeId(identifier) + ": " + safeId(typeNameOf(_type, value)) + " = this." + safeId(identifier))
        firstTime = false
      }
    }
  }
  
  def defaultNameOf(_type: ast.Type): String = {
    _type match {
      case ast.Type(typeKind, _) => defaultNameOf(typeKind)
    }
  }
  
  def defaultNameOf(typeKind: ast.TypeKind): String = {
    typeKind match {
      case builtinType: ast.BuiltinType => defaultNameOf(builtinType)
      case ast.TypeReference(reference) => reference
      case unmatched => "UnmatchedDefaultName(" + unmatched + ")"
    }
  }
  
  def defaultNameOf(typeKind: ast.TypeKind, value: ast.OptionalDefault[ast.Value]): String = {
    value match {
      case ast.Empty =>
        return defaultNameOf(typeKind)
      case ast.Default(value) =>
        return defaultNameOf(typeKind)
      case ast.Optional =>
        return "None"
    }
  }
  
  def defaultNameOf(builtinType: ast.BuiltinType): String = {
    builtinType match {
      case ast.BitStringType(_) => {
        return "_rt_.AsnBitString"
      }
      case ast.BOOLEAN => {
        return "false"
      }
      case characterString: ast.CharacterStringType => {
        defaultNameOf(characterString)
      }
      case _: ast.ChoiceType => {
        return "_rt_.AsnChoice"
      }
      case ast.EmbeddedPdvType => {
        return "_rt_.AsnEmbeddedPdv"
      }
      case ast.EnumeratedType(_) => {
        return "_rt_.AsnEnumeration"
      }
      case ast.EXTERNAL => {
        return "ExternalType"
      }
      case ast.InstanceOfType(_) => {
        return "InstanceOfType"
      }
      case ast.INTEGER(_) => {
        return "0L"
      }
      case ast.NULL => {
        return "_rt_.AsnNull"
      }
      case _: ast.ObjectClassFieldType => {
        return "_rt_.AsnObjectClassField"
      }
      case ast.ObjectIdentifierType => {
        return "_rt_.AsnObjectIdentifier"
      }
      case ast.OctetStringType => {
        return "_rt_.AsnOctetString"
      }
      case ast.REAL => {
        return "0.0"
      }
      case ast.RelativeOidType => {
        return "_rt_.AsnRelativeOidType"
      }
      case ast.SequenceOfType(_) => {
        return "_rt_.AsnSequenceOf"
      }
      case ast.SequenceType(_) => {
        return "_rt_.AsnSequence"
      }
      case ast.SetOfType(_) => {
        return "_rt_.AsnSetOf"
      }
      case ast.SetType(_) => {
        return "_rt_.AsnSet"
      }
      case ast.TaggedType(_, _, underlyingType) => {
        return defaultNameOf(underlyingType)
      }
      case unmatched => {
        return "UnknownBuiltinType(" + unmatched + ")"
      }
    }
  }
  
  def defaultNameOf(characterString: ast.CharacterStringType): String = {
    characterString match {
      case ast.BMPString => {
        return "_rt_.AsnBmpString"
      }
      case ast.GeneralString => {
        return "_rt_.AsnGeneralString"
      }
      case ast.GraphicString => {
        return "_rt_.AsnGraphicString"
      }
      case ast.IA5String => {
        return "_rt_.AsnIa5String"
      }
      case ast.ISO646String => {
        return "_rt_.AsnIso646String"
      }
      case ast.NumericString => {
        return "_rt_.AsnNumericString"
      }
      case ast.PrintableString => {
        return "_rt_.AsnPrintableString"
      }
      case ast.T61String => {
        return "_rt_.AsnT61String"
      }
      case ast.TeletexString => {
        return "_rt_.AsnTeletexString"
      }
      case ast.UniversalString => {
        return "_rt_.AsnUniversalString"
      }
      case ast.UTF8String => {
        return "_rt_.AsnUtf8String"
      }
      case ast.VideotexString => {
        return "_rt_.AsnVideotexString"
      }
      case ast.VisibleString => {
        return "_rt_.AsnVisibleString"
      }
      case unknown => {
        return "UnknownCharacterString(" + unknown + ")"
      }
    }
  }
  
  def typeNameOf(_type: ast.Type): String = {
    _type match {
      case ast.Type(typeKind, _) => typeNameOf(typeKind)
    }
  }
  
  def typeNameOf(typeKind: ast.TypeKind): String = {
    typeKind match {
      case builtinType: ast.BuiltinType => typeNameOf(builtinType)
      case ast.TypeReference(reference) => reference
      case unmatched => "Unmatched(" + unmatched + ")"
    }
  }
  
  def typeNameOf(typeKind: ast.TypeKind, value: ast.OptionalDefault[ast.Value]): String = {
    value match {
      case ast.Empty =>
        return typeNameOf(typeKind)
      case ast.Default(value) =>
        return typeNameOf(typeKind)
      case ast.Optional =>
        return "Option[" + typeNameOf(typeKind) + "]"
    }
  }
  
  def typeNameOf(builtinType: ast.BuiltinType): String = {
    builtinType match {
      case ast.BitStringType(_) => {
        return "_rt_.AsnBitString"
      }
      case ast.BOOLEAN => {
        return "Boolean"
      }
      case characterString: ast.CharacterStringType => {
        typeNameOf(characterString)
      }
      case _: ast.ChoiceType => {
        return "_rt_.AsnChoice"
      }
      case ast.EmbeddedPdvType => {
        return "_rt_.AsnEmbeddedPdv"
      }
      case ast.EnumeratedType(_) => {
        return "_rt_.AsnEnumeration"
      }
      case ast.EXTERNAL => {
        return "ExternalType"
      }
      case ast.InstanceOfType(_) => {
        return "InstanceOfType"
      }
      case ast.INTEGER(_) => {
        return "Long"
      }
      case ast.NULL => {
        return "_rt_.AsnNull"
      }
      case _: ast.ObjectClassFieldType => {
        return "_rt_.AsnObjectClassField"
      }
      case ast.ObjectIdentifierType => {
        return "_rt_.AsnObjectIdentifier"
      }
      case ast.OctetStringType => {
        return "_rt_.AsnOctetString"
      }
      case ast.REAL => {
        return "Double"
      }
      case ast.RelativeOidType => {
        return "_rt_.AsnRelativeOidType"
      }
      case ast.SequenceOfType(_) => {
        return "_rt_.AsnSequenceOf"
      }
      case ast.SequenceType(_) => {
        return "_rt_.AsnSequence"
      }
      case ast.SetOfType(_) => {
        return "_rt_.AsnSetOf"
      }
      case ast.SetType(_) => {
        return "_rt_.AsnSet"
      }
      case ast.TaggedType(_, _, underlyingType) => {
        return typeNameOf(underlyingType)
      }
      case unmatched => {
        return "UnknownBuiltinType(" + unmatched + ")"
      }
    }
  }
  
  def typeNameOf(characterString: ast.CharacterStringType): String = {
    characterString match {
      case ast.BMPString => {
        return "_rt_.AsnBmpString"
      }
      case ast.GeneralString => {
        return "_rt_.AsnGeneralString"
      }
      case ast.GraphicString => {
        return "_rt_.AsnGraphicString"
      }
      case ast.IA5String => {
        return "_rt_.AsnIa5String"
      }
      case ast.ISO646String => {
        return "_rt_.AsnIso646String"
      }
      case ast.NumericString => {
        return "_rt_.AsnNumericString"
      }
      case ast.PrintableString => {
        return "_rt_.AsnPrintableString"
      }
      case ast.T61String => {
        return "_rt_.AsnT61String"
      }
      case ast.TeletexString => {
        return "_rt_.AsnTeletexString"
      }
      case ast.UniversalString => {
        return "_rt_.AsnUniversalString"
      }
      case ast.UTF8String => {
        return "_rt_.AsnUtf8String"
      }
      case ast.VideotexString => {
        return "_rt_.AsnVideotexString"
      }
      case ast.VisibleString => {
        return "_rt_.AsnVisibleString"
      }
      case unknown => {
        return "UnknownCharacterString(" + unknown + ")"
      }
    }
  }
  
  def generateSequenceImmutableSetters(sequenceName: String, list: List[ast.ComponentType]): Unit = {
    val fieldNames = list.map {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _),
        _)
      => identifier
    }
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        generateSequenceImmutableSetter(
            sequenceName: String, identifier, _type, value, fieldNames)
      }
    }
  }
  
  def generateSequenceImmutableSetter(
      sequenceName: String,
      fieldName: String,
      _type: ast.Type,
      value: ast.OptionalDefault[ast.Value],
      fieldNames: List[String]): Unit = {
    _type match {
      case ast.Type(ast.TaggedType(_, _, fieldType), _) => {
        generateSequenceImmutableSetter(sequenceName, fieldName, fieldType, value, fieldNames)
        //out.println("// tag " + number)
      }
      case ast.Type(builtinType: ast.TypeKind, List()) => {
        val setterType = typeNameOf(builtinType, value)
        out.println(
            "def " + safeId(fieldName) + "(f: (" + setterType + " => " +
            setterType + ")): " + sequenceName + " =")
        out.indent(2) {
          out.println("this.copy(" + safeId(fieldName) + " = f(this." + safeId(fieldName) + "))")
        }
      }
      case unmatched => {
        out.println("// Unmatched type: " + unmatched)
      }
    }
  }
  
  def generateChoices(
      assignmentName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateChoices(assignmentName, namedType)
        }
      }
    }
  }
  
  def generateChoices(
      assignmentName: String,
      namedType: ast.NamedType): Unit = {
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        ast.Type(
          ast.TaggedType(
            ast.Tag(_, ast.Number(tagNumber)), _, _type),
          _))
      => {
        val safeName = safeId(name)
        val safeElementType = safeId(typeNameOf(_type))
        val safeChoiceType = safeId(assignmentName)
        val safeChoiceChoice = safeId(assignmentName + "_" + name)
        out.ensureEmptyLines(1)
        out.println(
            "case class " + safeChoiceChoice +
            "(_element: " + typeNameOf(_type) + ") extends " + safeId(assignmentName) + "(_element) {")
        out.indent(2) {
          out.print("override def _desc: _meta_.")
          out.print(safeChoiceType)
          out.print(" = _meta_.")
          out.print(safeChoiceType)
          out.println
          out.println("def _choice: Int = " + tagNumber)
          out.println
          out.print("override def ")
          out.print(safeName)
          out.print(": Option[")
          out.print(safeElementType)
          out.print("] = Some(_element)")
          out.println
          out.println
          out.print("override def ")
          out.print(safeName)
          out.print("(f: (")
          out.print(safeElementType)
          out.print(" => ")
          out.print(safeElementType)
          out.print(")): ")
          out.print(safeChoiceType)
          out.print(" = ")
          out.print(safeChoiceChoice)
          out.print("(f(_element))")
          out.println
          out.println
          out.println("override def _choiceName: String = " + name.inspect)
        }
        out.println("}")
      }
    }
  }

  def generateChoiceFieldTransformers(
      choiceTypeName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateChoiceFieldTransformer(choiceTypeName, namedType)
        }
      }
    }
  }

  def generateChoiceFieldTransformer(choiceTypeName: String, namedType: ast.NamedType): Unit = {
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        _type)
      => {
        val safeElementName = safeId(name)
        val safeChoiceType = safeId(choiceTypeName)
        val safeChoiceChoice = safeId(choiceTypeName + "_" + name)
        val safeElementType = safeId(typeNameOf(_type))
        out.println()
        out.print("def ")
        out.print(safeElementName)
        out.print("(f: (")
        out.print(safeElementType)
        out.print(" => ")
        out.print(safeElementType)
        out.print(")): ")
        out.print(safeChoiceType)
        out.print(" = this")
        out.println
        out.println
        out.print("def ")
        out.print(safeElementName)
        out.print("(f: => ")
        out.print(safeElementType)
        out.print("): ")
        out.print(safeChoiceType)
        out.print(" = ")
        out.print(safeChoiceChoice)
        out.print("(f)")
        out.println
      }
    }
  }

  def generateSimpleGetters(rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateSimpleGetters(namedType)
        }
      }
    }
  }
  
  def generateSimpleGetters(namedType: ast.NamedType): Unit = {
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        _type)
      => {
    	val safeName = safeId(name)
    	val safeType = safeId(typeNameOf(_type))
        out.println()
        out.print("def ")
        out.print(safeName)
        out.print(": Option[")
        out.print(safeType)
        out.println("] = None")
      }
    }
  }
}

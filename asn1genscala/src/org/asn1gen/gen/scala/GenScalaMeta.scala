package org.asn1gen.gen.scala

import java.io.PrintWriter
import org.asn1gen.parsing.asn1.{ast => ast}
import org.asn1gen.io._
import scala.collection.immutable.Set

class GenScalaMeta(packageName: String, out: IndentWriter) {
  val keywords = Set("yield", "type", "null")
  
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
    out.println("package " + packageName + ".meta")
    out.println()
    out.println("import org.asn1gen.runtime.{meta => _meta_}")
    out.println()
    out.println("object " + safeId(module.name) + " {")
    out.indent(2) {
      module.imports foreach { symbolsFromModule =>
        out.println("import " + symbolsFromModule.module + "._")
      }
      out.println()
      module.types.foreach { case (_, namedType: NamedType) => generate(namedType) }
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
            val safeAlias = safeId(namedType.name)
            val safeReferent = safeId(name)
            out.println("trait " + safeAlias + " extends " + safeReferent + "{")
            out.indent(2) {
              out.println("override def name: String = \"" + safeAlias + "\"")
            }
            out.println("}")
            out.println()
            out.println("object " + safeAlias + " extends " + safeAlias)
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
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnChoice {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
          out.println()
          out.println("override def children: Map[String, _meta_.AsnMember] = Map(")
          out.indent(2) {
            var firstItem = true
            rootAlternativeTypeList match {
              case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
                namedTypes foreach { case ast.NamedType(ast.Identifier(identifier), _type) =>
                  val safeIdentifier = safeId(identifier)
                  val safeType = safeId(typeNameOf(_type))
                  if (!firstItem) {
                    out.println(",")
                  }
                  out.print(
                      "\"" + safeIdentifier + "\" -> _meta_.AsnChoiceMember(\"" +
                      safeIdentifier + "\", " + safeType + ")")
                  firstItem = false
                }
              }
            }
            out.println(")")
          }
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.SequenceType(ast.Empty) => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnSequence {")
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.SequenceType(ast.ComponentTypeLists(list1, extension, list2)) => {
        val list = (list1.toList:::list2.toList).map { componentTypeList =>
          componentTypeList.componentTypes
        }.flatten
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnSequence {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
          out.println()
          out.println("override def children: Map[String, _meta_.AsnMember] = Map(")
          out.indent(2) {
            var firstItem = true
            list.map {
              case ast.NamedComponentType(
                ast.NamedType(ast.Identifier(identifier), _type), optionalValue)
              => {
                val safeIdentifier = safeId(identifier)
                val safeType = safeId(typeNameOf(_type))
                if (!firstItem) {
                  out.println(",")
                }
                out.print(
                    "\"" + safeIdentifier + "\" -> _meta_.AsnSequenceMember(\"" +
                    safeIdentifier + "\", " +
                    safeType + ", " +
                    (optionalValue == ast.Optional) + ")")
                firstItem = false
              }
            }
            out.println(")")
          }
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.EnumeratedType(enumerations)
      => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnEnumeration {")
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case setOfType: ast.SetOfType => {
        generate(assignmentName, setOfType)
      }
      case bitStringType: ast.BitStringType => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnBitString {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.INTEGER(None) => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnInteger {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.BOOLEAN => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnBoolean {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.OctetStringType => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnOctetString {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.PrintableString => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnPrintableString {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.REAL => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnReal {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case ast.UTF8String => {
        out.println("trait " + safeAssignmentName + " extends _meta_.AsnUtf8String {")
        out.indent(2) {
          out.println("override def name: String = \"" + safeAssignmentName + "\"")
        }
        out.println("}")
        out.println()
        out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
      }
      case unmatched => {
        out.println("// Unmatched " + safeAssignmentName + ": " + unmatched)
      }
    }
  }
  
  def generate(assignmentName: String, setOfType: ast.SetOfType): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    setOfType match {
      case ast.SetOfType(ast.Type(elementType, _)) => {
        elementType match {
          case ast.TypeReference(referencedType) => {
            out.println("trait " + safeAssignmentName + " extends _meta_.AsnList {")
            out.indent(2) {
              out.println("override def name: String = \"" + safeAssignmentName + "\"")
              out.println()
              out.println("override def children: Map[String, _meta_.AsnMember] = Map.empty")
            }
            out.println("}")
            out.println()
            out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
          }
          case sequenceType: ast.SequenceType => {
            assert(false)
          }
          case builtinType: ast.BuiltinType => {
            out.println("trait " + safeAssignmentName + " extends _meta_.AsnList {")
            out.indent(2) {
              out.println("override def name: String = \"" + safeAssignmentName + "\"")
              out.println()
              out.println("override def children: Map[String, _meta_.AsnMember] = Map.empty")
            }
            out.println("}")
            out.println()
            out.println("object " + safeAssignmentName + " extends " + safeAssignmentName)
          }
        }
      }
    }
  }
  
  def generate(assignmentName: String, enumerations: ast.Enumerations): Unit = {
    enumerations match {
      case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
      => {
        var index = 0
        items foreach {
          case ast.Identifier(item) => {
            out.println(
              "def " + safeId(item) + ": " + safeId(assignmentName) +
              " = " + safeId(assignmentName) + "(" + index + ")")
            index = index + 1
          }
          case v@_ => {
            out.println("/* unknown enumeration:")
            out.println(v)
            out.println("*/")
          }
        }
        extension match {
          case None => {}
          case _ => out.println(extension)
        }
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
        return "_meta_.AsnBitString"
      }
      case ast.BOOLEAN => {
        return "_meta_.AsnBoolean"
      }
      case characterString: ast.CharacterStringType => {
        defaultNameOf(characterString)
      }
      case _: ast.ChoiceType => {
        return "_meta_.AsnChoice"
      }
      case ast.EmbeddedPdvType => {
        return "_meta_.AsnEmbeddedPdv"
      }
      case ast.EnumeratedType(_) => {
        return "_meta_.AsnEnumeration"
      }
      case ast.EXTERNAL => {
        return "ExternalType"
      }
      case ast.InstanceOfType(_) => {
        return "InstanceOfType"
      }
      case ast.INTEGER(_) => {
        return "_meta_.AsnInteger"
      }
      case ast.NULL => {
        return "_meta_.AsnNull"
      }
      case _: ast.ObjectClassFieldType => {
        return "_meta_.AsnObjectClassField"
      }
      case ast.ObjectIdentifierType => {
        return "_meta_.AsnObjectIdentifier"
      }
      case ast.OctetStringType => {
        return "_meta_.AsnOctetString"
      }
      case ast.REAL => {
        return "_meta_.AsnReal"
      }
      case ast.RelativeOidType => {
        return "_meta_.AsnRelativeOidType"
      }
      case ast.SequenceOfType(_) => {
        return "_meta_.AsnSequenceOf"
      }
      case ast.SequenceType(_) => {
        return "_meta_.AsnSequence"
      }
      case ast.SetOfType(_) => {
        return "_meta_.AsnSetOf"
      }
      case ast.SetType(_) => {
        return "_meta_.AsnSet"
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
        return "_meta_.AsnBmpString"
      }
      case ast.GeneralString => {
        return "_meta_.AsnGeneralString"
      }
      case ast.GraphicString => {
        return "_meta_.AsnGraphicString"
      }
      case ast.IA5String => {
        return "_meta_.AsnIa5String"
      }
      case ast.ISO646String => {
        return "_meta_.AsnIso646String"
      }
      case ast.NumericString => {
        return "_meta_.AsnNumericString"
      }
      case ast.PrintableString => {
        return "_meta_.AsnPrintableString"
      }
      case ast.T61String => {
        return "_meta_.AsnT61String"
      }
      case ast.TeletexString => {
        return "_meta_.AsnTeletexString"
      }
      case ast.UniversalString => {
        return "_meta_.AsnUniversalString"
      }
      case ast.UTF8String => {
        return "_meta_.AsnUtf8String"
      }
      case ast.VideotexString => {
        return "_meta_.AsnVideotexString"
      }
      case ast.VisibleString => {
        return "_meta_.AsnVisibleString"
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
      case usefulType: ast.UsefulType => typeNameOf(usefulType)
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
  
  def typeNameOf(usefulType: ast.UsefulType): String = {
    usefulType match {
      case ast.GeneralizedTime => {
        return "_meta_.AsnGeneralizedTime"
      }
      case ast.ObjectDescriptor => {
        return "_meta_.AsnObjectDescriptor"
      }
      case ast.UTCTime => {
        return "_meta_.AsnUtcTime"
      }
      case unmatched => {
        return "UnknownUsefulType(" + unmatched + ")"
      }
    }
  }
  
  def typeNameOf(builtinType: ast.BuiltinType): String = {
    builtinType match {
      case ast.BitStringType(_) => {
        return "_meta_.AsnBitString"
      }
      case ast.BOOLEAN => {
        return "_meta_.AsnBoolean"
      }
      case characterString: ast.CharacterStringType => {
        typeNameOf(characterString)
      }
      case _: ast.ChoiceType => {
        return "_meta_.AsnChoice"
      }
      case ast.EmbeddedPdvType => {
        return "_meta_.AsnEmbeddedPdv"
      }
      case ast.EnumeratedType(_) => {
        return "_meta_.AsnEnumeration"
      }
      case ast.EXTERNAL => {
        return "ExternalType"
      }
      case ast.InstanceOfType(_) => {
        return "InstanceOfType"
      }
      case ast.INTEGER(_) => {
        return "_meta_.AsnInteger"
      }
      case ast.NULL => {
        return "_meta_.AsnNull"
      }
      case _: ast.ObjectClassFieldType => {
        return "_meta_.AsnObjectClassField"
      }
      case ast.ObjectIdentifierType => {
        return "_meta_.AsnObjectIdentifier"
      }
      case ast.OctetStringType => {
        return "_meta_.AsnOctetString"
      }
      case ast.REAL => {
        return "_meta_.AsnReal"
      }
      case ast.RelativeOidType => {
        return "_meta_.AsnRelativeOidType"
      }
      case ast.SequenceOfType(_) => {
        return "_meta_.AsnSequenceOf"
      }
      case ast.SequenceType(_) => {
        return "_meta_.AsnSequence"
      }
      case ast.SetOfType(_) => {
        return "_meta_.AsnSetOf"
      }
      case ast.SetType(_) => {
        return "_meta_.AsnSet"
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
        return "_meta_.AsnBmpString"
      }
      case ast.GeneralString => {
        return "_meta_.AsnGeneralString"
      }
      case ast.GraphicString => {
        return "_meta_.AsnGraphicString"
      }
      case ast.IA5String => {
        return "_meta_.AsnIa5String"
      }
      case ast.ISO646String => {
        return "_meta_.AsnIso646String"
      }
      case ast.NumericString => {
        return "_meta_.AsnNumericString"
      }
      case ast.PrintableString => {
        return "_meta_.AsnPrintableString"
      }
      case ast.T61String => {
        return "_meta_.AsnT61String"
      }
      case ast.TeletexString => {
        return "_meta_.AsnTeletexString"
      }
      case ast.UniversalString => {
        return "_meta_.AsnUniversalString"
      }
      case ast.UTF8String => {
        return "_meta_.AsnUtf8String"
      }
      case ast.VideotexString => {
        return "_meta_.AsnVideotexString"
      }
      case ast.VisibleString => {
        return "_meta_.AsnVisibleString"
      }
      case unknown => {
        return "UnknownCharacterString(" + unknown + ")"
      }
    }
  }
}

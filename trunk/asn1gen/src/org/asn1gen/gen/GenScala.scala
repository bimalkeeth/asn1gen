package org.asn1gen.gen

import java.io.PrintWriter
import org.asn1gen.parsing.asn1.ast._
import org.asn1gen.io._

class GenScala(out: IndentWriter) {
  var moduleName: Option[String] = None
  
  def generate(moduleDefinition: ModuleDefinition): Unit = {
    moduleDefinition match {
      case moduleDefinition@ModuleDefinition(
        ModuleIdentifier(
          ModuleReference(moduleName),
          DefinitiveIdentifier(_)),
        _,
        ExtensionDefault(_),
        ModuleBody(_, _, assignmentList))
      => {
        out.println("package " + this.moduleName.getOrElse(moduleName) +" {")
        out.indent(2) {
          out.println("import org.asn1gen.runtime._")
          out.println()
          generate(assignmentList)
        }
        out.println("}")
      }
    }
  }

  def generate(assignmentList: AssignmentList): Unit = {
    assignmentList match {
      case AssignmentList(assignments) => assignments foreach { assignment =>
        generate(assignment)
      }
    }
  }

  def generate(assignment: Assignment): Unit = {
    assignment match {
      case TypeAssignment(
        TypeReference(name),
        _type: Type)
      => {
        generate(_type , name)
      }
    }
  }
  
  def generate(_type: Type, name: String): Unit = {
    _type match {
      case Type(builtinType: BuiltinType, _) => {
        generate(builtinType, name)
      }
    }
  }
  
  def generate(builtinType: BuiltinType, assignmentName: String): Unit = {
    builtinType match {
      case ChoiceType(
        AlternativeTypeLists(rootAlternativeTypeList, _, _, _))
      => {
        out.println(
            "case class " + assignmentName +
            "(_choice: Int, _element: AsnType) extends AsnChoice {")
        out.indent(2) {
          out.println("//////////////////////////////////////////////////////////////////")
          out.println("// Choice IDs")
          generateChoiceIds(rootAlternativeTypeList)
          generateSimpleGetters(rootAlternativeTypeList)
        }
        out.println("}")
        out.println()
        val firstChoiceType =
          rootAlternativeTypeList.alternativeTypeList.namedTypes(0)._type
        out.println(
            "object " + assignmentName + " extends " + assignmentName +
            "(0, " + typeNameOf(firstChoiceType) + ") {")
        out.println("}")
      }
      case SequenceType(spec) => {
        out.print("case class " + assignmentName + "(")
        spec match {
          case ComponentTypeLists(list1, extension, list2) => {
            out.println()
            out.indent(2) {
              list1 match {
                case Some(ComponentTypeList(list)) => {
                  generateSequenceConstructor(assignmentName, list)
                }
                case None => ()
              }
              out.println()
            }
          }
          case Empty => {}
        }
        out.println(") extends AsnSequence {")
        out.indent(2) {
          spec match {
            case ComponentTypeLists(list1, extension, list2) => {
              list1 match {
                case Some(ComponentTypeList(list)) => {
                  generateSequenceImmutableSetters(assignmentName, list)
                }
                case None => ()
              }
            }
            case Empty => {}
          }
        }
        out.println("}")
        out.println()
        out.print("object " + assignmentName + " extends " + assignmentName + "(")
        out.indent(2) {
          spec match {
            case ComponentTypeLists(list1, extension, list2) => {
              out.println()
              list1 match {
                case Some(ComponentTypeList(list)) => {
                  var firstItem = true
                  list.map {
                    case NamedComponentType(
                      NamedType(_, _type),
                      optionalDefault)
                    => {
                      if (!firstItem) {
                        out.println(",")
                      }
                      optionalDefault match {
                        case Empty => {
                          out.print(typeNameOf(_type))
                        }
                        case Optional => {
                          out.print("Some(" + typeNameOf(_type) + ")")
                        }
                        case Default(value) => {
                          out.print("/* Default(" + value + ") */")
                        }
                      }
                      firstItem = false
                    }
                  }
                  out.println()
                }
                case None => ()
              }
            }
            case Empty => {}
          }
        }
        out.println(") {")
        out.println("}")
      }
      case EnumeratedType(enumerations)
      => {
        out.println("case class " + assignmentName + "(_value: Int) extends AsnEnumeration {")
        out.println("}")
        out.println()
        out.println("object " + assignmentName + " extends " + assignmentName + "(0) {")
        out.indent(2) {
          generate(assignmentName, enumerations)
        }
        out.println("}")
      }
      case unmatched => {
        out.println("// Unmatched: " + unmatched)
      }
    }
  }
  
  def generate(assignmentName: String, enumerations: Enumerations): Unit = {
    enumerations match {
      case Enumerations(RootEnumeration(Enumeration(items)), extension)
      => {
        var index = 0
        items foreach { case Identifier(item) =>
          out.println(
            "def " + item + ": " + assignmentName +
            " = " + assignmentName + "(" + index + ")")
          index = index + 1
        }
        extension match {
          case None => {}
          case _ => out.println(extension)
        }
      }
    }
  }
  
  def typeNameOf(namedComponentType: NamedComponentType): String = {
    namedComponentType match {
      case NamedComponentType(
        NamedType(Identifier(identifier), _type),
        value)
      => {
        typeNameOf(_type, value)
      }
    }
  }
  
  def typeNameOf(_type: Type, value: OptionalDefault[Value]): String = {
    value match {
      case Empty =>
        return typeNameOf(_type)
      case Default(value) =>
        return typeNameOf(_type)
      case Optional =>
        return "Option[" + typeNameOf(_type) + "]"
    }
  }
  
  def generateSequenceConstructor(
      sequenceName: String, list: List[ComponentType]): Unit = {
    var firstTime = true
    list foreach {
      case NamedComponentType(
        NamedType(Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out.println(",")
        }
        out.print(identifier + ": " + typeNameOf(_type, value))
        firstTime = false
      }
    }
  }
  
  def typeNameOf(_type: Type): String = {
    _type match {
      case Type(typeKind, _) => typeNameOf(typeKind)
    }
  }
  
  def typeNameOf(typeKind: TypeKind): String = {
    typeKind match {
      case builtinType: BuiltinType => typeNameOf(builtinType)
      case TypeReference(reference) => reference
      case unmatched => "Unmatched(" + unmatched + ")"
    }
  }
  
  def typeNameOf(typeKind: TypeKind, value: OptionalDefault[Value]): String = {
    value match {
      case Empty =>
        return typeNameOf(typeKind)
      case Default(value) =>
        return typeNameOf(typeKind)
      case Optional =>
        return "Option[" + typeNameOf(typeKind) + "]"
    }
  }
  
  def typeNameOf(builtinType: BuiltinType): String = {
    builtinType match {
      case BitStringType(_) => {
        return "AsnBitString"
      }
      case BOOLEAN => {
        return "AsnBoolean"
      }
      case characterString: CharacterStringType => {
        typeNameOf(characterString)
      }
      case _: ChoiceType => {
        return "AsnChoice"
      }
      case EmbeddedPdvType => {
        return "AsnEmbeddedPdv"
      }
      case EnumeratedType(_) => {
        return "AsnEnumerated"
      }
      case EXTERNAL => {
        return "ExternalType"
      }
      case InstanceOfType(_) => {
        return "InstanceOfType"
      }
      case IntegerType(_) => {
        return "AsnInteger"
      }
      case NULL => {
        return "AsnNull"
      }
      case _: ObjectClassFieldType => {
        return "AsnObjectClassField"
      }
      case ObjectIdentifierType => {
        return "AsnObjectIdentifier"
      }
      case OctetStringType => {
        return "AsnOctetString"
      }
      case REAL => {
        return "AsnReal"
      }
      case RelativeOidType => {
        return "AsnRelativeOidType"
      }
      case SequenceOfType(_) => {
        return "AsnSequenceOf"
      }
      case SequenceType(_) => {
        return "AsnSequence"
      }
      case SetOfType(_) => {
        return "AsnSetOf"
      }
      case SetType(_) => {
        return "AsnSet"
      }
      case TaggedType(_, _, underlyingType) => {
        return typeNameOf(underlyingType)
      }
      case unmatched => {
        return "UnknownBuiltinType(" + unmatched + ")"
      }
    }
  }
  
  def typeNameOf(characterString: CharacterStringType): String = {
    characterString match {
      case BMPString => {
        return "AsnBmpString"
      }
      case GeneralString => {
        return "AsnGeneralString"
      }
      case GraphicString => {
        return "AsnGraphicString"
      }
      case IA5String => {
        return "AsnIa5String"
      }
      case ISO646String => {
        return "AsnIso646String"
      }
      case NumericString => {
        return "AsnNumericString"
      }
      case PrintableString => {
        return "AsnPrintableString"
      }
      case T61String => {
        return "AsnT61String"
      }
      case TeletexString => {
        return "AsnTeletexString"
      }
      case UniversalString => {
        return "AsnUniversalString"
      }
      case UTF8String => {
        return "AsnUtf8String"
      }
      case VideotexString => {
        return "AsnVideotexString"
      }
      case VisibleString => {
        return "AsnVisibleString"
      }
      case unknown => {
        return "UnknownCharacterString(" + unknown + ")"
      }
    }
  }
  
  def generateSequenceImmutableSetters(sequenceName: String, list: List[ComponentType]): Unit = {
    val fieldNames = list.map {
      case NamedComponentType(
        NamedType(Identifier(identifier), _),
        _)
      => identifier
    }
    list foreach {
      case NamedComponentType(
        NamedType(Identifier(identifier), _type),
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
      _type: Type,
      value: OptionalDefault[Value],
      fieldNames: List[String]): Unit = {
    _type match {
      case Type(TaggedType(_, _, fieldType), _) => {
        generateSequenceImmutableSetter(sequenceName, fieldName, fieldType, value, fieldNames)
        //out.println("// tag " + number)
      }
      case Type(builtinType: BuiltinType, List()) => {
        val setterType = typeNameOf(builtinType, value)
        out.println(
            "def " + fieldName + "(f: (" + setterType + " => " +
            setterType + ")): " + sequenceName + " = " + sequenceName + "(")
        var firstIteration = true
        out.indent(2) {
          fieldNames foreach { listedFieldName =>
            if (!firstIteration) {
              out.println(",")
            }
            if (listedFieldName == fieldName) {
              out.print("f(this." + fieldName + ")")
            } else {
              out.print("this." + listedFieldName)
            }
            firstIteration = false
          }
          out.println(")")
        }
      }
      case unmatched => {
        out.println("// Unmatched type: " + unmatched)
      }
    }
  }
  
  def generateChoiceIds(rootAlternativeTypeList: RootAlternativeTypeList): Unit = {
    rootAlternativeTypeList match {
      case RootAlternativeTypeList(AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateChoiceIds(namedType)
        }
      }
    }
  }
  
  def generateChoiceIds(namedType: NamedType): Unit = {
    namedType match {
      case NamedType(
        Identifier(name),
        Type(
          TaggedType(
            Tag(_, Number(tagNumber)), _, _type),
          _))
      => {
        out.println()
        out.println("val " + name.toUpperCase + ": Integer = " + tagNumber)
      }
    }
  }

  def generateSimpleGetters(rootAlternativeTypeList: RootAlternativeTypeList): Unit = {
    rootAlternativeTypeList match {
      case RootAlternativeTypeList(AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateSimpleGetters(namedType)
        }
      }
    }
  }
  
  def generateSimpleGetters(namedType: NamedType): Unit = {
    namedType match {
      case NamedType(
        Identifier(name),
        _type)
      => {
        out.println()
        out.println(
        		"def " + name + ": " + typeNameOf(_type) +
        		" = choice_.asInstanceOf[" + typeNameOf(_type) + "]")
      }
    }
  }
}
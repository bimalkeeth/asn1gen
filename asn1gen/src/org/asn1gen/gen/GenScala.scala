package org.asn1gen.gen

import java.io.PrintWriter
import org.asn1gen.parsing.asn1.ast._
import org.asn1gen.io._

class GenScala(out: IndentWriter) {
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
        out.println("package " + moduleName +" {")
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
        type_ : Type_)
      => {
        generate(type_ , name)
      }
    }
  }
  
  def generate(type_ : Type_, name: String): Unit = {
    type_ match {
      case Type_(builtinType: BuiltinType, _) => {
        generate(builtinType, name)
      }
    }
  }
  
  def generate(builtinType: BuiltinType, name: String): Unit = {
    builtinType match {
      case ChoiceType(
        AlternativeTypeLists(rootAlternativeTypeList, _, _, _))
      => {
        out.println("case class " + name + "(override val choice: AsnType) extends AsnChoice(choice) {")
        out.indent(2) {
          out.println("//////////////////////////////////////////////////////////////////")
          out.println("// Choice IDs")
          generateChoiceIds(rootAlternativeTypeList)
          generateSimpleGetters(rootAlternativeTypeList)
        }
        out.println("}")
      }
      case SequenceType(ComponentTypeLists(list1, extension, list2))
      => {
        out.println("case class " + name + "(")
        out.indent(2) {
          list1 match {
            case Some(ComponentTypeList(list)) => {
              generateSequenceConstructor(name, list)
            }
            case None => ()
          }
        }
        out.println()
        out.println(") extends AsnSequence {")
        out.indent(2) {
          list1 match {
            case Some(ComponentTypeList(list)) => {
              generateSequenceImmutableSetters(name, list)
            }
            case None => ()
          }
        }
        out.println("}")
      }
    }
  }
  
  def generateSequenceConstructor(
      sequenceName: String, list: List[ComponentType]): Unit = {
    var firstTime = true
    list foreach {
      case NamedComponentType(
        NamedType(Identifier(identifier), componentType),
        value)
      => {
        if (!firstTime) {
          out.println(",")
        }
        out.print(identifier + ": " + typeNameOf(componentType))
        firstTime = false
      }
    }
  }
  
  def typeNameOf(type_ : Type_): String = {
    type_ match {
      case Type_(typeKind, _) => typeNameOf(typeKind)
    }
  }
  
  def typeNameOf(typeKind: TypeKind): String = {
    typeKind match {
      case builtinType: BuiltinType => typeNameOf(builtinType)
      case TypeReference(reference) => reference
      case unmatched => "Unmatched(" + unmatched + ")"
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
      case _: CharacterStringType => {
        return "AsnCharacterString"
      }
      case _: ChoiceType => {
        return "AsnChoice"
      }
      case EmbeddedPdvType => {
        return "AdnEmbeddedPdv"
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
  
  def generateSequenceImmutableSetters(sequenceName: String, list: List[ComponentType]): Unit = {
    val fieldNames = list.map {
      case NamedComponentType(
        NamedType(Identifier(identifier), _),
        _)
      => identifier
    }
    list foreach {
      case NamedComponentType(
        NamedType(Identifier(identifier), componentType),
        value)
      => {
        generateSequenceImmutableSetter(sequenceName: String, identifier, componentType, fieldNames)
      }
    }
  }
  
  def generateSequenceImmutableSetter(
      sequenceName: String,
      fieldName: String,
      type_ : Type_,
      fieldNames: List[String]): Unit = {
    type_ match {
      case Type_(TaggedType(_, _, fieldType), _) => {
        generateSequenceImmutableSetter(sequenceName, fieldName, fieldType, fieldNames)
        //out.println("// tag " + number)
      }
      case Type_(builtinType: BuiltinType, List()) => {
    	val setterType = typeNameOf(builtinType)
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
        Type_(
          TaggedType(
            Tag(_, Number(tagNumber)), _, type_),
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
        type_)
      => {
        out.println()
        out.println(
        		"def " + name + ": " + typeNameOf(type_) +
        		" = choice_.asInstanceOf[" + typeNameOf(type_) + "]")
      }
    }
  }
}

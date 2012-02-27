package org.asn1gen.gen.java

import java.io.PrintWriter
import org.asn1gen.gen.AsnCodeGenerationException
import org.asn1gen.extra.Extras._
import org.asn1gen.io._
import org.asn1gen.parsing.asn1.{ast => ast}
import scala.collection.immutable.Set
import org.asn1gen.gen.java.NameOf._

class GenJava(packageName: String, namedType: NamedType, out: IndentWriter) {
  def generate(module: Module): Unit = {
    out << "/* This file was generated by asn1gen */" << EndLn
    out << EndLn
    out << "package " << packageName << ";" << EndLn
    out << EndLn
    out << "import org.asn1gen.java.runtime;" << EndLn
    module.imports foreach { symbolsFromModule =>
      out << "import " << symbolsFromModule.module << "._" << EndLn
    }
    out << EndLn
    module.types.foreach { case (_, namedType: NamedType) =>
      generate(namedType)
    }
    module.values foreach { case (name, namedValue) =>
      generate(namedValue)
    }
  }
  
  def generate(namedValue: NamedValue): Unit = {
    out.trace("/*", "*/")
    namedValue match {
      case NamedValue(name, ast.Type(ast.INTEGER(None), _), ast.SignedNumber(negative, ast.Number(magnitude))) => {
        out << "lazy val " << name << " = "
        if (negative) {
          out << "-"
        }
        out << magnitude << EndLn
      }
      case NamedValue(name, ast.Type(ast.BOOLEAN, _), ast.BooleanValue(booleanValue)) => {
        out << "lazy val " << name << " = " << booleanValue << EndLn
      }
      case NamedValue(name, ast.Type(ast.OctetStringType, _), ast.CString(stringValue)) => {
        out << "lazy val " << name << " = " << stringValue.inspect << "" << EndLn
      }
      case NamedValue(name, typePart, valuePart) => {
        typePart match {
          case ast.Type(ast.TypeReference(typeName), _) => {
            out << "lazy val " << name << " = " << typeName << EndLn
          }
        }
        out.indent(2) {
          valuePart match {
            case ast.SequenceValue(memberValues) => {
              memberValues.foreach { memberValue =>
                memberValue match {
                  case ast.NamedValue(ast.Identifier(id), value) => {
                    out << "." << safeId(id) << " { " << "_ => "
                    value match {
                      case ast.CString(stringValue) => {
                        out << stringValue.inspect
                      }
                      case ast.ValueReference(valueReferenceName) => {
                        out << safeId(valueReferenceName)
                      }
                      case ast.BooleanValue(booleanValue) => {
                        out << booleanValue
                      }
                      
                    }
                    out << " }" << EndLn
                  }
                }
              }
            }
          }
        }
      }
      case x => {
        out << "/* unknown value " << x << " */" << EndLn
      }
    }
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
            out << "type " << safeId(namedType.name) << " = " << safeId(name) << EndLn
            out << "lazy val " << safeId(namedType.name) << " = " + safeId(name) << EndLn
          }
          case _ => {
            out << "/* referencedType" << EndLn
            out << referencedType << EndLn
            out << "*/" << EndLn
          }
        }
      }
      case t@ast.Type(_, _) => {
        out << "/* unknown: " << namedType.name << EndLn
        out << t << EndLn
        out << "*/" << EndLn
      }
    }
  }
  
  def generate(builtinType: ast.BuiltinType, assignmentName: String): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    builtinType match {
      case ast.ChoiceType(
        ast.AlternativeTypeLists(rootAlternativeTypeList, _, _, _))
      => {
        out << "abstract class " << safeAssignmentName << "(_element: Any) extends org.asn1gen.java.runtime.AsnChoice {" << EndLn
        out.indent(2) {
          out << "def _choice: Int" << EndLn
          generateSimpleGetters(rootAlternativeTypeList)
          generateChoiceFieldTransformers(assignmentName, rootAlternativeTypeList)
        }
        out << "}" << EndLn
        generateChoices(assignmentName, rootAlternativeTypeList)
        val firstNamedType =
          rootAlternativeTypeList.alternativeTypeList.namedTypes(0)
        out << EndLn
        out << "object " << safeAssignmentName << " extends " << safeId(assignmentName + "_" + firstNamedType.name) << "(" << rawDefaultOf(firstNamedType._type) << ") {" << EndLn
        out.indent(2) {
          generateChoiceValAliases(assignmentName, rootAlternativeTypeList)
          generateChoiceTypeAliases(assignmentName, rootAlternativeTypeList)
        }
        out << "}" << EndLn
      }
      case ast.SequenceType(ast.Empty) => {
        out.ensureEmptyLines(1)
        out << "class " << safeAssignmentName << " extends org.asn1gen.java.runtime.AsnSequence {" << EndLn
        out << "}" << EndLn
      }
      case ast.SequenceType(ast.ComponentTypeLists(list1, extension, list2)) => {
        val list = (list1.toList:::list2.toList).map { componentTypeList =>
          componentTypeList.componentTypes
        }.flatten
        out.ensureEmptyLines(1)
        out << "public class " << safeAssignmentName << " extends org.asn1gen.java.runtime.AsnSequence {" << EndLn
        out.indent(2) {
          generateSequenceFieldDefines(assignmentName, list)
          out << EndLn
          out << "public " << safeAssignmentName << "(" << EndLn
          out.indent(2) {
            out.indent(2) {
              generateSequenceParameters(list)
              out << ") {" << EndLn
            }
            list1 match {
              case Some(ast.ComponentTypeList(list)) => {
                generateConstructorAssignments(assignmentName, list)
              }
              case None => ()
            }
          }
          out << "}" << EndLn
          out << EndLn
          out << "@Override" << EndLn
          out << "public boolean equals(final " << safeAssignmentName << " that): Boolean = {" << EndLn
          out.indent(2) {
            out << "assert that != null" << EndLn
            out.trace("/*", "*/")
            list foreach {
              case ast.NamedComponentType(ast.NamedType(ast.Identifier(identifier), _type), value) => {
                out << EndLn
                out << "if (!this." << safeId(identifier) << ".equals(that." + safeId(identifier) + ")) {" << EndLn
                out.indent(2) {
                  out << "return false;" << EndLn
                }
                out << "}" << EndLn
              }
            }
            out << EndLn
            out << "return true" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "public boolean equals(final " << safeAssignmentName << " that) {" << EndLn
          out.indent(2) {
            out << "if (that instanceof " << safeAssignmentName << ") {" << EndLn
            out.indent(2) {
              out << "return this.equals((" + safeAssignmentName + ")that);" << EndLn
            }
            out << "}" << EndLn
            out << EndLn
            out << "return true;" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "@Override" << EndLn
          out << "public int hashCode() {" << EndLn
          out.indent(2) {
            out << "return (0"
            out.indent(2) {
              list foreach {
                case ast.NamedComponentType(ast.NamedType(ast.Identifier(identifier), _), value) => {
                  out << EndLn << "^ this." << safeId(identifier) << ".hashCode()"
                }
              }
            }
            out << ");" << EndLn
          }
          out << "}" << EndLn
        }
        out << "}" << EndLn
        out << EndLn
      }
      case ast.EnumeratedType(enumerations)
      => {
        var firstIndex: Option[Long] = None
        out.ensureEmptyLines(1)
        out << "public class " << safeAssignmentName << " extends org.asn1gen.java.runtime.AsnEnumeration {" << EndLn
        out.indent(2) {
          out << "public static " << safeAssignmentName << " EMPTY = new " << safeAssignmentName << "(0);" << EndLn
          out << EndLn
          out << "public final long value;" << EndLn
          out << EndLn
          out << "public " << safeAssignmentName << "(final long value) {" << EndLn
          out.indent(2) {
            out << "this.value = value;" << EndLn
          }
          out << "}" << EndLn
        }
        out << "}" << EndLn
        out << EndLn
        out << "object " << safeAssignmentName << " extends "
        out << safeAssignmentName << "(" << firstIndex.getOrElse(0L) << ") {" << EndLn
        out.indent(2) {
          generateEnumeratedValues(enumerations, assignmentName)
          out << EndLn
          out << "def of(name: String): " << safeId(assignmentName) << " = {" << EndLn
          out.indent(2) {
            out << "name match {" << EndLn
            out.indent(2) {
              enumerations match {
                case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
                => {
                  var index = 0
                  items foreach {
                    case ast.Identifier(item) => {
                      out << "case " << safeId(item).inspect << " => " << safeId(item) << EndLn
                      index = index + 1
                    }
                    case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                      out << "case " << safeId(item).inspect << " => " + safeId(item) << EndLn
                      index = index + 1
                    }
                  }
                  extension match {
                    case None => {}
                    case _ => out << extension << EndLn
                  }
                }
              }
              out << "case _ => throw org.asn1gen.java.runtime.BadEnumerationException(" << EndLn
              out.indent(2) {
                out << "\"Unrecogonised enumeration value + '\" + name + \"'\")" << EndLn
              }
            }
            out << "}" << EndLn
          }
          out << "}" << EndLn << EndLn
          out << "def of(value: Int): " << safeId(assignmentName) << " = {" << EndLn
          out.indent(2) {
            out << "value match {" << EndLn
            out.indent(2) {
              enumerations match {
                case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
                => {
                  var index = 0
                  items foreach {
                    case ast.Identifier(item) => {
                      out << "case " << index << " => " << safeId(item) << EndLn
                      index = index + 1
                    }
                    case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                      val value = if (sign) n * -1 else n
                      out << "case " << value << " => " << safeId(item) << EndLn
                      index = index + 1
                    }
                  }
                  extension match {
                    case None => {}
                    case _ => out << extension << EndLn
                  }
                }
              }
              out << "case _ => " << safeId(assignmentName) << "(value)" << EndLn
            }
            out << "}" << EndLn
          }
          out << "}" << EndLn
        }
        out << "}" << EndLn
        out << EndLn
        generateEnumeratedValues(enumerations, assignmentName)
      }
      case setOfType: ast.SetOfType => {
        generate(assignmentName, setOfType)
      }
      case bitStringType: ast.BitStringType => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = org.asn1gen.java.runtime.AsnBitString" << EndLn
        out << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = org.asn1gen.java.runtime.AsnBitString" << EndLn
      }
      case ast.INTEGER(None) => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = Long" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = 0L" << EndLn
      }
      case ast.BOOLEAN => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = org.asn1gen.java.runtime.AsnBoolean" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = org.asn1gen.java.runtime.AsnFalse" << EndLn
      }
      case ast.OctetStringType => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = org.asn1gen.java.runtime.AsnOctetString" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = org.asn1gen.java.runtime.AsnOctetString" << EndLn
      }
      case ast.PrintableString => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = String" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = \"\"" << EndLn
      }
      case ast.REAL => {
        out.ensureEmptyLines(1)
        out  << "type " << safeAssignmentName << " = Double" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = 0.0" << EndLn
      }
      case ast.UTF8String => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = String" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = \"\"" << EndLn
      }
      case unmatched => {
        out.ensureEmptyLines(1)
        out << "// Unmatched " << safeAssignmentName << ": " << unmatched << EndLn
      }
    }
  }
  
  def generateEnumeratedValues(enumerations: ast.Enumerations, assignmentName:String): Unit = {
    out.trace("/*", "*/")
    enumerations match {
      case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension) => {
        var index = 0
        items foreach {
          case ast.Identifier(item) => {
            out << "val " << safeId(item) << " = " << safeId(assignmentName) << "(" << index << ")" << EndLn
            index = index + 1
          }
          case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
            val value = if (sign) n * -1 else n
            out << "val " << safeId(item) << " = " << safeId(assignmentName) << "(" << value << ")" << EndLn
            index = index + 1
          }
        }
        extension match {
          case None => {}
          case _ => out.println(extension)
        }
      }
    }
  }
  
  def generate(assignmentName: String, setOfType: ast.SetOfType): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    setOfType match {
      case ast.SetOfType(ast.Type(elementType, _)) => {
        elementType match {
          case ast.TypeReference(referencedType) => {
            val safeReferenceType = safeId(referencedType)
            out.ensureEmptyLines(1)
            out << "public class " << safeAssignmentName << " extends org.asn1gen.java.runtime.AsnList {" << EndLn
            out.indent(2) {
              out << "public static " << safeAssignmentName << " EMPTY = new " << safeAssignmentName << "(org.asn1gen.java.runtime.Nil.<" << safeReferenceType << ">instance());" << EndLn
              out << EndLn
              out << "public final org.asn1gen.java.runtime.List<" << safeReferenceType << "> items;" << EndLn
              out << EndLn
              out << "public " << safeAssignmentName << " withItems(final org.asn1gen.java.runtime.List<" << safeReferenceType << "> value) {" << EndLn
              out.indent(2) {
                out << "return new " << safeAssignmentName << "(value);" << EndLn
              }
              out << "}" << EndLn
              out << EndLn
              out << "@Override" << EndLn
              out << "public boolean equals(final " << safeAssignmentName << " that) {" << EndLn
              out.indent(2) {
                out << "assert other != null;" << EndLn
                out << EndLn
                out << "return this.items.equals(that.items)" << EndLn
              }
              out << "}" << EndLn
              out << EndLn
              out << "@Override" << EndLn
              out << "public int hashCode() {" << EndLn
              out.indent(2) {
                out << "return this.items.hascode()" << EndLn
              }
              out << "}" << EndLn
            }
            out << "}" << EndLn << EndLn
          }
          case sequenceType: ast.SequenceType => {
            assert(false)
            val assignmentElementName = assignmentName + "_element"
            val safeAssignmentElementName = safeId(assignmentElementName)
            out.ensureEmptyLines(1)
            out << "type " << safeAssignmentName << " = List[" << safeAssignmentElementName << "]" << EndLn
            out << "lazy val " << safeAssignmentName << " = Nil: List[" << safeAssignmentElementName << "]" << EndLn
            generate(sequenceType, assignmentElementName)
          }
          case builtinType: ast.BuiltinType => {
            out.ensureEmptyLines(1)
            out  << "type " << safeAssignmentName << " = List[" << asnTypeOf(builtinType) << "]" << "lazy val " << safeAssignmentName << " = Nil: List[" << asnTypeOf(builtinType) << "]"
          }
        }
      }
    }
  }
  
  def generateSequenceFieldDefines(sequenceName: String, list: List[ast.ComponentType]): Unit = {
    out.trace("/*", "*/")
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        out << "public final " << safeId(asnTypeOf(_type, value)) << " " << safeId(identifier) << ";" << EndLn
      }
    }
  }
  
  def generateSequenceFieldParameters(
      sequenceName: String, list: List[ast.ComponentType]): Unit = {
    out.trace("/*", "*/")
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out << "," << EndLn
        }
        out << safeId(identifier) << ": " << safeId(asnTypeOf(_type, value))
        firstTime = false
      }
    }
  }
  
  def generateConstructorAssignments(
      sequenceName: String, list: List[ast.ComponentType]): Unit = {
    out.trace("/*", "*/")
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        out << "this." << safeId(identifier) << " = " << safeId(identifier) << ";" << EndLn
      }
    }
  }
  
  def generateSequenceParameters(
      list: List[ast.ComponentType]): Unit = {
    out.trace("/*", "*/")
    var firstTime = true
    list foreach {
      case ast.NamedComponentType(
        ast.NamedType(ast.Identifier(identifier), _type),
        value)
      => {
        if (!firstTime) {
          out << "," << EndLn
        }
        out << safeId(asnTypeOf(_type, value)) << " " << safeId(identifier)
        firstTime = false
      }
    }
  }
  
  def generateChoices(
      assignmentName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    out.trace("/*", "*/")
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
    out.trace("/*", "*/")
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        ast.Type(
          ast.TaggedType(
            ast.Tag(_, ast.Number(tagNumber)), _, _type),
          _))
      => {
        val safeName = safeId(name)
        val safeElementType = safeId(rawTypeOf(_type))
        val safeChoiceType = safeId(assignmentName)
        val safeChoiceChoice = safeId(assignmentName + "_" + name)
        out.ensureEmptyLines(1)
        out << "public class " << safeChoiceChoice << "(_element: " << asnTypeOf(_type) << ") extends " << safeId(assignmentName) << "(_element) {" << EndLn
        out.indent(2) {
          out << "def _choice: Int = " + tagNumber << EndLn
          out << EndLn
          out << "override def " << safeName << ": Option[" << safeElementType << "] = Some(_element)" << EndLn
          out << EndLn
          out << "override def " << safeName << "(f: (" << safeElementType << " => " << safeElementType << ")): " << safeChoiceType << " = " << safeChoiceChoice << "(f(_element))" << EndLn
          out << EndLn << "override def _choiceName: String = " << name.inspect << EndLn
        }
        out << "}" << EndLn
      }
      case x => {
        throw new org.asn1gen.gen.AsnCodeGenerationException("CHOICE members need to be tagged: " + x)
      }
    }
  }

  def generateChoiceFieldTransformers(
      choiceTypeName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    out.trace("/*", "*/")
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateChoiceFieldTransformer(choiceTypeName, namedType)
        }
      }
    }
  }
  
  def generateChoiceValAliases(
      choiceTypeName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    out.trace("/*", "*/")
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          out << "val " << namedType.name.capitalise << " = " << choiceTypeName << "_" << namedType.name << EndLn
        }
      }
    }
  }

  def generateChoiceTypeAliases(
      choiceTypeName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    out.trace("/*", "*/")
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          out << "type " << namedType.name.capitalise << " = " << choiceTypeName << "_" << namedType.name << EndLn
        }
      }
    }
  }

  def generateChoiceFieldTransformer(choiceTypeName: String, namedType: ast.NamedType): Unit = {
    out.trace("/*", "*/")
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        _type)
      => {
        val safeElementName = safeId(name)
        val safeChoiceType = safeId(choiceTypeName)
        val safeChoiceChoice = safeId(choiceTypeName + "_" + name)
        val safeElementType = safeId(asnTypeOf(_type))
        out << EndLn
        out << "def " << safeElementName << "(f: (" << safeElementType << " => " << safeElementType << ")): " << safeChoiceType << " = this" << EndLn
        out << EndLn
        out << "def " << safeElementName << "(f: => " << safeElementType << "): " << safeChoiceType << " = " << safeChoiceChoice << "(f)" << EndLn
      }
    }
  }

  def generateSimpleGetters(rootAlternativeTypeList: ast.RootAlternativeTypeList): Unit = {
    out.trace("/*", "*/")
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateSimpleGetters(namedType)
        }
      }
    }
  }
  
  def generateSimpleGetters(namedType: ast.NamedType): Unit = {
    out.trace("/*", "*/")
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        _type)
      => {
    	val safeName = safeId(name)
    	val safeType = safeId(asnTypeOf(_type))
        out << EndLn
        out << "def " << safeName << ": Option[" << safeType << "] = None" << EndLn
      }
    }
  }
}

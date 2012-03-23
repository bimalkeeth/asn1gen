package org.asn1gen.gen.java

import java.io.File
import java.io.PrintWriter
import org.asn1gen.extra.Extras._
import org.asn1gen.gen.AsnCodeGenerationException
import org.asn1gen.gen.java.NameOf._
import org.asn1gen.io._
import org.asn1gen.parsing.asn1.{ast => ast}
import scala.collection.immutable.Set

class GenJava(model: JavaModel, outDirectory: File, namespace: Option[String], moduleName: String) {
  val namespacePath = outDirectory / namespace.getOrElse("").replaceAll("\\.", "/")
  val modelPath = namespacePath / "model" / moduleName
  val codecPath = namespacePath / "codec" / moduleName
  val valuePath = namespacePath / "value"
  val valueFile = valuePath / (moduleName + ".java")
  
  def modelPackage(module: Module) = model.namespace.foldRight("model." + module.name)(_ + "." + _)
  def codecPackage(module: Module) = model.namespace.foldRight("codec." + module.name)(_ + "." + _)
  def valuePackage(module: Module) = model.namespace.foldRight("value")(_ + "." + _)
  
  def generate(implicit module: Module): Unit = {
    module.types.foreach { case (_, namedType: NamedType) =>
      val typeFile = modelPath.make.child(namedType.name + ".java")
      typeFile.withIndentWriter { out =>
        generateType(namedType)(module, out)
        println("Writing to " + typeFile)
      }
    }
    valuePath.make
    valueFile.withIndentWriter { out =>
      generateValues(module, out)
      println("Writing to " + valueFile)
    }
    codecPath.make
    val berCodecPath = codecPath / "BerEncoder.java"
    berCodecPath.withIndentWriter { out =>
      generateBerEncoder(module, out)
    }
  }
  
  def generatePackageAndImports(filePackage: String)(implicit module: Module, out: IndentWriter): Unit = {
    out << "/* This file was generated by asn1gen */" << EndLn
    out << EndLn
    out << "package " << filePackage << ";" << EndLn
    out << EndLn
    out << "import org.asn1gen.runtime.java.*;" << EndLn
    module.imports foreach { symbolsFromModule =>
      out << "import " << symbolsFromModule.module << "._" << EndLn
    }
    out << EndLn
    out << "import static org.asn1gen.runtime.java.Statics.*;" << EndLn
    out << EndLn
  }
  
  def generateType(namedType: NamedType)(implicit module: Module, out: IndentWriter): Unit = {
    generatePackageAndImports(modelPackage(module))(module, out)
    generate(namedType)
  }
  
  def generateValues(implicit module: Module, out: IndentWriter): Unit = {
    generatePackageAndImports(valuePackage(module))(module, out)
    out << "import " << modelPackage(module) << ".*;" << EndLn
    out << EndLn
    out << "public class " << module.name << " {" << EndLn
    out.indent(2) {
      module.values foreach { case (name, namedValue) =>
        generate(namedValue)
      }
    }
    out << "}" << EndLn
  }
  
  def generateBerEncoder(implicit module: Module, out: IndentWriter): Unit = {
    generatePackageAndImports(codecPackage(module))(module, out)
    out << "import " << modelPackage(module) << ";" << EndLn
    out << EndLn
    module.values foreach { case (name, namedValue) =>
      generate(namedValue)
    }
  }
  
  def generate(namedValue: NamedValue)(implicit module: Module, out: IndentWriter): Unit = {
    namedValue match {
      case NamedValue(name, ast.Type(ast.INTEGER(None), _), ast.SignedNumber(negative, ast.Number(magnitude))) => {
        out << "public static AsnInteger " << name << " = new AsnInteger("
        if (negative) {
          out << "-"
        }
        out << magnitude << ");" << EndLn
      }
      case NamedValue(name, ast.Type(ast.BOOLEAN, _), ast.BooleanValue(booleanValue)) => {
        out << "public static AsnBoolean " << name << " = new AsnBoolean(" << booleanValue << ");" << EndLn
      }
      case NamedValue(name, ast.Type(ast.OctetStringType, _), ast.CString(stringValue)) => {
        out << "public static AsnOctetString " << name << " = new AsnOctetString(" << stringValue.inspect << ");" << EndLn
      }
      case NamedValue(name, typePart, valuePart) => {
        typePart match {
          case ast.Type(ast.TypeReference(typeName), _) => {
            out << "public static " << safeId(typeName) << " " << name << " = " << typeName
          }
        }
        out.indent(2) {
          valuePart match {
            case ast.SequenceValue(memberValues) => {
              memberValues.foreach { memberValue =>
                memberValue match {
                  case ast.NamedValue(ast.Identifier(id), value) => {
                    out << EndLn
                    out << ".with" << safeId(id).capitalise << "("
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
                    out << ")"
                  }
                }
              }
              out << ";" << EndLn
            }
          }
        }
      }
      case x => {
        out << "/* unknown value " << x << " */" << EndLn
      }
    }
  }
  
  def generate(namedType: NamedType)(implicit module: Module, out: IndentWriter): Unit = {
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
  
  def generate(builtinType: ast.BuiltinType, assignmentName: String)(implicit module: Module, out: IndentWriter): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    builtinType match {
      case ast.ChoiceType(
        ast.AlternativeTypeLists(rootAlternativeTypeList, _, _, _))
      => {
        out << "abstract class " << safeAssignmentName << " extends org.asn1gen.runtime.java.AsnChoice {" << EndLn
        out.indent(2) {
          generateSimpleGetters(rootAlternativeTypeList)
          generateChoiceFieldTransformers(assignmentName, rootAlternativeTypeList)
        }
        out << "}" << EndLn
        out.trace("/*", "*/")
        rootAlternativeTypeList match {
          case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
            namedTypes foreach { namedType =>
              generateChoices(assignmentName, namedType)
            }
          }
        }

        val firstNamedType =
          rootAlternativeTypeList.alternativeTypeList.namedTypes(0)
        out << EndLn
      }
      case ast.SequenceType(ast.Empty) => {
        out.ensureEmptyLines(1)
        out << "public class " << safeAssignmentName << " extends org.asn1gen.runtime.java.AsnSequence {" << EndLn
        out.indent(2) {
          out << "public static final " << safeAssignmentName << " EMPTY = new " << safeAssignmentName << "();" << EndLn
        }
        out << "}" << EndLn
      }
      case ast.SequenceType(ast.ComponentTypeLists(list1, extension, list2)) => {
        val list = (list1.toList:::list2.toList).map { componentTypeList =>
          componentTypeList.componentTypes
        }.flatten
        out.ensureEmptyLines(1)
        out << "public class " << safeAssignmentName << " extends org.asn1gen.runtime.java.AsnSequence {" << EndLn
        out.indent(2) {
          out << "public static final " << safeAssignmentName << " EMPTY = new " << safeAssignmentName << "("
          out.indent(2) {
            var delim = ""
            list foreach {
              case ast.NamedComponentType(
                ast.NamedType(ast.Identifier(identifier), _type),
                value)
              => {
                out << delim << EndLn
                out << safeId(asnTypeOf(_type, value)) << ".EMPTY"
                delim = ","
              }
            }
          }
          out << ");" << EndLn
          out << EndLn
          out.trace("/*", "*/")
          list foreach {
            case ast.NamedComponentType(
              ast.NamedType(ast.Identifier(identifier), _type),
              value)
            => {
              out << "public final " << safeId(asnTypeOf(_type, value)) << " " << safeId(identifier) << ";" << EndLn
            }
          }
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
          list foreach {
            case ast.NamedComponentType(
              ast.NamedType(ast.Identifier(identifier), _type),
              value)
            => {
              out << "public final " << safeAssignmentName << " with" << safeId(identifier).capitalise << "(final " << safeId(asnTypeOf(_type, value)) << " value) {" << EndLn
              out.indent(2) {
                out << "return new " << safeAssignmentName << "("
                out.indent(2) {
                  var firstTime = true
                  list foreach {
                    case ast.NamedComponentType(
                      ast.NamedType(ast.Identifier(subIdentifier), _type),
                      value)
                    => {
                      if (!firstTime) {
                        out << ","
                      }
                      out << EndLn
                      if (identifier == subIdentifier) {
                        out << "value"
                      } else {
                        out << "this." << safeId(subIdentifier)
                      }
                      firstTime = false
                    }
                  }
                  out << ");" << EndLn
                }
              }
              out << "}" << EndLn
            }
          }
          out << EndLn
          out << "public boolean equals(final " << safeAssignmentName << " that) {" << EndLn
          out.indent(2) {
            out << "assert that != null;" << EndLn
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
            out << "return true;" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "@Override" << EndLn
          out << "public boolean equals(final Object that) {" << EndLn
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
        out << "public class " << safeAssignmentName << " extends org.asn1gen.runtime.java.AsnEnumeration {" << EndLn
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
          out << EndLn
          generateEnumeratedValues(enumerations, assignmentName)
          out << EndLn
          out << "public static " << safeId(assignmentName) << " of(final String name) {" << EndLn
          out.indent(2) {
            enumerations match {
              case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
              => {
                items foreach {
                  case ast.Identifier(item) => {
                    out << "if (name.equals(" << safeId(item).inspect << ")) {" << EndLn
                    out.indent(2) {
                      out << "return " << safeId(item) << ";" << EndLn
                    }
                    out << "}" << EndLn
                    out << EndLn
                  }
                  case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                    out << "if (name.equals(" << safeId(item).inspect << ")) {" << EndLn
                    out.indent(2) {
                      out << "return " << safeId(item) << ";" << EndLn
                    }
                    out << "}" << EndLn
                    out << EndLn
                  }
                }
                extension match {
                  case None => {}
                  case _ => out << extension << EndLn
                }
              }
            }
            out << "throw new org.asn1gen.runtime.java.BadEnumerationException(" << EndLn
            out.indent(2) {
              out << "\"Unrecogonised enumeration value + '\" + name + \"'\");" << EndLn
            }
          }
          out << "}" << EndLn
          out << EndLn
          out << "public static " << safeId(assignmentName) << " of(final int value) {" << EndLn
          out.indent(2) {
            out << "switch (value) {" << EndLn
            out.indent(2) {
              enumerations match {
                case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension)
                => {
                  var index = 0
                  items foreach {
                    case ast.Identifier(item) => {
                      out << "case " << index << ": return " << safeId(item) << ";" << EndLn
                      index = index + 1
                    }
                    case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
                      val value = if (sign) n * -1 else n
                      out << "case " << value << ": return " << safeId(item) << ";" << EndLn
                      index = index + 1
                    }
                  }
                  extension match {
                    case None => {}
                    case _ => out << extension << EndLn
                  }
                }
              }
              out << "default: return new " << safeId(assignmentName) << "(value);" << EndLn
            }
            out << "}" << EndLn
          }
          out << "}" << EndLn
        }
        out << "}" << EndLn
      }
      case setOfType: ast.SetOfType => {
        generate(assignmentName, setOfType)
      }
      case bitStringType: ast.BitStringType => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = org.asn1gen.runtime.java.AsnBitString" << EndLn
        out << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = org.asn1gen.runtime.java.AsnBitString" << EndLn
      }
      case ast.INTEGER(None) => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = Long" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = 0L" << EndLn
      }
      case ast.BOOLEAN => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = org.asn1gen.runtime.java.AsnBoolean" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = org.asn1gen.runtime.java.AsnFalse" << EndLn
      }
      case ast.OctetStringType => {
        out.ensureEmptyLines(1)
        out << "type " << safeAssignmentName << " = org.asn1gen.runtime.java.AsnOctetString" << EndLn
        out << EndLn
        out << "lazy val " << safeAssignmentName << " = org.asn1gen.runtime.java.AsnOctetString" << EndLn
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
  
  def generateEnumeratedValues(enumerations: ast.Enumerations, assignmentName: String)(implicit module: Module, out: IndentWriter): Unit = {
    enumerations match {
      case ast.Enumerations(ast.RootEnumeration(ast.Enumeration(items)), extension) => {
        var index = 0
        items foreach {
          case ast.Identifier(item) => {
            out << "public static final " << safeId(assignmentName) << " " << safeId(item) << " = new " << safeId(assignmentName) << "(" << index << ");" << EndLn
            index = index + 1
          }
          case ast.NamedNumber(ast.Identifier(item), ast.SignedNumber(sign, ast.Number(n))) => {
            val value = if (sign) n * -1 else n
            out << "public static final " << safeId(assignmentName) << safeId(item) << " = new " << safeId(assignmentName) << "(" << value << ");" << EndLn
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
  
  def generate(assignmentName: String, setOfType: ast.SetOfType)(implicit module: Module, out: IndentWriter): Unit = {
    val safeAssignmentName = safeId(assignmentName)
    setOfType match {
      case ast.SetOfType(ast.Type(elementType, _)) => {
        elementType match {
          case ast.TypeReference(referencedType) => {
            val safeReferenceType = safeId(referencedType)
            out.ensureEmptyLines(1)
            out << "public class " << safeAssignmentName << " extends org.asn1gen.runtime.java.AsnList {" << EndLn
            out.indent(2) {
              out << "public static " << safeAssignmentName << " EMPTY = new " << safeAssignmentName << "(org.asn1gen.runtime.java.Nil.<" << safeReferenceType << ">instance());" << EndLn
              out << EndLn
              out << "public final org.asn1gen.runtime.java.List<" << safeReferenceType << "> items;" << EndLn
              out << EndLn
              out << "public " << safeAssignmentName << "(final org.asn1gen.runtime.java.List<" << safeReferenceType << "> items) {" << EndLn
              out.indent(2) {
                out << "this.items = items;" << EndLn
              }
              out << "}" << EndLn
              out << EndLn
              out << "public " << safeAssignmentName << " withItems(final org.asn1gen.runtime.java.List<" << safeReferenceType << "> value) {" << EndLn
              out.indent(2) {
                out << "return new " << safeAssignmentName << "(value);" << EndLn
              }
              out << "}" << EndLn
              out << EndLn
              out << "public boolean equals(final " << safeAssignmentName << " that) {" << EndLn
              out.indent(2) {
                out << "assert that != null;" << EndLn
                out << EndLn
                out << "return this.items.equals(that.items);" << EndLn
              }
              out << "}" << EndLn
              out << EndLn
              out << "public boolean equals(final Object that) {" << EndLn
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
                out << "return this.items.hashCode();" << EndLn
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
  
  def generateSequenceFieldParameters(
      sequenceName: String, list: List[ast.ComponentType])(implicit module: Module, out: IndentWriter): Unit = {
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
      sequenceName: String, list: List[ast.ComponentType])(implicit module: Module, out: IndentWriter): Unit = {
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
  
  def generateSequenceParameters(list: List[ast.ComponentType])(implicit module: Module, out: IndentWriter): Unit = {
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
        out << "final " << safeId(asnTypeOf(_type, value)) << " " << safeId(identifier)
        firstTime = false
      }
    }
  }
  
  def generateChoices(
      assignmentName: String,
      namedType: ast.NamedType)(implicit module: Module, out: IndentWriter): Unit = {
    out.trace("/*", "*/")
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        choiceType)
      => {
        val valuesFile = modelPath.child(safeId(assignmentName) + "_" + safeId(name) + ".java")
        valuesFile.openPrintStream { ps =>
          println("Writing to " + valuesFile)
          generateChoice(assignmentName, name, choiceType)(module, new IndentWriter(ps))
        }
      }
      case x => {
        throw new org.asn1gen.gen.AsnCodeGenerationException("CHOICE members need to be tagged: " + x)
      }
    }
  }
  
  def generateChoice(assignmentName: String, name: String, choiceType: ast.Type)(implicit module: Module, out: IndentWriter): Unit = {
    choiceType match {
      case ast.Type(
          ast.TaggedType(
            ast.Tag(_, ast.Number(tagNumber)), _, _type),
          _) => {
        generatePackageAndImports(modelPackage(module))(module, out)
        val safeName = safeId(name)
        val safeElementType = safeId(rawTypeOf(_type))
        val safeChoiceType = safeId(assignmentName)
        val safeChoiceChoice = safeId(assignmentName + "_" + name)
        out.ensureEmptyLines(1)
        out << "public class " << safeChoiceChoice << " extends " << safeId(assignmentName) << " {" << EndLn
        out.indent(2) {
          out << "public final static " << safeChoiceChoice << " EMPTY = new " << safeChoiceChoice << "(" << asnTypeOf(_type) << ".EMPTY);" << EndLn
          out << EndLn
          out << "public final " << asnTypeOf(_type) << " element;" << EndLn
          out << EndLn
          out << "public " << safeChoiceChoice << "(final " << asnTypeOf(_type) << " element) {" << EndLn
          out.indent(2) {
            out << "this.element = element;" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "public " << asnTypeOf(_type) << " element() {" << EndLn
          out.indent(2) {
            out << "return this.element;" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "public int choiceId() {" << EndLn
          out.indent(2) {
            out << "return " << tagNumber << ";" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "@Override" << EndLn
          out << "public Option<" << safeElementType << "> get" << safeName.capitalise << "() {" << EndLn
          out.indent(2) {
            out << "return some(this.element);" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "@Override" << EndLn
          out << "public " << safeChoiceChoice << " with" << safeName.capitalise << "(final " << asnTypeOf(_type) << " value) {" << EndLn
          out.indent(2) {
            out << "return new " << safeChoiceChoice << "(value);" << EndLn
          }
          out << "}" << EndLn
          out << EndLn
          out << "public String choiceName() {" << EndLn
          out.indent(2) {
            out << "return " << name.inspect << ";" << EndLn
          }
          out << "}" << EndLn
        }
        out << "}" << EndLn
      }
    }
  }

  def generateChoiceFieldTransformers(
      choiceTypeName: String,
      rootAlternativeTypeList: ast.RootAlternativeTypeList)(implicit module: Module, out: IndentWriter): Unit = {
    out.trace("/*", "*/")
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
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
              out << "public " << safeChoiceChoice << " with" << safeElementName.capitalise << "(final " << safeElementType << " value) {" << EndLn
              out.indent(2) {
                out << "return new " << safeChoiceChoice << "(value);"
              }
              out << "}" << EndLn
            }
          }
        }
      }
    }
  }

  def generateSimpleGetters(rootAlternativeTypeList: ast.RootAlternativeTypeList)(implicit module: Module, out: IndentWriter): Unit = {
    out.trace("/*", "*/")
    rootAlternativeTypeList match {
      case ast.RootAlternativeTypeList(ast.AlternativeTypeList(namedTypes)) => {
        namedTypes foreach { namedType =>
          generateSimpleGetters(namedType)
        }
      }
    }
  }
  
  def generateSimpleGetters(namedType: ast.NamedType)(implicit module: Module, out: IndentWriter): Unit = {
    namedType match {
      case ast.NamedType(
        ast.Identifier(name),
        _type)
      => {
    	val safeName = safeId(name)
    	val safeType = safeId(asnTypeOf(_type))
        out << EndLn
        out << "public Option<" << safeType << "> get" << safeName.capitalise << "() {" << EndLn
        out.indent(2) {
    	    out << "return None.instance();" << EndLn
    	  }
        out << "}" << EndLn
      }
    }
  }
}

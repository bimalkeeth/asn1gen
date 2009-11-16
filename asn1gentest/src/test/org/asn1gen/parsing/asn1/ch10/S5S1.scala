import _root_.org.junit._
import _root_.org.junit.Assert._
import _root_.junit.framework.TestCase
import _root_.org.asn1gen.parsing.asn1.Asn1Parser
import _root_.org.asn1gen.parsing.asn1.ast._
import _root_.scala.util.parsing.input._

package test.org.asn1gen.parsing.asn1.ch10 {
  class TestS5S1 extends TestCase {
    
    object TheParser extends Asn1Parser {
      def parse[N](root: Parser[N], input: String) =
        phrase(root)(new lexical.Scanner(input))
    }
    
    import TheParser._
    
    @Test def test_1() {
      val text = """" +
          ExtendedReal ::= CHOICE {
            decimal REAL,
            particular-real ENUMERATED {
              one-third,
              pi,
              e,
              ...
            }
          }
          """
      parse(assignmentList, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_2() {
      val text = """
          pi REAL ::= { mantissa 314159, base 10, exponent -5 }
          e REAL ::= { mantissa 271828128459045235360287, base 10, exponent -23 }
          zero REAL ::= 0
          """
      parse(assignmentList, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_3() {
      val text = """
          SEQUENCE {
            mantissa INTEGER (ALL EXCEPT 0),
            base INTEGER (2|10),
            exponent INTEGER
          }
          """
      parse(assignmentList, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_4() {
      val text = """
          pi REAL ::= { 314159, 10, -5 }
          e REAL ::= { 271828128459045235360287, 10, -23 }
          zero REAL ::= 0
          """
      parse(assignmentList, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_5() {
      val text = """" +
          BinaryReal ::= REAL (WITH COMPONENTS {..., base (2)})
          """
      parse(assignmentList, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6() {
      val text = """
          RestrictedReal ::= REAL (
            WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            })
          """
      parse(assignmentList, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1() {
      val text = """
          RestrictedReal ::= REAL (
            WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            })
          """
      parse(typeAssignment, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }

    @Test def test_6_1_1() {
      val text = """
          REAL (
            WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            })
          """
      parse(type_, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }

    @Test def test_6_1_1_1() {
      val text = """
          REAL (
            WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            })
          """
      parse(type_, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }

    @Test def test_6_1_1_1_1() {
      val text = """
          REAL
          """
      parse(realType, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }

    @Test def test_6_1_1_1_2() {
      val text = """
          (WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            })
          """
      parse(constraint, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }

    @Test def test_6_1_1_1_2_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(constraintSpec, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(elementSetSpecs, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(rootElementSetSpec, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(elementSetSpec, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(unions, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(intersections, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(intersectionElements, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(elements, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(subtypeElements, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_1x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(singleValue, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_2x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(containedSubtype, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_3x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(valueRange, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_4x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(permittedAlphabet, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_5x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(sizeConstraint, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_6x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(typeConstraint, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(innerTypeConstraints, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1() {
      val text = """
          { mantissa (-16777215..16777215),
            base (2),
            exponent (-125..128)
          }
          """
      parse(multipleTypeConstraints, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1() {
      val text = """
          { mantissa (-16777215..16777215),
            base (2),
            exponent (-125..128)
          }
          """
      parse(fullSpecification, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1() {
      val text = """
          mantissa (-16777215..16777215),
          base (2),
          exponent (-125..128)
          """
      parse(typeConstraints, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1() {
      val text = """
          mantissa (-16777215..16777215)
          """
      parse(namedConstraint, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_1() {
      val text = """
          mantissa
          """
      parse(identifier, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2() {
      val text = """
          (-16777215..16777215)
          """
      parse(componentConstraint, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1() {
      val text = """
          (-16777215..16777215)
          """
      parse(valueConstraint, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1() {
      val text = """
          (-16777215..16777215)
          """
      parse(constraint, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(constraintSpec, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(elementSetSpecs, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(rootElementSetSpec, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(elementSetSpec, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(unions, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(intersections, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(intersectionElements, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(elements, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(subtypeElements, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215..16777215
          """
      parse(valueRange, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215
          """
      parse(lowerEndPoint, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215
          """
      parse(lowerEndValue, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215
          """
      parse(value, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215
          """
      parse(builtinValue, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215
          """
      parse(integerValue, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_7_1_1_1_1_2_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1_1() {
      val text = """
          -16777215
          """
      parse(signedNumber, text) match {
        case Success(_, _) => ()
        case x => fail("Parse failure: " + x)
      }
    }
    
    @Test def test_6_1_1_1_2_1_1_1_1_1_1_1_1_1_8x() {
      val text = """
          WITH COMPONENTS {
              mantissa (-16777215..16777215),
              base (2),
              exponent (-125..128)
            }
          """
      parse(patternConstraint, text) match {
        case Success(_, _) => fail("Parse failure expected")
        case x => ()
      }
    }
  }
}

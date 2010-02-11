package org.asn1gen.runtime.printing

import org.asn1gen.{runtime => _rt_}
import org.asn1gen.runtime.{meta => _meta_}

import java.io.PrintWriter

object SimplePrinter {
  def print(out: PrintWriter, value: Any): Unit = {
    value match {
      case value: _rt_.AsnOctetString => {
        out.print("AsnOctetString(\"" + value.string + "\")")
      }
      case asnSequence: _rt_.AsnSequence => {
        val desc = asnSequence._desc
        out.print(desc.name)
        desc.children.foreach { case (name, _: _meta_.AsnSequenceMember) =>
          val child = asnSequence._child(name)
          child match {
            case None =>
            case Some(subValue: _rt_.AsnType) => {
              out.print(".")
              out.print(name)
              out.print("{ _ => Some(")
              this.print(out, subValue)
              out.print(") }")
            }
            case subValue: _rt_.AsnType => {
              out.print(".")
              out.print(name)
              out.print("{ _ => ")
              this.print(out, subValue)
              out.print(" }")
            }
          }
        }
      }
      case _ => {
        out.print("/**")
        out.print(value)
        out.print("**/")
      }
    }
  }
}

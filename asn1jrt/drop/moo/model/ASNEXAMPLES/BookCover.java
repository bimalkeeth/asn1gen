/* This file was generated by asn1gen */

package moo.model.ASNEXAMPLES;

import org.asn1gen.runtime.java.*;

import static org.asn1gen.runtime.java.Statics.*;

@SuppressWarnings("unused")
public class BookCover extends org.asn1gen.runtime.java.AsnEnumeration {
  public static BookCover EMPTY = new BookCover(0);

  public final long value;

  public BookCover(final long value) {
    this.value = value;
  }

  public static final BookCover hardCover = new BookCover(0);
  public static final BookCover paperBack = new BookCover(1);

  public static BookCover of(final String name) {
    if (name.equals("hardCover")) {
      return hardCover;
    }

    if (name.equals("paperBack")) {
      return paperBack;
    }

    throw new org.asn1gen.runtime.java.BadEnumerationException(
      "Unrecogonised enumeration value + '" + name + "'");
  }

  public static BookCover of(final int value) {
    switch (value) {
      case 0: return hardCover;
      case 1: return paperBack;
      default: return new BookCover(value);
    }
  }
}

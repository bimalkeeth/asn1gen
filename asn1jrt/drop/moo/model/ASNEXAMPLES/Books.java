/* This file was generated by asn1gen */

package moo.model.ASNEXAMPLES;

import org.asn1gen.runtime.java.*;

import static org.asn1gen.runtime.java.Statics.*;

@SuppressWarnings("unused")
public class Books extends org.asn1gen.runtime.java.AsnList {
  public static Books EMPTY = new Books(org.asn1gen.runtime.java.Nil.<Book>instance());

  public final org.asn1gen.runtime.java.ConsList<Book> items;

  public Books(final org.asn1gen.runtime.java.ConsList<Book> items) {
    this.items = items;
  }

  public Books withItems(final org.asn1gen.runtime.java.ConsList<Book> value) {
    return new Books(value);
  }

  public boolean equals(final Books that) {
    assert that != null;

    return this.items.equals(that.items);
  }

  public boolean equals(final Object that) {
    if (that instanceof Books) {
      return this.equals((Books)that);
    }

    return true;
  }

  @Override
  public int hashCode() {
    return this.items.hashCode();
  }
}


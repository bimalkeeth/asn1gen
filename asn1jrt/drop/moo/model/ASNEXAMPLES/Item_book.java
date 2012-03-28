/* This file was generated by asn1gen */

package moo.model.ASNEXAMPLES;

import org.asn1gen.runtime.java.*;

import static org.asn1gen.runtime.java.Statics.*;

public class Item_book extends Item {
  public final static Item_book EMPTY = new Item_book(Book.EMPTY);

  public final Book element;

  public Item_book(final Book element) {
    this.element = element;
  }

  public Book element() {
    return this.element;
  }

  public int choiceId() {
    return 1;
  }

  @Override
  public Option<Book> getBook() {
    return some(this.element);
  }

  @Override
  public Item_book withBook(final Book value) {
    return new Item_book(value);
  }

  public String choiceName() {
    return "book";
  }
}

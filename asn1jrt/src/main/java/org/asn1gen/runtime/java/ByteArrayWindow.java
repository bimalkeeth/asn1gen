package org.asn1gen.runtime.java;

import java.nio.ByteBuffer;

public class ByteArrayWindow {
  private final byte[] array;
  private final int start;
  private final int length;
  
  public ByteArrayWindow(final byte[] array, final int start, final int length) {
    assert start >= 0;
    assert start + length <= array.length;
    
    this.array = array;
    this.start = start;
    this.length = length;
  }
  
  public ByteArrayWindow(final byte[] array) {
    this(array, 0, array.length);
  }
  
  public ByteArrayWindow(final int size) {
    this(new byte[size]);
  }
  
  public ByteArrayWindow from(final int offset) {
    assert offset >= 0;
    assert start + offset <= length;
    return new ByteArrayWindow(array, start + offset, length - offset);
  }

  public ByteArrayWindow until(final int offset) {
    assert offset >= 0;
    assert offset <= length;
    
    return new ByteArrayWindow(array, start, offset);
  }
  
  public byte get(final int index) {
    assert index >= 0;
    assert index < length;
    
    return this.array[start + index];
  }
  
  public void set(final int index, final byte value) {
    assert index >= 0;
    assert index < length;
    
    this.array[start + index] = value;
  }
  
  public ByteBuffer toByteBuffer() {
    return ByteBuffer.wrap(array, start, length);
  }

  public static ByteArrayWindow to(final byte[] array) {
    return new ByteArrayWindow(array, 0, array.length);
  }
}

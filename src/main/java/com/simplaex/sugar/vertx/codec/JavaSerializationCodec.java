package com.simplaex.sugar.vertx.codec;

import com.simplaex.bedrock.Numbers;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.*;

public class JavaSerializationCodec<T extends Serializable> extends ValueCodec<T> {

  public JavaSerializationCodec(final Class<T> clazz) {
    super(clazz);
  }

  public static <T extends Serializable> JavaSerializationCodec<T> forClass(final Class<T> clazz) {
    return new JavaSerializationCodec<>(clazz);
  }

  @Override
  @SneakyThrows
  public void encodeToWire(@Nonnull final Buffer buffer, final T t) {
    new ObjectOutputStream(new OutputStream() {
      @Override
      public void write(final int b) {
        buffer.appendByte((byte) b);
      }
    }).writeObject(t);
  }

  @Override
  @SneakyThrows
  public T decodeFromWire(@Nonnegative final int pos, @Nonnull final Buffer buffer) {
    final ObjectInputStream s = new ObjectInputStream(new InputStream() {
      int p = pos;

      @Override
      public int read() {
        if (p >= buffer.length()) {
          return -1;
        }
        return Numbers.byteToInt(buffer.getByte(p++));
      }
    });
    @SuppressWarnings("unchecked") final T object = (T) s.readObject();
    return object;
  }
}

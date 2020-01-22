package com.simplaex.sugar.vertx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class CborCodec<T> extends UserCodec<T, T> {

  private final ObjectMapper mapper;
  private final Class<T> clazz;

  public CborCodec(@Nonnull final Class<T> clazz) {
    super(clazz.getCanonicalName());
    this.mapper = new ObjectMapper(new CBORFactory());
    this.clazz = clazz;
  }

  public static <T> CborCodec<T> forClass(final Class<T> clazz) {
    return new CborCodec<>(clazz);
  }

  @Override
  @SneakyThrows
  public void encodeToWire(@Nonnull final Buffer buffer, final T t) {
    final byte[] bytes = mapper.writeValueAsBytes(t);
    buffer.appendInt(bytes.length);
    buffer.appendBytes(bytes);
  }

  @Override
  @SneakyThrows
  public T decodeFromWire(@Nonnegative final int pos, @Nonnull final Buffer buffer) {
    final int len = buffer.getInt(pos);
    final byte[] bytes = new byte[len];
    buffer.getBytes(pos, pos + 4 + len, bytes);
    return mapper.readValue(bytes, clazz);
  }

  @Override
  public T transform(final T t) {
    return t;
  }
}

package com.simplaex.sugar.vertx.codec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplaex.sugar.vertx.codec.ValueCodec;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class ObjectMapperCodec<T> extends ValueCodec<T> {

  private final ObjectMapper mapper;

  public ObjectMapperCodec(@Nonnull final Class<T> clazz, @Nonnull final JsonFactory jsonFactory) {
    super(clazz);
    this.mapper = new ObjectMapper(jsonFactory);
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
    return mapper.readValue(bytes, getForClass());
  }
}

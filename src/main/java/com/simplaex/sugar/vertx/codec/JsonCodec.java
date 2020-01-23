package com.simplaex.sugar.vertx.codec;

import com.fasterxml.jackson.core.JsonFactory;

import javax.annotation.Nonnull;

public class JsonCodec<T> extends ObjectMapperCodec<T> {

  public JsonCodec(@Nonnull final Class<T> clazz) {
    super(clazz, new JsonFactory());
  }

  public static <T> JsonCodec<T> forClass(final Class<T> clazz) {
    return new JsonCodec<>(clazz);
  }
}

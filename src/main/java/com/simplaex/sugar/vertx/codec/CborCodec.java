package com.simplaex.sugar.vertx.codec;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import javax.annotation.Nonnull;

public class CborCodec<T> extends ObjectMapperCodec<T> {

  public CborCodec(@Nonnull final Class<T> clazz) {
    super(clazz, new CBORFactory());
  }

  public static <T> CborCodec<T> forClass(final Class<T> clazz) {
    return new CborCodec<>(clazz);
  }
}

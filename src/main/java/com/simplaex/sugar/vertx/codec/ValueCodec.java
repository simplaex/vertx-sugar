package com.simplaex.sugar.vertx.codec;

import lombok.Getter;

import javax.annotation.Nonnull;

public abstract class ValueCodec<T> extends UserCodec<T, T> {

  @Getter
  private final Class<T> forClass;

  public ValueCodec(@Nonnull final Class<T> forClass) {
    super(forClass.getCanonicalName());
    this.forClass = forClass;
  }

  @Override
  public T transform(final T t) {
    return t;
  }
}

package com.simplaex.sugar.vertx;

import io.vertx.core.eventbus.MessageCodec;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class UserCodec<S, R> implements MessageCodec<S, R> {

  private final String name;

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final byte systemCodecID() {
    return -1;
  }
}

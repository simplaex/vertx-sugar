package com.simplaex.sugar.vertx;

import com.simplaex.bedrock.ThrowingBiFunction;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;

import javax.annotation.Nonnull;

public class GenericVerticleFactory implements VerticleFactory {

  private final String prefix;
  private final ThrowingBiFunction<String, ClassLoader, Verticle> verticleCreator;

  public GenericVerticleFactory(
    @Nonnull final String prefix,
    @Nonnull final ThrowingBiFunction<String, ClassLoader, Verticle> verticleCreator
  ) {
    this.prefix = prefix;
    this.verticleCreator = verticleCreator;
  }

  @Override
  public String prefix() {
    return prefix;
  }

  @Override
  public Verticle createVerticle(
    final String verticleName,
    final ClassLoader classLoader
  ) throws Exception {
    return verticleCreator.execute(verticleName, classLoader);
  }
}

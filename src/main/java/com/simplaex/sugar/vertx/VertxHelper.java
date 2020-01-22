package com.simplaex.sugar.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class VertxHelper {

  private final Vertx vertx;

  public void deployVerticle(final Class<?> clazz, final Handler<AsyncResult<String>> handler) {
    vertx.deployVerticle("guice:" + clazz.getCanonicalName(), handler);
  }

}

package com.simplaex.sugar.vertx.web;

import com.simplaex.http.StatusCode;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;

public class HealthAndStatusReporter implements Handler<RoutingContext> {

  @Nonnull
  private final JsonObject info;

  @Nonnull
  private final Consumer<Handler<AsyncResult<?>>> statusInfo;

  public HealthAndStatusReporter(
    @Nullable final Consumer<Handler<AsyncResult<?>>> statusInfoHandler,
    @Nonnull final ClassLoader classLoader
  ) {
    final InputStream resource = classLoader.getResourceAsStream("git-commit.json");
    this.info = Optional
      .ofNullable(resource)
      .map(is -> new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next())
      .map(JsonObject::new)
      .orElseGet(JsonObject::new);
    this.statusInfo = statusInfoHandler == null
      ? (handler -> handler.handle((AsyncResult<?>) Future.succeededFuture("ok")))
      : statusInfoHandler;
  }

  @Override
  public void handle(@Nonnull final RoutingContext context) {
    try {
      statusInfo.accept(result -> {
        if (result.failed()) {
          RouteUtil.error(context, StatusCode.INTERNAL_SERVER_ERROR, result.cause());
          return;
        }
        final Object status = result.result();
        final String json = new JsonObject()
          .put("status", status == null ? "ok" : status)
          .put("datetime", Instant.now())
          .put("commit", info)
          .encodePrettily();
        context
          .response()
          .setStatusCode(StatusCode.OK.getCode())
          .putHeader("Content-Type", "application/json")
          .end(json);
      });
    } catch (final Exception exc) {
      RouteUtil.error(context, StatusCode.INTERNAL_SERVER_ERROR, exc);
    }
  }
}

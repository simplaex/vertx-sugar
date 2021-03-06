package com.simplaex.sugar.vertx.web;

import com.simplaex.bedrock.ArrayMap;
import com.simplaex.bedrock.Mapping;
import com.simplaex.bedrock.Pair;
import com.simplaex.bedrock.Seq;
import com.simplaex.http.StatusCode;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

@UtilityClass
@Log4j2
public class RouteUtil {

  public static void appendCause(final JsonObject obj, @Nullable final Throwable exc) {
    if (exc == null) {
      return;
    }
    final JsonObject cause = new JsonObject();
    cause.put("exception", exc.getClass().getName());
    cause.put("message", Optional.ofNullable(exc.getMessage()).orElse(""));
    final Seq<StackTraceElement> stackTrace = Seq.wrap(exc.getStackTrace());
    stackTrace.headOptional().ifPresent(trace -> cause.put("location", new JsonObject()
      .put("class", trace.getClassName())
      .put("method", trace.getMethodName())
      .put("file", trace.getFileName())
      .put("line", trace.getLineNumber())));
    appendCause(cause, exc.getCause());
    obj.put("cause", cause);
  }

  @Nonnull
  public static Handler<Throwable> errorHandler(
    @Nonnull final RoutingContext context,
    @Nonnull final StatusCode status
  ) {
    return exc -> error(context, status, exc);
  }

  public static void error(
    @Nonnull final RoutingContext context,
    @Nonnull final StatusCode status
  ) {
    error(context, status, null);
  }

  public static void error(
    @Nonnull final RoutingContext context,
    @Nonnull final StatusCode status,
    @Nullable final Throwable cause
  ) {
    final JsonObject errorMessage = new JsonObject();
    errorMessage.put("code", status.getCode());
    errorMessage.put("message", status.getLabel());
    appendCause(errorMessage, cause);
    context
      .response()
      .setStatusCode(status.getCode())
      .putHeader("Content-Type", "application/json")
      .end(errorMessage.encodePrettily());
  }

  public static void redirect(
    @Nonnull final RoutingContext context,
    @Nonnull final String target
  ) {
    context.response().setStatusCode(308).putHeader("Location", target).end();
  }

  public static void seeOther(
    @Nonnull final RoutingContext context,
    @Nonnull final String target
  ) {
    context.response().setStatusCode(303).putHeader("Location", target).end();
  }

  @Nonnull
  public static String urlEncode(@Nonnull final String data) {
    try {
      return URLEncoder.encode(data, "UTF-8");
    } catch (final Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  @Nonnull
  public static String urlDecode(@Nonnull final String data) {
    try {
      return URLDecoder.decode(data, "UTF-8");
    } catch (final Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  @Nonnull
  public static Mapping<String, String> readFormData(@Nonnull final String body) {
    return Arrays
      .stream(body.split("&"))
      .map(str -> str.split("=", 2))
      .filter(arr -> arr.length == 2)
      .map(arr -> Pair.of(arr[0], arr[1]))
      .collect(ArrayMap.builder());
  }

  public static void putCorsHeaders(@Nonnull final HttpServerResponse httpServerResponse) {
    httpServerResponse
      .putHeader("Access-Control-Allow-Origin", "*")
      .putHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
      .putHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, User-Agent");
  }

  public static void forwardRequest(
    @Nonnull final HttpClient client,
    @Nonnull final RoutingContext context,
    @Nonnull final String targetHost,
    @Nonnegative final int targetPort,
    @Nullable final Consumer<HttpClientRequest> requestAugmentor,
    @Nullable final Consumer<HttpServerResponse> responseAugmentor
  ) {
    final HttpServerRequest request = context.request();
    final HttpClientRequest forwardingRequest = client.request(
      request.method(),
      targetPort,
      targetHost,
      request.uri(),
      response -> {
        final HttpServerResponse resp = context.response();
        resp.setStatusCode(response.statusCode());
        resp.headers().setAll(response.headers());
        if (responseAugmentor != null) {
          responseAugmentor.accept(resp);
        }
        response.handler(resp::write);
        response.endHandler(__ -> resp.end());
      }
    );

    forwardingRequest.headers().addAll(request.headers());
    if (requestAugmentor != null) {
      requestAugmentor.accept(forwardingRequest);
    }
    forwardingRequest.setChunked(true);

    if (request.isEnded()) {
      log.warn("Request was already ended - maybe you forgot to pause() it.");
      forwardingRequest.end();
    } else {
      request.handler(forwardingRequest::write);
      request.endHandler(__ -> forwardingRequest.end());
      request.resume();
    }
  }

}

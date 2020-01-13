package com.simplaex.sugar.vertx.web;

import com.simplaex.bedrock.ArrayMap;
import com.simplaex.bedrock.Mapping;
import com.simplaex.bedrock.Pair;
import com.simplaex.http.StatusCode;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;

@UtilityClass
public class RouteUtil {

    public static void appendCause(final JsonObject obj, final Throwable exc) {
        if (exc == null) {
            return;
        }
        final JsonObject cause = new JsonObject();
        cause.put("exception", exc.getClass().getName());
        cause.put("message", exc.getMessage());
        appendCause(cause, exc.getCause());
        obj.put("cause", cause);
    }

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

}

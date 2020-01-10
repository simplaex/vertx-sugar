package com.simplaex.sugar.vertx.web;

import com.simplaex.http.StatusCode;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

}

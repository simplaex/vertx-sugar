package com.simplaex.sugar.vertx.web;

import com.simplaex.http.StatusCode;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class RouteUtil {

    public static void error(
            @Nonnull final RoutingContext context,
            @Nonnull final StatusCode status
    ) {
        final JsonObject errorMessage = new JsonObject();
        errorMessage.put("code", status.getCode());
        errorMessage.put("message", status.getLabel());
        context
                .response()
                .setStatusCode(status.getCode())
                .putHeader("Content-Type", "application/json")
                .end(errorMessage.encodePrettily());
    }

}

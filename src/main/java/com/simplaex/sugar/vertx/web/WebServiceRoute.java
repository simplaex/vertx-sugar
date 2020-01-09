package com.simplaex.sugar.vertx.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface WebServiceRoute extends Handler<RoutingContext> {

    String handles();

}

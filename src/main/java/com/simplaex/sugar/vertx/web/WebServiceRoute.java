package com.simplaex.sugar.vertx.web;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.Set;

public interface WebServiceRoute extends Handler<RoutingContext> {

    Set<String> handles();

}

package com.simplaex.sugar.vertx.web;

import com.simplaex.bedrock.Mapping;
import com.simplaex.bedrock.Set;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface WebServiceRoute extends Handler<RoutingContext> {

    default Mapping<String, String> redirects() {
        return Mapping.empty();
    }

    Set<String> handles();

}

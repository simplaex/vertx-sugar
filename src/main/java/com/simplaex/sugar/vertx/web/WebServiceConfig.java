package com.simplaex.sugar.vertx.web;

import com.simplaex.sugar.guice.BindInstance;

@BindInstance
public interface WebServiceConfig {

    String getWebServiceApiPrefix();

    int getWebServicePort();
}

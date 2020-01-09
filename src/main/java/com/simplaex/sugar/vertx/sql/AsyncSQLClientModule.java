package com.simplaex.sugar.vertx.sql;

import com.google.inject.Provides;
import com.simplaex.sugar.guice.SimpleModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;

import javax.inject.Singleton;

public class AsyncSQLClientModule extends SimpleModule {

  @Provides
  @Singleton
  public AsyncSQLClient getAsyncSQLClient(final Vertx vertx, final DatabaseConfig config) {
    final JsonObject clientConfig = new JsonObject()
      .put("username", config.getDatabaseUsername())
      .put("password", config.getDatabasePassword())
      .put("database", config.getDatabaseDatabase())
      .put("port", config.getDatabasePort())
      .put("host", config.getDatabaseHost());
    return PostgreSQLClient.createShared(vertx, clientConfig);
  }

}

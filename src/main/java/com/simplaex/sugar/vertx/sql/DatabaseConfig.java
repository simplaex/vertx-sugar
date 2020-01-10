package com.simplaex.sugar.vertx.sql;

import com.simplaex.sugar.guice.BindInstance;

@BindInstance
public interface DatabaseConfig {

    String getDatabaseHost();

    int getDatabasePort();

    String getDatabaseUsername();

    String getDatabasePassword();

    String getDatabaseDatabase();

    String getDatabaseSchema();
}

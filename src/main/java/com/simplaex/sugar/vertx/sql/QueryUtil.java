package com.simplaex.sugar.vertx.sql;

import com.simplaex.bedrock.Strings;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.util.Arrays;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Log4j2
public class QueryUtil {

    private final DatabaseConfig config;
    private final AsyncSQLClient sqlClient;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public class Query {

        @Getter(AccessLevel.PRIVATE)
        private final String query;
        private final JsonArray values;
        private Handler<Throwable> onFailure = exc -> log.error("Error handling query={}", getQuery(), exc);

        public Query onFailure(final Handler<Throwable> handler) {
            onFailure = handler;
            return this;
        }

        public void execute(final Handler<ResultSet> handler) {
            sqlClient.getConnection(connF -> {
                if (connF.failed()) {
                    log.error("Failed getting a connection for execution query={}", query, connF.cause());
                    onFailure.handle(connF.cause());
                    return;
                }
                final SQLConnection connection = connF.result();
                connection.queryWithParams(query, values, resultF -> {
                    try {
                        if (resultF.failed()) {
                            log.error("Failed executing query={}", query, resultF.cause());
                            onFailure.handle(resultF.cause());
                            return;
                        }
                        log.info("Successfully executed query={}", query);
                        handler.handle(resultF.result());
                    } catch (final Exception exc) {
                        if (onFailure != null) {
                            onFailure.handle(exc);
                        }
                    } finally {
                        connection.close();
                    }
                });
            });
        }
    }

    public Query query(final String query, final Object... values) {
        final Strings.Template template = Strings.template("<<", ">>", query);
        final String augmentedQuery = template.apply(variable -> {
            switch (variable) {
                case "schema":
                    return config.getDatabaseSchema();
                case "database":
                    return config.getDatabaseDatabase();
                case "user":
                case "username":
                    return config.getDatabaseUsername();
                case "host":
                case "hostname":
                    return config.getDatabaseHost();
                case "port":
                    return Integer.toString(config.getDatabasePort());
                default:
                    return "<<" + variable + ">>";
            }
        });
        final JsonArray valuesArray = new JsonArray();
        Arrays.stream(values).forEach(valuesArray::add);
        return new Query(augmentedQuery, valuesArray);
    }

}
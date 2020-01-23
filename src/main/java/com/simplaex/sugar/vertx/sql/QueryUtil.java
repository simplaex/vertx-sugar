package com.simplaex.sugar.vertx.sql;

import com.simplaex.bedrock.Strings;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

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

    @Nonnull
    public Query onFailure(@Nonnull final Handler<Throwable> handler) {
      onFailure = handler;
      return this;
    }

    public void execute(@Nonnull final Handler<ResultSet> handler) {
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

    public void executeUpdate(@Nonnull final Handler<UpdateResult> handler) {
      sqlClient.getConnection(connF -> {
        if (connF.failed()) {
          log.error("Failed getting a connection for execution query={}", query, connF.cause());
          onFailure.handle(connF.cause());
          return;
        }
        final SQLConnection connection = connF.result();
        connection.updateWithParams(query, values, resultF -> {
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

    /**
     * Executes the statement as a batch with the values from the given array.
     */
    public void executeBatch(
      @Nonnull final Handler<List<Integer>> handler,
      @Nonnull final List<JsonArray> batchValues
    ) {
      sqlClient.getConnection(connF -> {
        if (connF.failed()) {
          log.error("Failed getting a connection for execution query={}", query, connF.cause());
          onFailure.handle(connF.cause());
          return;
        }
        final SQLConnection connection = connF.result();
        connection.batchWithParams(query, batchValues, resultF -> {
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

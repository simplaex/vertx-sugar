package com.simplaex.sugar.vertx;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.vertx.core.Vertx;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Associated a Vertx-Instance with a Guice Injector.
 */
@UtilityClass
public class VertxInjector {

  public static class CreationException extends Exception {
    CreationException(final Exception cause) {
      super(cause.getMessage(), cause);
    }
  }

  private static final Map<Vertx, Injector> injectors = new WeakHashMap<>();

  /**
   * Creates a new Injector instance and associates it with the given vertx instance.
   */
  public static Injector injector(final Vertx vertx, final Module... modules) throws CreationException {
    synchronized (injectors) {
      if (injectors.containsKey(vertx)) {
        throw new CreationException(new IllegalStateException("vertx instance is already associated with an injector"));
      }
      final Injector injector;
      try {
        injector = Guice.createInjector(Stage.PRODUCTION, modules);
      } catch (final Exception exc) {
        throw new CreationException(exc);
      }
      injectors.put(vertx, injector);
      return injector;
    }
  }

  /**
   * Retrieves the injector associated with this vertx instance.
   */
  @Nonnull
  public static Injector injector(@Nonnull final Vertx vertx) {
    Objects.requireNonNull(vertx, "vertx instance required to be non-null");
    final Injector injector;
    synchronized (injectors) {
      injector = injectors.get(vertx);
    }
    if (injector == null) {
      throw new IllegalStateException("No injector associated with vertx instance " + vertx);
    }
    return injector;
  }

}

package com.simplaex.sugar.vertx;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.simplaex.bedrock.AsyncExecutionException;
import com.simplaex.bedrock.EnvironmentVariables;
import com.simplaex.bedrock.Promise;
import com.simplaex.sugar.vertx.sql.DatabaseConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Log4j2LogDelegateFactory;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.ext.web.Router;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.flywaydb.core.Flyway;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.simplaex.sugar.guice.SimpleModule.instance;
import static com.simplaex.sugar.guice.SimpleModule.module;

/**
 * The usual pattern for starting up a vertx application given some configuration class,
 * a guice module factory (which is a function that creates a guice module from
 * that configuration and a vertx instance) and a MainVerticle class.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class VertxStarter<Config, MainVerticle extends Verticle> {

  static {
    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, Log4j2LogDelegateFactory.class.getName());
  }

  private final Logger log;
  private final Vertx vertx;

  private final Config config;

  private final Class<Config> configClass;
  private final Class<MainVerticle> mainVerticleClass;
  private final BiFunction<Vertx, Config, Module> moduleFactory;
  private final Supplier<Module>[] otherModules;

  public final static class StartupException extends Exception {
    StartupException(
      @Nonnull final String message,
      @Nonnull final Throwable cause
    ) {
      super(message, cause);
    }
  }

  /**
   * Starts a Vertx-Application and handles the usual errors when starting up.
   *
   * @param log               The logger of the calling Main-class.
   *                          This is so such that the logger is the logger of the respective application.
   * @param configClass       The class of the configuration pojo to use.
   * @param mainVerticleClass The class of the main verticle to be deployed as first verticle.
   * @param moduleFactory     A function that creates the app's guice module.
   * @param otherModules      An optional varg list of suppliers of other guice modules.
   *                          This is an array of suppliers such that construction and exception handling can
   *                          happen here.
   */
  @SafeVarargs
  public static <Config, MainVerticle extends Verticle> void start(
    @Nonnull final Logger log,
    @Nonnull final Class<Config> configClass,
    @Nullable final BiConsumer<Logger, Config> prestart,
    @Nonnull final Class<MainVerticle> mainVerticleClass,
    @Nonnull final BiFunction<Vertx, Config, Module> moduleFactory,
    @Nonnull final Supplier<Module>... otherModules
  ) {
    adjustLoggingConfig();
    try {
      final VertxStarter<Config, MainVerticle> vertxStarter = new VertxStarter<>(
        log,
        configClass,
        mainVerticleClass,
        moduleFactory,
        otherModules
      );
      if (prestart != null) {
        prestart.accept(log, vertxStarter.config);
      }
      vertxStarter.start();
    } catch (final StartupException exc) {
      handleStartupException(log, exc);
    }
  }

  private static void handleStartupException(
    @Nonnull final Logger log,
    @Nonnull final StartupException exc
  ) {
    log.error(exc.getMessage(), exc);
    System.exit(1);
  }

  @SuppressWarnings("unchecked")
  private static <T> Supplier<T>[] emptySupplierArray() {
    return new Supplier[0];
  }

  @SafeVarargs
  private VertxStarter(
    @Nullable final Logger log,
    @Nonnull final Class<Config> configClass,
    @Nonnull final Class<MainVerticle> mainVerticleClass,
    @Nonnull final BiFunction<Vertx, Config, Module> moduleFactory,
    @Nullable final Supplier<Module>... otherModules
  ) throws StartupException {
    Objects.requireNonNull(configClass, "'configClass' must not be null");
    Objects.requireNonNull(mainVerticleClass, "'mainVerticleClass' must not be null");
    Objects.requireNonNull(moduleFactory, "'moduleFactory' must not be null");
    this.log = log == null ? LogManager.getLogger(getClass()) : log;
    this.configClass = configClass;
    this.mainVerticleClass = mainVerticleClass;
    this.moduleFactory = moduleFactory;
    this.otherModules = otherModules == null ? emptySupplierArray() : otherModules;
    final Config config = readConfigFromEnvironment();
    this.config = config;
    this.vertx = createVertx(config);
  }

  private Vertx createVertx(final Config config) throws StartupException {
    try {
      final VertxOptions vertxOptions = new VertxOptions();
      // put vertx options here
      final Vertx vertx = Vertx.vertx(vertxOptions);
      vertx.registerVerticleFactory(new GenericVerticleFactory("guice", (verticleName, classLoader) -> {
        final Injector injector = VertxInjector.injector(vertx);
        final String clazzName = VerticleFactory.removePrefix(verticleName);
        final Class<?> clazz = classLoader.loadClass(clazzName);
        if (!Verticle.class.isAssignableFrom(clazz)) {
          throw new IllegalArgumentException(clazzName + " does not implement the Verticle interface");
        }
        @SuppressWarnings("unchecked") final Class<? extends Verticle> verticleClass = (Class<? extends Verticle>) clazz;
        return injector.getInstance(verticleClass);
      }));
      return vertx;
    } catch (final Exception exc) {
      throw new StartupException("Failed to start vertx", exc);
    }
  }

  /**
   * Since all of our services are deployed as docker images they are configured
   * by passing environment variables. The EnvironmentVariables utility class from
   * bedrock (an open-source utility library developed by us) reads the configuration
   * for all getter/setters from the given configuration class from the environment
   * variables present.
   */
  private Config readConfigFromEnvironment() throws StartupException {
    try {
      final Config config = EnvironmentVariables.read(configClass);
      // If a configuration instance implements CheckableConfig the check-method will
      // be invoked which may throw an exception if the configuration is no good.
      if (config instanceof CheckableConfig) {
        ((CheckableConfig) config).check();
      }
      return config;
    } catch (final Exception exc) {
      throw new StartupException("Failed to read configuration", exc);
    }
  }

  private Injector initializeGuiceApplication() throws StartupException {
    // Create an array of n + 2 items:
    // - 1 for the application module (at index 0)
    // - 1 for an ad-hoc module that binds vertx and configuration instances (at index 1)
    // - n for the other modules (at index n+2)
    final Module[] modules = new Module[2 + otherModules.length];
    // Initialize the other modules one by one.
    for (int i = 0; i < otherModules.length; i += 1) {
      try {
        modules[i + 2] = otherModules[i].get();
      } catch (final Exception exc) {
        throw new StartupException("Failed to create module #" + i, exc);
      }
    }
    // Initialize said ad-hoc module at index=1
    modules[1] = module(
      instance(Vertx.class, vertx),
      instance(EventBus.class, vertx.eventBus()),
      instance(FileSystem.class, vertx.fileSystem()),
      instance(Router.class, Router.router(vertx)),
      instance(configClass, config)
    );
    // Finally initialize the main module at index=0
    try {
      modules[0] = moduleFactory.apply(vertx, config);
    } catch (final Exception exc) {
      throw new StartupException("Failed to create main module", exc);
    }
    try {
      return VertxInjector.injector(vertx, modules);
    } catch (final Exception exc) {
      throw new StartupException("Failed to create Guice injector", exc);
    }
  }

  private void start() throws StartupException {
    log.info("Starting up...");
    final Injector injector = initializeGuiceApplication();

    // Bring up the MainVerticle which is supposed to deploy (i.e. start) everything.
    final DeploymentOptions deploymentOptions = new DeploymentOptions();
    final Promise<String> promise = Promise.promise();
    final Verticle mainVerticle = injector.getInstance(mainVerticleClass);
    vertx.deployVerticle(mainVerticle, deploymentOptions, result -> {
      if (!result.succeeded()) {
        promise.fail(result.cause());
      } else {
        promise.success(result.result());
      }
    });
    try {
      final String result = promise.get();
      log.info("Successfully deployed {}", result);
    } catch (final AsyncExecutionException exc) {
      throw new StartupException("Failed to deploy main verticle: " + mainVerticleClass, exc.getCause());
    }
  }

  private static void adjustLoggingConfig() {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    config.getRootLogger().setLevel(Level.INFO);
    ctx.updateLoggers();
  }

  public static void initDatabase(@Nonnull final Logger log, @Nonnull final DatabaseConfig config) {
    final String jdbcUrl = String.format(
      "jdbc:postgresql://%s:%s/%s",
      config.getDatabaseHost(),
      config.getDatabasePort(),
      config.getDatabaseDatabase()
    );
    log.info("Attempting Flyway migration using jdbcUrl={}", jdbcUrl);
    Flyway
      .configure()
      .dataSource(jdbcUrl, config.getDatabaseUsername(), config.getDatabasePassword())
      .schemas(config.getDatabaseSchema())
      .load()
      .migrate();
    log.info("Successfully migrated.");
  }
}

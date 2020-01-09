package com.simplaex.sugar.vertx.web;

import com.google.inject.Injector;
import com.simplaex.sugar.vertx.VertxInjector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static com.simplaex.bedrock.Control.type;
import static com.simplaex.bedrock.Control.typeOf;

@Log4j2
@RequiredArgsConstructor
public class WebServiceVerticle extends AbstractVerticle {

    private WebServiceConfig getConfig(final Injector injector) {
        try {
            return injector.getProvider(WebServiceConfig.class).get();
        } catch (final Exception exc) {
            log.warn("No config was bound in injector", exc);
            // if no config was bound
            return new WebServiceConfig() {
                @Override
                public String getWebServiceApiPrefix() {
                    return "";
                }

                @Override
                public int getWebServicePort() {
                    return 8080;
                }
            };
        }
    }

    @Override
    public final void start(final Future<Void> startFuture) {
        final Injector injector = VertxInjector.injector(vertx);
        final WebServiceConfig config = getConfig(injector);
        final Router router = Router.router(vertx);
        injector.getBindings().forEach((key, binding) -> {
            typeOf(binding.getProvider().get(),
                    type(WebServiceRoute.class, route -> {
                        final String path = config.getWebServiceApiPrefix() + route.handles();
                        log.info("Registering route {} on {}", path, route.getClass());
                        router.route(path).handler(route);
                        return null;
                    }));
        });
        final HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router).listen(config.getWebServicePort(), result -> {
            if (result.succeeded()) {
                log.info("Listening on port 8080");
                startFuture.complete();
            } else {
                log.error("Failed to start webserver", result.cause());
                startFuture.fail(result.cause());
            }
        });
    }

}

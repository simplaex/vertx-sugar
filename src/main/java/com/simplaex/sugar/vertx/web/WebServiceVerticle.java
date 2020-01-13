package com.simplaex.sugar.vertx.web;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.simplaex.bedrock.Strings;
import com.simplaex.sugar.vertx.VertxInjector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

import static com.simplaex.bedrock.Control.typeOf;
import static com.simplaex.bedrock.Control.type_;

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
        router.route().handler(BodyHandler.create());
        for (Map.Entry<Key<?>, Binding<?>> entry : injector.getBindings().entrySet()) {
            final Binding<?> binding = entry.getValue();
            typeOf(binding.getProvider().get(),
                    type_(WebServiceRoute.class, route -> {
                        route.handles().forEach(p -> {
                            final String path = config.getWebServiceApiPrefix() + p;
                            log.info("Registering route {} on {}", path, route.getClass());
                            router.route(path).handler(route);
                        });
                        route.redirects().forEach((from, to) -> {
                            final String pathFrom = config.getWebServiceApiPrefix() + from;
                            final String pathTo = config.getWebServiceApiPrefix() + to;
                            log.info("Registering redirect from {} to {}", pathFrom, pathTo);
                            final Strings.Template template = Strings.template(":([a-zA-Z](_?[a-zA-Z0-9]+))*", pathTo);
                            router.get(pathFrom).handler(context -> {
                                final String path = template.apply(k -> context.request().getParam(k.substring(1)));
                                final String query = context.request().query();
                                final String pathWithQuery = query == null || query.isEmpty() ? path : path + "?" + query;
                                log.info("Redirecting to {}", pathWithQuery);
                                RouteUtil.redirect(context, pathWithQuery);
                            });
                        });
                    }));
        }
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.index.service.vertx;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import static org.kie.kogito.index.service.vertx.IndexRouteRegistrar.CONFIG_KEY_UI_PATH;

@ApplicationScoped
public class VertxRouterSetup {

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "false")
    Boolean authEnabled;

    @ConfigProperty(name = "kogito.data-index.vertx-graphql.ui.path", defaultValue = "/graphiql")
    String graphUIPath;

    @ConfigProperty(name = CONFIG_KEY_UI_PATH, defaultValue = "")
    Optional<String> indexUIPath;

    @Inject
    Vertx vertx;

    void setupRouter(@Observes Router router) {
        router.route().handler(LoggerHandler.create());
        GraphiQLHandler graphiQLHandler = GraphiQLHandler.create(vertx, new GraphiQLHandlerOptions().setEnabled(true));
        if (Boolean.TRUE.equals(authEnabled)) {
            addGraphiqlRequestHeader(graphiQLHandler);
        }
        router.route().handler(BodyHandler.create());
        router.route(graphUIPath + "/*").handler(graphiQLHandler);
        if (indexUIPath.isEmpty()) {
            router.route("/").handler(ctx -> ctx.response().putHeader("location", graphUIPath + "/").setStatusCode(302).end());
        }
        router.route().handler(FaviconHandler.create(vertx));
    }

    protected void addGraphiqlRequestHeader(GraphiQLHandler graphiQLHandler) {
        graphiQLHandler.graphiQLRequestHeaders(rc -> MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.AUTHORIZATION, "Bearer " + getCurrentAccessToken(rc)));
    }

    private String getCurrentAccessToken(RoutingContext routingContext) {
        return Optional.ofNullable(routingContext.user())
                .map(user -> ((QuarkusHttpUser) user).getSecurityIdentity())
                .map(identity -> identity.getCredential(AccessTokenCredential.class))
                .map(AccessTokenCredential::getToken)
                .orElse("");
    }
}

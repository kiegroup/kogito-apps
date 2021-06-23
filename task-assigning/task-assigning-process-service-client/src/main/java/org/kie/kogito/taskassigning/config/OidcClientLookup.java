/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.taskassigning.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;

@ApplicationScoped
public class OidcClientLookup {

    private static final String DEFAULT_CLIENT = "Default";

    private final OidcClients oidcClients;

    @Inject
    public OidcClientLookup(OidcClients oidcClients) {
        this.oidcClients = oidcClients;
    }

    public OidcClient lookup(String clientId) {
        if (DEFAULT_CLIENT.equals(clientId)) {
            return oidcClients.getClient();
        } else {
            return oidcClients.getClient(clientId);
        }
    }
}
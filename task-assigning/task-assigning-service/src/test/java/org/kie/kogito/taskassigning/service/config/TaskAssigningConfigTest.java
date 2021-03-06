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

package org.kie.kogito.taskassigning.service.config;

import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.CLIENT_AUTH_USER;
import static org.kie.kogito.taskassigning.service.config.TaskAssigningConfigProperties.QUARKUS_OIDC_AUTH_SERVER_URL;

class TaskAssigningConfigTest {

    private static final String DATA_INDEX_SERVER_URL = "http://localhost:8180/graphql";
    private static final String AUTH_SERVER_URL = "http://localhost:8280/auth/realms/kogito";
    private static final String REALM = "kogito";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String CREDENTIALS_SECRET = "CREDENTIALS_SECRET";
    private static final String CLIENT_USER = "CLIENT_USER";
    private static final String CLIENT_PASSWORD = "CLIENT_PASSWORD";

    private TaskAssigningConfig config;

    @BeforeEach
    void setUp() {
        config = new TaskAssigningConfig();
    }

    @Test
    void isOidcTenantEnabled() {
        config.oidcTenantEnabled = true;
        assertThat(config.isOidcTenantEnabled()).isTrue();
    }

    @Test
    void isKeycloakSetTrue() {
        config.oidcTenantEnabled = true;
        assertThat(config.isKeycloakSet()).isTrue();
    }

    @Test
    void isKeycloakSetFalse() {
        config.oidcTenantEnabled = false;
        assertThat(config.isKeycloakSet()).isFalse();
    }

    @Test
    void isBasicAuthSetTrue() {
        config.oidcTenantEnabled = false;
        config.clientAuthUser = Optional.of(CLIENT_AUTH_USER);
        assertThat(config.isBasicAuthSet()).isTrue();
    }

    @Test
    void isBasicAuthSetFalseWhenKeycloakSet() {
        config.oidcTenantEnabled = true;
        config.clientAuthUser = Optional.of(CLIENT_AUTH_USER);
        assertThat(config.isBasicAuthSet()).isFalse();
    }

    @Test
    void isBasicAuthSetFalse() {
        config.oidcTenantEnabled = false;
        config.clientAuthUser = Optional.empty();
        assertThat(config.isBasicAuthSet()).isFalse();
    }

    @Test
    void getOidcAuthServerUrl() throws Exception {
        URL url = new URL(AUTH_SERVER_URL);
        config.oidcAuthServerUrl = Optional.of(url);
        assertThat(config.getOidcAuthServerUrl()).contains(url);
    }

    @Test
    void getOidcAuthServerCanonicUrl() throws Exception {
        URL canonicUrl = new URL("http://localhost:8280/auth");
        config.oidcAuthServerUrl = Optional.of(new URL(AUTH_SERVER_URL));
        assertThat(config.getOidcAuthServerCanonicUrl()).isEqualTo(canonicUrl);
    }

    @Test
    void getOidcAuthServerCanonicUrlFailureValueNotSet() {
        config.oidcAuthServerUrl = Optional.empty();
        assertThatThrownBy(() -> config.getOidcAuthServerCanonicUrl()).hasMessage("A configuration value must be set for the property: " + QUARKUS_OIDC_AUTH_SERVER_URL);
    }

    @Test
    void getOidcAuthServerCanonicUrlFailureMalformedKeycloakAuth() throws Exception {
        String malfFormedKeycloakAuthUrl = "http://localhost:8280/auth/notExpected/kogito";
        config.oidcAuthServerUrl = Optional.of(new URL(malfFormedKeycloakAuthUrl));
        assertThatThrownBy(() -> config.getOidcAuthServerCanonicUrl())
                .hasMessageContaining("%s doesn't look to be a valid Keycloak authentication domain", malfFormedKeycloakAuthUrl);
    }

    @Test
    void getOidcAuthServerRealm() throws Exception {
        config.oidcAuthServerUrl = Optional.of(new URL(AUTH_SERVER_URL));
        assertThat(config.getOidcAuthServerRealm()).isEqualTo(REALM);
    }

    @Test
    void getOidcAuthServerRealFailureValueNotSet() {
        config.oidcAuthServerUrl = Optional.empty();
        assertThatThrownBy(() -> config.getOidcAuthServerRealm()).hasMessage("A configuration value must be set for the property: " + QUARKUS_OIDC_AUTH_SERVER_URL);
    }

    @Test
    void getOidcAuthServerRealmMalformedKeycloakAuth() throws Exception {
        String malfFormedKeycloakAuthUrl = "http://localhost:8280/auth/notExpected/kogito";
        config.oidcAuthServerUrl = Optional.of(new URL(malfFormedKeycloakAuthUrl));
        assertThatThrownBy(() -> config.getOidcAuthServerRealm())
                .hasMessageContaining("%s doesn't look to be a valid Keycloak authentication domain", malfFormedKeycloakAuthUrl);
    }

    @Test
    void getOidcClientId() {
        config.oidcClientId = Optional.of(CLIENT_ID);
        assertThat(config.getOidcClientId()).contains(CLIENT_ID);
    }

    @Test
    void getOidcCredentialsSecret() {
        config.oidcCredentialsSecret = Optional.of(CREDENTIALS_SECRET);
        assertThat(config.getOidcCredentialsSecret()).contains(CREDENTIALS_SECRET);
    }

    @Test
    void getOidcClientAuthUser() {
        config.clientAuthUser = Optional.of(CLIENT_USER);
        assertThat(config.getClientAuthUser()).contains(CLIENT_USER);
    }

    @Test
    void getOidcClientAuthPassword() {
        config.clientAuthPassword = Optional.of(CLIENT_PASSWORD);
        assertThat(config.getClientAuthPassword()).contains(CLIENT_PASSWORD);
    }

    @Test
    void getDataIndexServerUrl() throws Exception {
        URL url = new URL(DATA_INDEX_SERVER_URL);
        config.dataIndexServerUrl = url;
        assertThat(config.getDataIndexServerUrl()).isEqualTo(url);
    }
}

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
package org.kie.kogito.index.service.graphql;

import org.junit.jupiter.api.Test;
import org.kie.kogito.index.model.ProcessDefinition;
import org.kie.kogito.index.model.ProcessInstance;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.schema.DataFetchingEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GraphQLSchemaManagerTest {

    GraphQLSchemaManagerImpl schemaManager = new GraphQLSchemaManagerImpl();
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testNullServiceUrl() {
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv(null, null))).isNull();
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("travels", null))).isNull();
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("demo.orders", null))).isNull();
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("demo.orderItems", null))).isNull();
    }

    @Test
    public void testNullProcessIdServiceUrl() {
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("travels", "/travels"))).isEmpty();
    }

    @Test
    public void testUrlProcessIdServiceUrl() {
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("travels", "http://localhost:8080/travels"))).isEqualTo("http://localhost:8080");
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("travels", "http://travels.example.com/travels"))).isEqualTo("http://travels.example.com");
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("demo.orders", "http://localhost:8080/orders"))).isEqualTo("http://localhost:8080");
        assertThat(schemaManager.getProcessInstanceServiceUrl(getEnv("demo.orderItems", "http://localhost:8080/orderItems"))).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testNullProcessDefinitionServiceUrl() {
        assertThat(schemaManager.getProcessDefinitionServiceUrl(getDefEnv(null, null, null))).isNull();
        assertThat(schemaManager.getProcessDefinitionServiceUrl(getDefEnv("travels", null, null))).isNull();
    }

    @Test
    public void testUrlProcessDefinitionServiceUrl() {
        assertThat(schemaManager.getProcessDefinitionServiceUrl(getDefEnv("travels", "1.0", "http://localhost:8080/travels"))).isEqualTo("http://localhost:8080");
        assertThat(schemaManager.getProcessDefinitionServiceUrl(getDefEnv("travels", "http://travels.example.com/travels"))).isEqualTo("http://travels.example.com");
    }

    @Test
    public void testUrlWithVersionProcessDefinitionServiceUrl() {
        assertThat(schemaManager.getProcessDefinitionServiceUrl(getDefEnv("callbackstatetimeouts", "0.0.1", "http://callbackstatetimeouts.example.com/callbackstatetimeouts/0.0.1")))
                .isEqualTo("http://callbackstatetimeouts.example.com");
    }

    private DataFetchingEnvironment getEnv(String processId, String endpoint) {
        DataFetchingEnvironment env = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(env.getSource()).thenReturn(getProcessInstance(processId, endpoint));
        return env;
    }

    private ProcessInstance getProcessInstance(String processId, String endpoint) {
        ProcessInstance pi = new ProcessInstance();
        pi.setProcessId(processId);
        pi.setEndpoint(endpoint);
        return pi;
    }

    private DataFetchingEnvironment getDefEnv(String id, String endpoint) {
        return getDefEnv(id, null, endpoint);
    }

    private DataFetchingEnvironment getDefEnv(String id, String version, String endpoint) {
        DataFetchingEnvironment env = Mockito.mock(DataFetchingEnvironment.class);
        Mockito.when(env.getSource()).thenReturn(getProcessDefinition(id, version, endpoint));
        return env;
    }

    private ProcessDefinition getProcessDefinition(String id, String version, String endpoint) {
        ProcessDefinition pd = new ProcessDefinition();
        pd.setId(id);
        pd.setVersion(version);
        pd.setEndpoint(endpoint);
        return pd;
    }
}

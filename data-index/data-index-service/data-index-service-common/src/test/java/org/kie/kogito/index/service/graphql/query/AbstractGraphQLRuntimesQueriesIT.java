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
package org.kie.kogito.index.service.graphql.query;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.kogito.event.process.ProcessDefinitionDataEvent;
import org.kie.kogito.event.process.ProcessInstanceDataEvent;
import org.kie.kogito.event.process.ProcessInstanceStateDataEvent;
import org.kie.kogito.event.process.ProcessInstanceVariableDataEvent;
import org.kie.kogito.event.process.ProcessInstanceVariableEventBody;

import org.kie.kogito.index.api.ExecuteArgs;
import org.kie.kogito.index.api.KogitoRuntimeClient;
import org.kie.kogito.index.event.KogitoJobCloudEvent;
import org.kie.kogito.index.service.graphql.GraphQLSchemaManagerImpl;
import org.kie.kogito.index.test.TestUtils;
import org.kie.kogito.jackson.utils.ObjectMapperFactory;
import org.kie.kogito.persistence.protobuf.ProtobufService;
import org.mockito.junit.jupiter.MockitoExtension;

import io.restassured.http.ContentType;

import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.kie.kogito.index.model.ProcessInstanceState.ACTIVE;
import static org.kie.kogito.index.test.TestUtils.getJob;
import static org.kie.kogito.index.test.TestUtils.getJobCloudEvent;
import static org.kie.kogito.index.test.TestUtils.getProcessCloudEvent;
import static org.kie.kogito.index.test.TestUtils.getProcessInstance;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractGraphQLRuntimesQueriesIT {

    @Inject
    public GraphQLSchemaManagerImpl manager;

    @Inject
    public ProtobufService protobufService;

    private String processId = "travels";
    String user = "jdoe";
    List<String> groups = Arrays.asList("managers", "users", "IT");

    private KogitoRuntimeClient dataIndexApiClient;

    @BeforeEach
    public void setup() throws Exception {
        protobufService.registerProtoBufferType(getTestProtobufFileContent());
        dataIndexApiClient = mock(KogitoRuntimeClient.class);
        manager.setDataIndexApiExecutor(dataIndexApiClient);
    }

    @Test
    void testProcessInstanceAbort() {
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ ProcessInstanceAbort ( id: \\\"" + processInstanceId + "\\\")}\"}");

        verify(dataIndexApiClient).abortProcessInstance(eq(getProcessInstance(processId, processInstanceId, 1, null, null)));
    }

    @Test
    void testProcessExecuteInstance() {
        String assesmentInstanceId = UUID.randomUUID().toString();
        ProcessInstanceStateDataEvent assesmentEvent = getProcessCloudEvent(processId, assesmentInstanceId, ACTIVE, null, null, null, "currentUser");
        indexProcessCloudEvent(assesmentEvent);
        final String assesmentVarName = "assesmentVar";
        final String assesmentVarValue = "assesmentValue";
        final String infraVarName = "clientVar";
        final String infraVarValue = "clientValue";
        final String excludedKey = "notUsedKey";
        final String excludedValue = "irrelevant";
        indexProcessCloudEvent(buildVariableEvent(assesmentInstanceId, assesmentVarName, assesmentVarValue));
        indexProcessCloudEvent(buildVariableEvent(assesmentInstanceId, excludedKey, excludedValue));
        final String infraProcessId = "infra";
        ProcessDefinitionDataEvent definitionEvent = TestUtils.getProcessDefinitionDataEvent(infraProcessId);
        indexProcessCloudEvent(definitionEvent);
        checkOkResponse("{ \"query\" : \"mutation{ ExecuteAfter ( " + fragment("completedInstanceId", assesmentInstanceId) + "," + fragment("processId", infraProcessId) +
                ",excludeProperties: [\\\"" + excludedKey + "\\\"]" +
                "," + fragment("processVersion", TestUtils.PROCESS_VERSION) + "," + "input: {" + fragment(infraVarName, infraVarValue) + "})}\"}");
        verify(dataIndexApiClient).executeProcessInstance(TestUtils.getProcessDefinition(infraProcessId),
                ExecuteArgs.of(ObjectMapperFactory.get().createObjectNode().put(assesmentVarName, assesmentVarValue)
                        .put(infraVarName, infraVarValue)));
    }

    private ProcessInstanceVariableDataEvent buildVariableEvent(String processInstanceId, String key, String name) {
        ProcessInstanceVariableDataEvent variableEvent = new ProcessInstanceVariableDataEvent();
        variableEvent.setKogitoProcessId(processId);
        variableEvent.setKogitoProcessInstanceId(processInstanceId);
        variableEvent.setData(ProcessInstanceVariableEventBody.create().processId(processId).processInstanceId(processInstanceId)
                .variableName(key).variableValue(name).build());
        return variableEvent;
    }

    private String fragment(String name, String value) {
        return name + ": \\\"" + value + "\\\"";
    }

    @Test
    void testProcessInstanceRetry() {
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ ProcessInstanceRetry ( id: \\\"" + processInstanceId + "\\\")}\"}");

        verify(dataIndexApiClient).retryProcessInstance(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)));
    }

    @Test
    void testProcessInstanceSkip() {
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ ProcessInstanceSkip ( id: \\\"" + processInstanceId + "\\\")}\"}");

        verify(dataIndexApiClient).skipProcessInstance(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)));
    }

    @Test
    void testProcessInstanceUpdateVariables() {
        String variablesUpdated = "variablesUpdated";
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ ProcessInstanceUpdateVariables ( id: \\\"" + processInstanceId + "\\\", variables: \\\"" + variablesUpdated + "\\\")}\"}");

        verify(dataIndexApiClient).updateProcessInstanceVariables(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)), eq(variablesUpdated));
    }

    @Test
    void testProcessDefinitionNodes() {
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"query { ProcessInstances (where: { id: {equal: \\\"" + processInstanceId + "\\\"}}) { nodeDefinitions { id }} }\" }");
        verify(dataIndexApiClient).getProcessDefinitionNodes(TestUtils.getProcessDefinition(processInstanceId));
    }

    @Test
    void testProcessInstanceDiagram() {
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"query { ProcessInstances (where: { id: {equal: \\\"" + processInstanceId + "\\\"}}) {diagram} }\" }");

        verify(dataIndexApiClient).getProcessInstanceDiagram(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)));
    }

    @Test
    void testProcessDefinitionSource() {
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"query { ProcessInstances (where: { id: {equal: \\\"" + processInstanceId + "\\\"}}) {source} }\" }");

        verify(dataIndexApiClient).getProcessDefinitionSourceFileContent(TestUtils.getProcessDefinition(processInstanceId));
    }

    @Test
    void testNodeInstanceTrigger() {
        String nodeId = "nodeIdToTrigger";
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ NodeInstanceTrigger ( id: \\\"" + processInstanceId + "\\\", nodeId: \\\"" + nodeId + "\\\")}\"}");

        verify(dataIndexApiClient).triggerNodeInstance(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)), eq(nodeId));
    }

    @Test
    void testNodeInstanceRetrigger() {
        String nodeInstanceId = "nodeInstanceIdToRetrigger";
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ NodeInstanceRetrigger ( id: \\\"" + processInstanceId + "\\\", nodeInstanceId: \\\"" + nodeInstanceId + "\\\")}\"}");

        verify(dataIndexApiClient).retriggerNodeInstance(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)), eq(nodeInstanceId));
    }

    @Test
    void testNodeInstanceCancel() {
        String nodeInstanceId = "nodeInstanceIdToCancel";
        String processInstanceId = UUID.randomUUID().toString();

        ProcessInstanceStateDataEvent startEvent = getProcessCloudEvent(processId, processInstanceId, ACTIVE, null, null, null, "currentUser");

        indexProcessCloudEvent(startEvent);

        checkOkResponse("{ \"query\" : \"mutation{ NodeInstanceCancel ( id: \\\"" + processInstanceId + "\\\", nodeInstanceId: \\\"" + nodeInstanceId + "\\\")}\"}");

        verify(dataIndexApiClient).cancelNodeInstance(
                eq(getProcessInstance(processId, processInstanceId, 1, null, null)), eq(nodeInstanceId));
    }

    @Test
    void testJobCancel() {
        String jobId = UUID.randomUUID().toString();
        String processInstanceId = UUID.randomUUID().toString();

        KogitoJobCloudEvent event = getJobCloudEvent(jobId, processId, processInstanceId, null, null, "EXECUTED");

        indexJobCloudEvent(event);
        checkOkResponse("{ \"query\" : \"mutation{ JobCancel ( id: \\\"" + jobId + "\\\")}\"}");

        verify(dataIndexApiClient).cancelJob(
                eq(getJob(jobId, processId, processInstanceId, null, null, "SCHEDULED")));
    }

    @Test
    void testJobReschedule() {
        String jobId = UUID.randomUUID().toString();
        String processInstanceId = UUID.randomUUID().toString();
        String data = "jobNewData";

        KogitoJobCloudEvent event = getJobCloudEvent(jobId, processId, processInstanceId, null, null, "EXECUTED");

        indexJobCloudEvent(event);
        checkOkResponse("{ \"query\" : \"mutation{ JobReschedule ( id: \\\"" + jobId + "\\\", data: \\\"" + data + "\\\")}\"}");

        verify(dataIndexApiClient).rescheduleJob(
                eq(getJob(jobId, processId, processInstanceId, null, null, "SCHEDULED")),
                eq(data));
    }

    private void checkOkResponse(String body) {
        given().contentType(ContentType.JSON)
                .body(body)
                .when().post("/graphql")
                .then().statusCode(200);
    }

    protected abstract String getTestProtobufFileContent() throws Exception;


    protected abstract void indexProcessCloudEvent(ProcessDefinitionDataEvent definitionEvent);

    protected abstract void indexProcessCloudEvent(ProcessInstanceDataEvent<?> variableEvent);

    protected abstract void indexJobCloudEvent(KogitoJobCloudEvent event);

}

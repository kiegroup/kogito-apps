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
package org.kie.kogito.index.quarkus.service.api;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.index.api.ExecuteArgs;
import org.kie.kogito.index.api.KogitoRuntimeClient;
import org.kie.kogito.index.model.*;
import org.kie.kogito.index.service.DataIndexServiceException;
import org.kie.kogito.index.service.KogitoRuntimeCommonClient;
import org.kie.kogito.index.service.auth.DataIndexAuthTokenReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import static java.lang.String.format;

@ApplicationScoped
class KogitoRuntimeClientImpl extends KogitoRuntimeCommonClient implements KogitoRuntimeClient {

    public static final String ABORT_PROCESS_INSTANCE_PATH = "/management/processes/%s/instances/%s";
    public static final String RETRY_PROCESS_INSTANCE_PATH = "/management/processes/%s/instances/%s/retrigger";
    public static final String SKIP_PROCESS_INSTANCE_PATH = "/management/processes/%s/instances/%s/skip";
    public static final String GET_PROCESS_INSTANCE_SOURCE_PATH = "/management/processes/%s/source";
    public static final String GET_PROCESS_INSTANCE_NODE_DEFINITIONS_PATH = "/management/processes/%s/nodes";
    public static final String GET_PROCESS_INSTANCE_TIMERS_PATH = "/management/processes/%s/instances/%s/timers";
    public static final String UPDATE_VARIABLES_PROCESS_INSTANCE_PATH = "/%s/%s";
    public static final String TRIGGER_NODE_INSTANCE_PATH = "/management/processes/%s/instances/%s/nodes/%s";
    public static final String RETRIGGER_NODE_INSTANCE_PATH = "/management/processes/%s/instances/%s/nodeInstances/%s";
    public static final String CANCEL_NODE_INSTANCE_PATH = "/management/processes/%s/instances/%s/nodeInstances/%s";
    public static final String UPDATE_NODE_INSTANCE_SLA_PATH = "/management/processes/%s/instances/%s/nodeInstances/%s/sla";
    public static final String UPDATE_PROCESS_INSTANCE_SLA_PATH = "/management/processes/%s/instances/%s/sla";

    public static final String GET_PROCESS_INSTANCE_DIAGRAM_PATH = "/svg/processes/%s/instances/%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(KogitoRuntimeClientImpl.class);

    KogitoRuntimeClientImpl() {
        this(null, null, null);
    }

    @Inject
    public KogitoRuntimeClientImpl(@ConfigProperty(name = "kogito.dataindex.gateway.url") Optional<String> gatewayTargetUrl, DataIndexAuthTokenReader authTokenReader, Vertx vertx) {
        super(gatewayTargetUrl, authTokenReader, vertx);
    }

    @Override
    public CompletableFuture<JsonNode> executeProcessInstance(ProcessDefinition definition, ExecuteArgs args) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();

        URI uri = URI.create(definition.getEndpoint());
        HttpRequest<Buffer> request = getWebClient(uri).post(uri.getPath());
        if (args.businessKey() != null) {
            request.addQueryParam("businessKey", args.businessKey());
        }
        request.sendJson(args.input(), res -> asyncHttpResponseTreatment(res, future, result -> result.bodyAsJson(JsonNode.class),
                "START ProcessInstance of type " + definition.getId()));
        return future;
    }

    @Override
    public CompletableFuture<String> abortProcessInstance(ProcessInstance processInstance) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI = format(ABORT_PROCESS_INSTANCE_PATH, info.idVersion(), processInstance.getId());
        return sendDeleteClientRequest(info, requestURI, "ABORT ProcessInstance with id: " + processInstance.getId());
    }

    @Override
    public CompletableFuture<String> retryProcessInstance(ProcessInstance processInstance) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI = format(RETRY_PROCESS_INSTANCE_PATH, info.idVersion(), processInstance.getId());
        return sendPostClientRequest(info, requestURI, "RETRY ProcessInstance with id: " + processInstance.getId());
    }

    @Override
    public CompletableFuture<String> skipProcessInstance(ProcessInstance processInstance) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI = format(SKIP_PROCESS_INSTANCE_PATH,
                info.idVersion(), processInstance.getId());
        return sendPostClientRequest(info, requestURI, "SKIP ProcessInstance with id: " + processInstance.getId());
    }

    @Override
    public CompletableFuture<String> updateProcessInstanceVariables(ProcessInstance processInstance, String variables) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        return sendJSONPutClientRequest(info, info.idVersion(), "UPDATE VARIABLES of ProcessInstance with id: " + processInstance.getId(), variables);
    }

    @Override
    public CompletableFuture<String> rescheduleNodeInstanceSla(ProcessInstance processInstance, String nodeInstanceId, ZonedDateTime expirationTime) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI =
                format(UPDATE_NODE_INSTANCE_SLA_PATH, info.idVersion(),
                        processInstance.getId(), nodeInstanceId);
        return sendPatchClientRequest(info, requestURI, "Update SLA of NodesInstance with id: " + nodeInstanceId, new JsonObject(expirationTime.toString()));
    }

    @Override
    public CompletableFuture<String> rescheduleProcessInstanceSla(ProcessInstance processInstance, ZonedDateTime expirationTime) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI = format(UPDATE_PROCESS_INSTANCE_SLA_PATH,
                info.idVersion(), processInstance.getId());
        return sendPatchClientRequest(info, requestURI, "Update SLA of ProcessInstance with id: " + processInstance.getId(), new JsonObject(expirationTime.toString()));
    }

    @Override
    public CompletableFuture<String> getProcessInstanceDiagram(ProcessInstance processInstance) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI = format(GET_PROCESS_INSTANCE_DIAGRAM_PATH, info.idVersion(), processInstance.getId());
        return sendGetClientRequest(info, requestURI, "Get Process Instance diagram with id: " + processInstance.getId(), null);
    }

    @Override
    public CompletableFuture<List<Timer>> getProcessInstanceTimers(ProcessInstance processInstance) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI = format(GET_PROCESS_INSTANCE_TIMERS_PATH, info.idVersion(), processInstance.getId());
        return sendGetClientRequest(info, requestURI, "Get Process Instance Timers: " + processInstance.getId(), List.class);
    }

    @Override
    public CompletableFuture<String> getProcessDefinitionSourceFileContent(ProcessDefinition processDef) {
        URIInfo info = URIInfo.buildURIInfo(processDef);
        String requestURI = format(GET_PROCESS_INSTANCE_SOURCE_PATH, info.idVersion());
        return sendGetClientRequest(info, requestURI, "Get Process Instance source file with processId: " + processDef.getKogitoProcessId(), null);
    }

    @Override
    public CompletableFuture<List<Node>> getProcessDefinitionNodes(ProcessDefinition processDef) {
        URIInfo info = URIInfo.buildURIInfo(processDef);
        String requestURI = format(GET_PROCESS_INSTANCE_NODE_DEFINITIONS_PATH, info.idVersion());
        return sendGetClientRequest(info, requestURI, "Get Process available nodes with id: " + processDef.getKogitoProcessId(), List.class);
    }

    @Override
    public CompletableFuture<String> triggerNodeInstance(ProcessInstance processInstance, String nodeDefinitionId) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI =
                format(TRIGGER_NODE_INSTANCE_PATH, info.idVersion(),
                        processInstance.getId(), nodeDefinitionId);
        return sendPostClientRequest(info, requestURI,
                "Trigger Node " + nodeDefinitionId + FROM_PROCESS_INSTANCE_WITH_ID + processInstance.getId());
    }

    @Override
    public CompletableFuture<String> retriggerNodeInstance(ProcessInstance processInstance, String nodeInstanceId) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI =
                format(RETRIGGER_NODE_INSTANCE_PATH, info.idVersion(),
                        processInstance.getId(), nodeInstanceId);
        return sendPostClientRequest(info, requestURI,
                "Retrigger NodeInstance " + nodeInstanceId + FROM_PROCESS_INSTANCE_WITH_ID + processInstance.getId());
    }

    @Override
    public CompletableFuture<String> cancelNodeInstance(ProcessInstance processInstance, String nodeInstanceId) {
        URIInfo info = URIInfo.buildURIInfo(processInstance);
        String requestURI =
                format(CANCEL_NODE_INSTANCE_PATH, info.idVersion(),
                        processInstance.getId(), nodeInstanceId);
        return sendDeleteClientRequest(info, requestURI,
                "Cancel NodeInstance " + nodeInstanceId + FROM_PROCESS_INSTANCE_WITH_ID + processInstance.getId());
    }

    protected CompletableFuture sendPostWithBodyClientRequest(WebClient webClient, String requestURI, String logMessage, String body, String contentType) {
        CompletableFuture future = new CompletableFuture<>();

        HttpRequest<Buffer> request = webClient.post(requestURI)
                .putHeader("Authorization", getAuthHeader())
                .putHeader("Content-Type", contentType);
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            LOGGER.trace("Sending Json Body: {} POST  to URI {}", body, requestURI);
            request.sendJson(new JsonObject(body), res -> asyncHttpResponseTreatment(res, future, logMessage));
        } else {
            LOGGER.trace("Sending Buffer(Body): {} POST to URI {}", body, requestURI);
            request.sendBuffer(Buffer.buffer(body), res -> asyncHttpResponseTreatment(res, future, logMessage));
        }
        return future;
    }

    protected CompletableFuture sendPostClientRequest(URIInfo info, String requestURI, String logMessage) {
        CompletableFuture future = new CompletableFuture<>();

        getWebClient(info.truncatedURI()).post(append(info, requestURI))
                .putHeader("Authorization", getAuthHeader())
                .send(res -> asyncHttpResponseTreatment(res, future, logMessage));
        LOGGER.debug("Sending post to URI {}", requestURI);
        return future;
    }

    protected CompletableFuture sendJSONPutClientRequest(URIInfo info, String requestURI, String logMessage, String jsonString) {
        return sendPutClientRequest(getWebClient(info.truncatedURI()), append(info, requestURI), logMessage, jsonString, MediaType.APPLICATION_JSON);
    }

    protected CompletableFuture sendPutClientRequest(WebClient webClient, String requestURI, String logMessage, String body, String contentType) {
        CompletableFuture future = new CompletableFuture<>();
        HttpRequest<Buffer> request = webClient.put(requestURI)
                .putHeader("Authorization", getAuthHeader())
                .putHeader("Content-Type", contentType);
        if (MediaType.APPLICATION_JSON.equals(contentType)) {
            LOGGER.info("Sending Json Body: {} PUT  to URI {}", body, requestURI);
            request.sendJson(new JsonObject(body), res -> asyncHttpResponseTreatment(res, future, logMessage));
        } else {
            LOGGER.info("Sending Buffer(Body): {} PUT to URI {}", body, requestURI);
            request.sendBuffer(Buffer.buffer(body), res -> asyncHttpResponseTreatment(res, future, logMessage));
        }
        return future;
    }

    protected CompletableFuture sendGetClientRequest(URIInfo info, String requestURI, String logMessage, Class type) {
        CompletableFuture future = new CompletableFuture<>();

        getWebClient(info.truncatedURI()).get(append(info, requestURI))
                .putHeader("Authorization", getAuthHeader())
                .send(res -> send(logMessage, type, future, res));
        LOGGER.debug("Sending GET to URI {}", requestURI);
        return future;
    }

    protected void send(String logMessage, Class type, CompletableFuture future, AsyncResult<HttpResponse<Buffer>> res) {
        if (res.succeeded() && res.result().statusCode() == 200) {
            if (type != null) {
                future.complete(res.result().bodyAsJson(type));
            } else {
                future.complete(res.result().bodyAsString());
            }
        } else if (res.succeeded() && res.result().statusCode() == 404) {
            future.complete(null);
        } else {
            future.completeExceptionally(new DataIndexServiceException(getErrorMessage(logMessage, res.result()), res.cause()));
        }
    }
}

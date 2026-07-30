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
package org.kie.kogito.index;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;

public abstract class AbstractProcessDataIndexIT {

    protected static Duration TIMEOUT = Duration.ofSeconds(30);

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        JsonConfig jsonConfig = JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE);

        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig);
    }

    RequestSpecification spec;

    public abstract String getDataIndexURL();

    public boolean validateDomainData() {
        return true;
    }

    public boolean validateGetProcessInstanceSource() {
        return false;
    }

    public RequestSpecification dataIndexSpec() {
        if (spec == null) {
            spec = new RequestSpecBuilder().setBaseUri(getDataIndexURL()).build();
        }
        return spec;
    }

    protected ValidatableResponse addCheck(ValidatableResponse res) {
        return res.body("data.ProcessDefinitions[0].source", notNullValue());
    }

    @Test
    public void testProcessInstanceEvents() throws IOException {
        String pId = createTestProcessInstance();

        if (validateDomainData()) {

            await()
                    .atMost(TIMEOUT)
                    .untilAsserted(() -> given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                            .body("{ \"query\" : \"{helloworld { id," +
                                    "metadata { processInstances { id, state }} } }\" }")
                            .when().post("/graphql")
                            .then().statusCode(200)
                            .body("data.helloworld.size()", is(1))
                            .body("data.helloworld[0].id", is(pId))
                            .body("data.helloworld[0].workflowdata.result", is("Hello World!"))
                            .body("data.helloworld[0].metadata.processInstances", is(notNullValue()))
                            .body("data.helloworld[0].metadata.processInstances.size()", is(1))
                            .body("data.helloworld[0].metadata.processInstances[0].id", is(pId))
                            .body("data.helloworld[0].metadata.processInstances[0].state", is("COMPLETED")));
        }

        await()
                .atMost(TIMEOUT)
                .untilAsserted(() -> given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                        .body("{ \"query\" : \"{ProcessInstances{ id, processId, state, createdBy} }\" }")
                        .when().post("/graphql")
                        .then().statusCode(200)
                        .body("data.ProcessInstances.size()", is(1))
                        .body("data.ProcessInstances[0].id", is(pId))
                        .body("data.ProcessInstances[0].processId", is("helloworld"))
                        .body("data.ProcessInstances[0].state", is("COMPLETED"))
                        .body("data.ProcessInstances[0].createdBy", nullValue()));

        await()
                .atMost(TIMEOUT)
                .untilAsserted(() -> addCheck(given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                        .body("{ \"query\" : \"{ProcessDefinitions{ id, version, name, source} }\" }")
                        .when().post("/graphql")
                        .then().statusCode(200)
                        .body("data.ProcessDefinitions.size()", is(1))
                        .body("data.ProcessDefinitions[0].id", is("approvals"))
                        .body("data.ProcessDefinitions[0].version", is("1.0"))
                        .body("data.ProcessInstances[0].source", is(getTestFileContentByFilename("helloworld.sw.json")))
                        .body("data.ProcessDefinitions[0].name", is("approvals"))));

        testProcessGatewayAPI();
    }

    public void testProcessGatewayAPI() throws IOException {
        String pId = given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/nonStartEvent")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");

        await()
                .atMost(TIMEOUT)
                .untilAsserted(() -> getProcessInstanceById(pId, "ACTIVE"));

        await()
                .atMost(TIMEOUT).untilAsserted(() -> given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                        .body("{ \"query\" : \"mutation {ProcessInstanceAbort( id: \\\"" + pId + "\\\")}\"}")
                        .when().post("/graphql")
                        .then()
                        .statusCode(200)
                        .body("errors", nullValue()));
        await()
                .atMost(TIMEOUT)
                .untilAsserted(() -> getProcessInstanceById(pId, "ABORTED"));

        given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                .body("{ \"query\" : \"mutation{ ProcessInstanceRetry( id: \\\"" + pId + "\\\")}\"}")
                .when().post("/graphql")
                .then()
                .statusCode(200)
                .body("data.ProcessInstanceRetry", nullValue());

        given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                .body("{ \"query\" : \"mutation{ ProcessInstanceSkip( id: \\\"" + pId + "\\\")}\"}")
                .when().post("/graphql")
                .then()
                .statusCode(200)
                .body("errors[0].message", containsString("FAILED: SKIP"));

        given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                .body("{ \"query\" : \"mutation{ UndefinedMutation( id: \\\"" + pId + "\\\")}\"}")
                .when().post("/graphql")
                .then()
                .statusCode(200)
                .body("errors[0].message", containsString("Field 'UndefinedMutation' in type 'Mutation' is undefined"));

    }

    protected String createTestProcessInstance() {
        return given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/helloworld")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");
    }

    protected ValidatableResponse getProcessInstanceById(String processInstanceId, String state) {
        return given().spec(dataIndexSpec()).contentType(ContentType.JSON)
                .body("{ \"query\" : \"{ProcessInstances(where: {  id: {  equal : \\\"" + processInstanceId + "\\\"}}){ id, processId, state, executionSummary } }\" }")
                .when().post("/graphql")
                .then().statusCode(200)
                .body("data.ProcessInstances.size()", is(1))
                .body("data.ProcessInstances[0].id", is(processInstanceId))
                .body("data.ProcessInstances[0].processId", is("nonStartEvent"))
                .body("data.ProcessInstances[0].state", is(state))
                .body("data.ProcessInstances[0].executionSummary", is(notNullValue()))
                .body("data.ProcessInstances[0].executionSummary.size()", greaterThan(0));
    }

    public static String readFileContent(String file) throws URISyntaxException, IOException {
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource(file).toURI());
        return Files.readString(path);
    }

    public String getTestFileContentByFilename(String fileName) throws Exception {
        return readFileContent(fileName);
    }
}

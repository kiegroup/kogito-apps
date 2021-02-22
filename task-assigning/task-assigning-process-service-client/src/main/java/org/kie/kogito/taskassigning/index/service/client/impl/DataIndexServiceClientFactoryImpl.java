/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.taskassigning.index.service.client.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.kogito.taskassigning.auth.AuthenticationCredentials;
import org.kie.kogito.taskassigning.index.service.client.DataIndexServiceClient;
import org.kie.kogito.taskassigning.index.service.client.DataIndexServiceClientConfig;
import org.kie.kogito.taskassigning.index.service.client.DataIndexServiceClientFactory;
import org.kie.kogito.taskassigning.index.service.client.graphql.GraphQLServiceClientConfig;
import org.kie.kogito.taskassigning.index.service.client.graphql.impl.mp.graphql.GraphQLServiceClientFactoryMP;

@ApplicationScoped
public class DataIndexServiceClientFactoryImpl implements DataIndexServiceClientFactory {

    private GraphQLServiceClientFactoryMP queryServiceFactory;

    public DataIndexServiceClientFactoryImpl() {
        //CDI proxying
    }

    @Inject
    public DataIndexServiceClientFactoryImpl(GraphQLServiceClientFactoryMP queryServiceFactory) {
        this.queryServiceFactory = queryServiceFactory;
    }

    @Override
    public DataIndexServiceClient newClient(DataIndexServiceClientConfig config, AuthenticationCredentials credentials) {
        return new DataIndexServiceClientImpl(queryServiceFactory.newClient(GraphQLServiceClientConfig.newBuilder()
                                                                                    .serviceUrl(config.getServiceUrl().toString())
                                                                                    .connectTimeoutMillis(config.getConnectTimeoutMillis())
                                                                                    .readTimeoutMillis(config.getReadTimeoutMillis())
                                                                                    .build(), credentials));
    }
}
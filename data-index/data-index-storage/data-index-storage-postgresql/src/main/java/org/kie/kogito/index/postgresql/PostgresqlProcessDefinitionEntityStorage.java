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
package org.kie.kogito.index.postgresql;

import org.kie.kogito.index.jpa.mapper.ProcessDefinitionEntityMapper;
import org.kie.kogito.index.jpa.model.ProcessDefinitionEntityRepository;
import org.kie.kogito.index.jpa.storage.ProcessDefinitionEntityStorage;
import org.kie.kogito.index.model.ProcessDefinition;
import org.kie.kogito.persistence.api.query.Query;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PostgresqlProcessDefinitionEntityStorage extends ProcessDefinitionEntityStorage {

    @Inject
    public PostgresqlProcessDefinitionEntityStorage(ProcessDefinitionEntityRepository repository, ProcessDefinitionEntityMapper mapper) {
        super(repository, mapper);
    }

    @Override
    public Query<ProcessDefinition> query() {
        return new PostgresqlJsonJPAQuery<>(repository, mapToModel, entityClass);
    }
}

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

package org.kie.kogito.taskassigning.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.kogito.taskassigning.index.service.client.graphql.UserTaskInstance;
import org.kie.kogito.taskassigning.user.service.api.Group;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TestUtil {

    private TestUtil() {
    }

    public static ZonedDateTime parseZonedDateTime(String value) {
        return ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static org.kie.kogito.taskassigning.user.service.api.User mockExternalUser(String id) {
        return mockExternalUser(id, new ArrayList<>(), new HashMap<>());
    }

    public static org.kie.kogito.taskassigning.user.service.api.User mockExternalUser(String id, List<String> groups, Map<String, Object> attributes) {
        org.kie.kogito.taskassigning.user.service.api.User externalUser = mock(org.kie.kogito.taskassigning.user.service.api.User.class);
        doReturn(id).when(externalUser).getId();
        Set<Group> externalGroups = groups.stream().map(TestUtil::mockExternalGroup).collect(Collectors.toSet());
        doReturn(externalGroups).when(externalUser).getGroups();
        doReturn(attributes).when(externalUser).getAttributes();
        return externalUser;
    }

    public static org.kie.kogito.taskassigning.user.service.api.Group mockExternalGroup(String id) {
        org.kie.kogito.taskassigning.user.service.api.Group externalGroup = mock(org.kie.kogito.taskassigning.user.service.api.Group.class);
        doReturn(id).when(externalGroup).getId();
        return externalGroup;
    }

    public static UserTaskInstance mockUserTaskInstance(String taskId, ZonedDateTime started, String state) {
        UserTaskInstance userTaskInstance = new UserTaskInstance();
        userTaskInstance.setId(taskId);
        userTaskInstance.setStarted(started);
        userTaskInstance.setState(state);
        return userTaskInstance;
    }

    public static UserTaskInstance mockUserTaskInstance(String taskId, String state) {
        UserTaskInstance userTaskInstance = new UserTaskInstance();
        userTaskInstance.setId(taskId);
        userTaskInstance.setState(state);
        return userTaskInstance;
    }

    public static UserTaskInstance mockUserTaskInstance(String taskId, String state, String actualOwner) {
        UserTaskInstance userTaskInstance = new UserTaskInstance();
        userTaskInstance.setId(taskId);
        userTaskInstance.setState(state);
        userTaskInstance.setActualOwner(actualOwner);
        return userTaskInstance;
    }
}
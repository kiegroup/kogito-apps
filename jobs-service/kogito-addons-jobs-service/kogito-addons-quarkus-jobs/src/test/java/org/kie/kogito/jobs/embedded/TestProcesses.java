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
package org.kie.kogito.jobs.embedded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstances;
import org.kie.kogito.process.Processes;
import org.mockito.Mockito;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestProcesses implements Processes {

    private Process process = Mockito.mock(Process.class);
    private ProcessInstances instances = Mockito.mock(ProcessInstances.class);
    private Collection<Process<? extends Model>> processes;

    @PostConstruct
    void setup() {
        process = Mockito.mock(Process.class);
        instances = Mockito.mock(ProcessInstances.class);
        processes = new ArrayList<>();
        processes.add(process);
        Mockito.when(process.instances()).thenReturn(instances);
    }

    @Override
    public Iterator<Process<? extends Model>> iterator() {
        return processes.iterator();
    }
}

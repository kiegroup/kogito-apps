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
package org.kie.kogito.jobs.service.stream;

import java.util.List;
import java.util.stream.Collectors;

import org.kie.kogito.jobs.service.model.JobDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class JobStreamsEventPublisher implements JobEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStreamsEventPublisher.class);

    @Inject
    Instance<JobStreams> jobStreamsInstance;

    private List<JobStreams> enabledStreams;

    @PostConstruct
    void init() {
        this.enabledStreams = jobStreamsInstance.stream()
                .filter(stream -> {
                    LOGGER.info("Job stream: {}, enabled: {}", stream, stream.isEnabled());
                    return stream.isEnabled();
                })
                .collect(Collectors.toList());
    }

    @Override
    public JobDetails publishJobStatusChange(JobDetails job) {
        LOGGER.debug("publishJobStatusChange to streams, job: {}", job);
        enabledStreams.forEach(jobStream -> jobStream.jobStatusChange(job));
        return job;
    }
}

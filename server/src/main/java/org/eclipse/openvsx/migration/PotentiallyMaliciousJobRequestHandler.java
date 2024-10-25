/********************************************************************************
 * Copyright (c) 2024 STMicroelectronics and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Component
public class PotentiallyMaliciousJobRequestHandler implements JobRequestHandler<MigrationJobRequest> {

    protected final Logger logger = new JobRunrDashboardLogger(LoggerFactory.getLogger(PotentiallyMaliciousJobRequestHandler.class));

    private final MigrationService migrations;
    private final CheckPotentiallyMaliciousExtensionVersionsService service;

    public PotentiallyMaliciousJobRequestHandler(MigrationService migrations, CheckPotentiallyMaliciousExtensionVersionsService service) {
        this.migrations = migrations;
        this.service = service;
    }

    @Override
    @Job(name = "Check published extensions for potentially malicious vsix file", retries = 3)
    public void run(MigrationJobRequest jobRequest) throws Exception {
        var download = migrations.getResource(jobRequest);
        var extVersion = download.getExtension();
        logger.info("Checking extension version for potentially malicious vsix file: {}", NamingUtil.toLogFormat(extVersion));

        try(var extensionFile = migrations.getExtensionFile(download)) {
            if(Files.size(extensionFile.getPath()) == 0) {
                logger.info("Extension file is empty, skipping: {}", download.getName());
                return;
            }

            logger.info("Checking vsix file for potentially malicious metadata: {}", download.getName());
            service.checkPotentiallyMaliciousExtensionVersion(extVersion, extensionFile);
        }
    }

}

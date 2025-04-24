/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.extension_control;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExtensionControlJobRequestHandler implements JobRequestHandler<HandlerJobRequest<?>>  {

    protected final Logger logger = LoggerFactory.getLogger(ExtensionControlJobRequestHandler.class);

    private final AdminService admin;
    private final ExtensionControlService service;
    private final RepositoryService repositories;

    public ExtensionControlJobRequestHandler(AdminService admin, ExtensionControlService service, RepositoryService repositories) {
        this.admin = admin;
        this.service = service;
        this.repositories = repositories;
    }

    @Override
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        logger.info("Run extension control job");
        var json = service.getExtensionControlJson();
        logger.info("Got extension control JSON");
        processMaliciousExtensions(json);
        processDeprecatedExtensions(json);
    }

    private void processMaliciousExtensions(JsonNode json) {
        logger.info("Process malicious extensions");
        var node = json.get("malicious");
        if(!node.isArray()) {
            logger.error("field 'malicious' is not an array");
            return;
        }

        var adminUser = service.createExtensionControlUser();
        for(var item : node) {
            if(logger.isInfoEnabled()) {
                logger.info("malicious: {}", item.asText());
            }
            var extensionId = NamingUtil.fromExtensionId(item.asText());
            if(extensionId != null && repositories.hasExtension(extensionId.namespace(), extensionId.extension())) {
                logger.info("delete malicious extension");
                admin.deleteExtensionAndDependencies(extensionId.namespace(), extensionId.extension(), adminUser);
            }
        }
    }

    private void processDeprecatedExtensions(JsonNode json) {
        logger.info("Process deprecated extensions");
        var node = json.get("deprecated");
        if(!node.isObject()) {
            logger.error("field 'deprecated' is not an object");
            return;
        }

        node.fields().forEachRemaining(field -> {
            logger.info("deprecated: {}", field.getKey());
            var extensionId = NamingUtil.fromExtensionId(field.getKey());
            if(extensionId == null) {
                return;
            }

            var value = field.getValue();
            if(value.isBoolean()) {
                service.updateExtension(extensionId, value.asBoolean(), null, true);
            } else if(value.isObject()) {
                var replacement = value.get("extension");
                var replacementId = replacement != null && replacement.isObject()
                        ? NamingUtil.fromExtensionId(replacement.get("id").asText())
                        : null;

                var disallowInstall = value.has("disallowInstall") && value.get("disallowInstall").asBoolean(false);
                service.updateExtension(extensionId, true, replacementId, !disallowInstall);
            } else {
                logger.error("field '{}' is not an object or a boolean", extensionId);
            }
        });
    }
}

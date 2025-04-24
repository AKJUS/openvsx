/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.openvsx.util.NamingUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
public class MirrorExtensionHandlerInterceptor implements HandlerInterceptor {

    private final DataMirrorService dataMirror;

    public MirrorExtensionHandlerInterceptor(DataMirrorService dataMirror) {
        this.dataMirror = dataMirror;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var params = request.getRequestURI().equals("/vscode/item")
                ? extractQueryParams(request)
                : extractPathParams(request);
        var namespaceName = (String) params.get("namespaceName");
        var extensionName = (String) params.get("extensionName");
        if (!dataMirror.match(namespaceName, extensionName)) {
            response.reset();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        return true;
    }

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        var itemName = NamingUtil.fromExtensionId(request.getParameter("itemName"));
        return itemName != null
                ? Map.of("namespaceName", itemName.namespace(), "extensionName", itemName.extension())
                : Collections.emptyMap();
    }

    private Map<String, String> extractPathParams(HttpServletRequest request) {
        return new TreeMap<>((Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
    }
}

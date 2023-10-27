/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import org.eclipse.openvsx.entities.Extension;

public class BuiltInExtensionUtil {
    private static final String BUILT_IN_EXTENSION_NAMESPACE = "vscode";

    private BuiltInExtensionUtil() {}

    public static String getBuiltInNamespace() {
        return BUILT_IN_EXTENSION_NAMESPACE;
    }

    public static boolean isBuiltIn(String namespace) {
        return BUILT_IN_EXTENSION_NAMESPACE.equals(namespace);
    }

    public static boolean isBuiltIn(Extension extension) {
        return BUILT_IN_EXTENSION_NAMESPACE.equals(extension.getNamespace().getName());
    }
}

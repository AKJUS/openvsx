/********************************************************************************
 * Copyright (c) 2022 Anibal Solon and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { getPAT } from './login';
import { Registry } from './registry';
import { readManifest, addEnvOptions } from './util';
import { VerifyPatOptions } from './verify-pat-options';

/**
 * Validates that a Personal Access Token can publish to a namespace.
 */
export async function verifyPat(options: VerifyPatOptions): Promise<void> {
    addEnvOptions(options);
    if (!options.namespace) {
        let error;
        try {
            options.namespace = (await readManifest()).publisher;
        } catch (e) {
            error = e;
        }

        if (!options.namespace) {
            throw new Error(
                `Unable to read the namespace's name. Please supply it as an argument or run ovsx from the extension folder.` +
                (error ? `\n\n${error}` : '')
            );
        }
    }

    options.pat = await getPAT(options.namespace, options, false);
    await doVerifyPat(options);
}

export async function doVerifyPat(options: VerifyPatOptions) {
    const registry = new Registry(options);
    const namespace = options.namespace as string;
    const pat = options.pat as string;
    const result = await registry.verifyPat(namespace, pat);
    if (result.error) {
        throw new Error(result.error);
    }
    console.log(`\ud83d\ude80  PAT valid to publish at ${namespace}`);
}

/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.openvsx.entities.SignatureKeyPair;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

@Component
public class GenerateKeyPairJobService {

    private final EntityManager entityManager;
    private final RepositoryService repositories;

    public GenerateKeyPairJobService(EntityManager entityManager, RepositoryService repositories) {
        this.entityManager = entityManager;
        this.repositories = repositories;
    }

    @Transactional
    public void updateKeyPair(SignatureKeyPair keyPair) {
        repositories.deactivateKeyPairs();
        entityManager.persist(keyPair);
    }

    public SignatureKeyPair generateKeyPair() throws IOException {
        var generator = new Ed25519KeyPairGenerator();
        generator.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        var pair = generator.generateKeyPair();

        var keyPair = new SignatureKeyPair();
        keyPair.setPublicId(UUID.randomUUID().toString());
        keyPair.setPrivateKey(((Ed25519PrivateKeyParameters) pair.getPrivate()).getEncoded());
        keyPair.setPublicKeyText(getPublicKeyText(pair));
        keyPair.setCreated(TimeUtil.getCurrentUTC());
        keyPair.setActive(true);
        return keyPair;
    }
    
    private String getPublicKeyText(AsymmetricCipherKeyPair pair) throws IOException {
        var publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pair.getPublic());
        var pemObject = new PemObject("PUBLIC KEY", publicKeyInfo.getEncoded());

        try (
                var output = new ByteArrayOutputStream();
                var writer = new PemWriter(new OutputStreamWriter(output))
        ) {
            writer.writeObject(pemObject);
            writer.flush();
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    @Transactional
    public void deleteSignaturesAndKeyPairs() {
        repositories.deleteAllKeyPairs();
        repositories.deleteDownloadSigFiles();
    }
}
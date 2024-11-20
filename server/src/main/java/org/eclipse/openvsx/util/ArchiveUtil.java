/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ArchiveUtil {

    // Limit the size of fetched zip entries to 32 MB
    private static final long MAX_ENTRY_SIZE = 33_554_432;

    private ArchiveUtil() {}

    public static ZipEntry getEntryIgnoreCase(ZipFile archive, String entryName) {
        return archive.stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(entryName))
                .findAny()
                .orElse(null);
    }

    public static TempFile readEntry(ZipFile archive, String entryName) throws IOException {
        var entry = archive.getEntry(entryName);
        if (entry == null)
            return null;
        return readEntry(archive, entry);
    }

    public static TempFile readEntry(ZipFile archive, ZipEntry entry) throws IOException {
        var fileNameIndex = entry.getName().lastIndexOf('/');
        var fileName = fileNameIndex == -1 ? entry.getName() : entry.getName().substring(fileNameIndex + 1);
        var suffixIndex = fileName.lastIndexOf('.');
        var suffix = suffixIndex == -1 ? null : fileName.substring(suffixIndex);
        var prefix = suffixIndex == -1 ? fileName : fileName.substring(0, suffixIndex);
        var file = new TempFile(prefix, suffix);
        try (var out = Files.newOutputStream(file.getPath())){
            if (entry.getSize() > MAX_ENTRY_SIZE)
                throw new ErrorResultException("The file " + entry.getName() + " exceeds the size limit of 32 MB.");
            archive.getInputStream(entry).transferTo(out);
        }
        return file;
    }
}
/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.web;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static org.eclipse.openvsx.cache.CacheService.CACHE_SITEMAP;

@Component
public class SitemapService {

    @Autowired
    RepositoryService repositories;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Cacheable(CACHE_SITEMAP)
    public String generateSitemap() throws ParserConfigurationException {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        document.setXmlStandalone(true);
        var namespace = "http://www.sitemaps.org/schemas/sitemap/0.9";
        var urlset = document.createElementNS(namespace, "urlset");
        document.appendChild(urlset);

        var baseUrl = getBaseUrl();
        var rows = repositories.fetchSitemapRows();
        for(var row : rows) {
            var entry = document.createElement("url");
            var loc = document.createElement("loc");
            loc.setTextContent(baseUrl + row.namespace() + "/" + row.extension());
            entry.appendChild(loc);

            var lastmod = document.createElement("lastmod");
            lastmod.setTextContent(row.lastUpdated());
            entry.appendChild(lastmod);
            urlset.appendChild(entry);
        }

        try(var writer = new StringWriter()) {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException | IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    private String getBaseUrl() {
        String url;
        if (StringUtils.isEmpty(webuiUrl))
            url = UrlUtil.getBaseUrl();
        else if (URI.create(webuiUrl).isAbsolute())
            url = webuiUrl;
        else
            url = UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), webuiUrl.split("/"));
        if(!url.endsWith("/"))
            url += "/";

        return url + "extension/";
    }
}

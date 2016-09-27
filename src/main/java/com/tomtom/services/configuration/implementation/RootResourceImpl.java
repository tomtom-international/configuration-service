/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import com.google.common.base.Joiner;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.RootResource;
import com.tomtom.services.configuration.dto.VersionDTO;
import com.tomtom.speedtools.maven.MavenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This class implements the REST API that deals with TTBin files.
 */
public class RootResourceImpl implements RootResource {
    private static final Logger LOG = LoggerFactory.getLogger(RootResourceImpl.class);

    @Nonnull
    private static final String HELP_TEXT = "" +
            "You can query the individual nodes of the search tree using:\n\n" +

            "  GET /configuration/tree[/{level1}[/{level2[...]]]\n\n" +

            "The returned response looks like this:\n\n" +

            "  {\"nodes\": [\"{node1}\", \"{node2}\", ...],\n" +
            "  \"parameters\": [{\"key\": \"{key1}\", \"value\": \"{value1}\"]}, ...]," +
            "  \"name\": \"{node-name\"}}\n\n" +

            "The \"nodes\" array is optional and lists the children nodes with search\n" +
            "terms one level below the specified node.\n\n" +

            "The \"parameters\" value is the optional leaf node of this node and lists the\n" +
            "search result (an array of key-value pairs).\n\n" +

            "Note that this is exactly the same format as the configuration file for the service.\n" +
            "The configuration is fetched from a URI specified in the properties file called\n\n" +

            "  application-configuration-data.properties\n\n" +

            "Normally you would use the '?levels=...&search=' ability to find the best matching node, based on\n" +
            "hierarchical search criteria, which falls back to parent nodes for missing entries:\n\n" +

            "  GET /tree?levels={nameLevel1}[/{nameLevel2}[/...]]&search={level1}[/{level2}[/...]]\n\n" +

            "So, the format is almost identical, except the search path is now provided as a query parameter\n" +
            "and the order of the node levels is defined by 'order' (each node level in the configuration has a name).\n" +
            "The returned result is the value of the leaf of the deepest node matching the search criteria:\n\n" +

            "  {\"parameters\" : [{\"key\": \"{key1}\", \"value\": \"{value1}\"}, ...],\n" +
            "  \"matched\" : \"{path-of-deepest-node-that-matched}\"}\n\n" +

            "The \"matched\" value indicates which node provided the parameters. This may be an exact match\n" +
            "of the search path in the query, or any node above it (if path as partially matched).\n\n" +

            "You can get multiple configurations at once by supplying more than 1 query string after 'search='\n" +
            "all separated by a ';', like this:\n\n" +

            "  GET /tree?levels=level1/level2/...&search=query1;query2;...\n\n" +

            "The result of a multi-query request is a JSON array of results, with the elements in the same order\n" +
            "as the sub-queries that were specified.\n\n" +

            "You can use the `If-Modified-Since` HTTP header to have the service return `304 NOT MODIFIED`\n" +
            "if the configuration was no newer than the supplied date. Note that the HTTP header must be of the format:\n\n" +

            "    If-Modified-Since: Sun, 06 Nov 1994 08:49:37 GMT\n\n" +

            "You can also use the `If-None-Match` HTTP header to have the service return `304 NOT MODIFIED`\n" +
            "if the supplied ETag is the same for the returned data. The HTTP header must be of the format:\n\n" +

            "    ETag: \"686897696a7c876b7e\"\n\n" +

            "Return codes:\n" +
            "  Successful GET:                          200 - OK\n" +
            "  Not modified since If-Modified-Since:    304 - NOT MODIFIED\n" +
            "  Node not found for GET:                  404 - NOT FOUND\n" +
            "  No search result found for GET:          404 - NOT FOUND\n";

    /**
     * The search tree, which holds all configurations.
     */
    @Nonnull
    private final Configuration configuration;

    /**
     * Specific ACDS properties from the properties file.
     */
    @Nonnull
    private final ConfigurationServiceProperties configurationServiceProperties;

    /**
     * POM version.
     */
    @Nonnull
    private final MavenProperties mavenProperties;

    @Inject
    public RootResourceImpl(
            @Nonnull final Configuration configuration,
            @Nonnull final ConfigurationServiceProperties configurationServiceProperties,
            @Nonnull final MavenProperties mavenProperties) {

        // Store the injected values.
        this.configuration = configuration;
        this.configurationServiceProperties = configurationServiceProperties;
        this.mavenProperties = mavenProperties;
    }

    @Override
    @Nonnull
    public String getHelpHTML() {
        LOG.info("getHelpHTML: show help page", mavenProperties.getPomVersion());
        return "<html><pre>\n" +
                "CONFIGURATION SERVICE (" + mavenProperties.getPomVersion() + ")\n" +
                "---------------------\n\n" +
                HELP_TEXT + '\n' +
                (configuration.getRoot().getLevels() == null ? "" :
                        "The levels specified in the current configuration are:\n" +
                                Joiner.on("/").join(configuration.getRoot().getLevels())) +
                "\n</pre></html>\n";
    }

    @Override
    public void getVersion(
            @Suspended @Nonnull final AsyncResponse response) {

        // No input validation required. Just return version number.
        final String pomVersion = mavenProperties.getPomVersion();
        final String startupConfigurationURI = configurationServiceProperties.getStartupConfigurationURI();
        LOG.info("getVersion: POM version={}, startupConfigurationURI={}", pomVersion, startupConfigurationURI);

        final VersionDTO result = new VersionDTO(pomVersion, startupConfigurationURI);
        result.validate();

        response.resume(Response.ok(result).build());
    }

    @Override
    public void getStatus(@Suspended @Nonnull final AsyncResponse response) {

        LOG.info("getStatus: get status");
        if (configuration.isStartupConfigurationOK()) {
            response.resume(Response.ok().build());
        } else {
            response.resume(Response.status(Status.METHOD_NOT_ALLOWED).build());
        }
    }
}

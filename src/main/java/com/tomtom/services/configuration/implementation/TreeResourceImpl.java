/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import akka.dispatch.Futures;
import com.google.common.base.Joiner;
import com.tomtom.services.configuration.TreeResource;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.services.configuration.dto.NodeDTO;
import com.tomtom.services.configuration.dto.SearchResultDTO;
import com.tomtom.services.configuration.dto.SearchResultsDTO;
import com.tomtom.speedtools.apivalidation.exceptions.ApiForbiddenException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiNotFoundException;
import com.tomtom.speedtools.apivalidation.exceptions.ApiParameterSyntaxException;
import com.tomtom.speedtools.checksums.SHA1Hash;
import com.tomtom.speedtools.json.Json;
import com.tomtom.speedtools.rest.ResourceProcessor;
import com.tomtom.speedtools.time.UTCTime;
import com.tomtom.speedtools.utils.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class implements the /parameter resource.
 */
public class TreeResourceImpl implements TreeResource {
    private static final Logger LOG = LoggerFactory.getLogger(TreeResourceImpl.class);

    /**
     * We use a salt to not expose the fact we use SHA1 hashes as ETags. The salt may
     * change over time.
     */
    private static final String HASH_SALT = "3141592654";

    /**
     * The search tree, which holds all configurations.
     */
    @Nonnull
    private final Configuration configuration;

    /**
     * The scalable web resources processor.
     */
    @Nonnull
    private final ResourceProcessor processor;

    /**
     * The data/time format used by the HTTP header If-Modified-Since.
     */
    private final SimpleDateFormat formatIfModifiedSince = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    @Inject
    public TreeResourceImpl(
            @Nonnull final Configuration configuration,
            @Nonnull final ResourceProcessor processor) {

        // Store the injected values.
        this.configuration = configuration;
        this.processor = processor;
    }

    @Override
    public void findBestMatch(
            @Nullable final String levels,
            @Nullable final String search,
            @Nullable final String ifModifiedSince,
            @Nullable final String ifNoneMatch,
            @Nonnull final AsyncResponse response) {

        // If order does not exist, return tree instead.
        if ((levels == null) && (search == null)) {
            getNode("", null, null, ifModifiedSince, ifNoneMatch, response);
            return;
        }

        processor.process("findBestMatchingNodes", LOG, response, () -> {
            LOG.info("findBestMatchingNodes: levels='{}', search='{}', if-modified-since={}, if-none-match={}", levels, search, ifModifiedSince, ifNoneMatch);

            // Levels must be present.
            if ((levels == null) || levels.isEmpty()) {
                throw new ApiParameterSyntaxException(TreeResource.PARAM_LEVELS, levels, "Parameter " + TreeResource.PARAM_LEVELS + " must be specified, next to " + TreeResource.PARAM_SEARCH);
            }

            // Search terms must be present.
            if ((search == null) || search.isEmpty()) {
                throw new ApiParameterSyntaxException(TreeResource.PARAM_SEARCH, search, "Parameter " + TreeResource.PARAM_SEARCH + " must be specified, next to " + TreeResource.PARAM_LEVELS);
            }

            // Search terms must be present.
            if (search.indexOf(WRONG_SEPARATOR) != -1) {
                throw new ApiParameterSyntaxException(TreeResource.PARAM_SEARCH, search, "Parameter " + TreeResource.PARAM_SEARCH + " cannot contain '" + WRONG_SEPARATOR + '\'');
            }

            // First try and find the response.
            final SearchResultsDTO foundResults = configuration.findBestMatchingNodes(levels, StringUtils.nullToEmpty(search));
            if (foundResults.isEmpty()) {
                throw new ApiNotFoundException("No result found: searchQuery=" + search);
            }

            // Check if the ETag matches.
            final String eTag = calculateETag(foundResults);
            final boolean eTagMatches = (ifNoneMatch != null) && ifNoneMatch.equalsIgnoreCase(eTag);
            LOG.debug("findBestMatchingNodes: etag='{}', matches={}", eTag, eTagMatches);

            // Get latest modified time from search results.
            DateTime lastModified = null;
            for (final SearchResultDTO foundResult : foundResults) {
                final DateTime modified = foundResult.getNode().searchModifiedUpToRoot();
                if ((lastModified == null) || ((modified != null) && modified.isAfter(lastModified))) {
                    lastModified = modified;
                }
            }

            // And check If-Modified-Since to see if we can avoid returning the body.
            final boolean isModified = isModifiedSince(lastModified, ifModifiedSince);
            if (((ifNoneMatch != null) && eTagMatches) ||
                    ((ifNoneMatch == null) && (ifModifiedSince != null) && !isModified)) {
                response.resume(Response.status(Status.NOT_MODIFIED).
                        tag(eTag).
                        lastModified((lastModified == null) ? null : lastModified.toDate()).
                        build());
                LOG.debug("findBestMatchingNodes: NOT MODIFIED");
                return Futures.successful(null);
            }

            if (foundResults.size() == 1) {
                final SearchResultDTO entity = foundResults.get(0);
                entity.validate();
                LOG.debug("findBestMatchingNodes: OK, entity={}", entity);
                response.resume(Response.status(Status.OK).entity(entity).
                        tag(eTag).
                        lastModified((lastModified == null) ? null : lastModified.toDate()).
                        build());
            } else {
                foundResults.validate();
                LOG.debug("findBestMatchingNodes: OK, found={}", foundResults);
                response.resume(Response.status(Status.OK).entity(foundResults).
                        tag(eTag).
                        lastModified((lastModified == null) ? null : lastModified.toDate()).
                        build());
            }

            return Futures.successful(null);
        });
    }

    @Override
    public void getNode(
            @Nonnull final String fullNodePath,
            @Nullable final String levels,
            @Nullable final String search,
            @Nullable final String ifModifiedSince,
            @Nullable final String ifNoneMatch,
            @Nonnull final AsyncResponse response) {

        processor.process("getNode", LOG, response, () -> {
            LOG.info("getNode: fullNodePath='{}', if-modified-since={}, if-none-match={}", fullNodePath, ifModifiedSince, ifNoneMatch);

            // Make sure parameters 'levels' and 'search' were not specified.
            if ((levels != null) || (search != null)) {
                throw new ApiForbiddenException("Can't specify " + TreeResource.PARAM_LEVELS + " or " + TreeResource.PARAM_SEARCH + " when retrieving specific configuration tree nodes");
            }

            // First, try and get the node from the tree.
            final Node resultNode = configuration.findNode(fullNodePath);
            if (resultNode == null) {
                throw new ApiNotFoundException("Path not found: fullNodePath=" + fullNodePath);
            }

            // Check if the ETag matches.
            final String eTag = calculateETag(resultNode);
            final boolean eTagMatches = (ifNoneMatch != null) && ifNoneMatch.equalsIgnoreCase(eTag);
            LOG.debug("getNode: etag='{}', matches={}", eTag, eTagMatches);

            // Then check If-Modified-Since to see if we can avoid returning the body.
            final DateTime lastModified = resultNode.searchModifiedUpToRoot();
            final boolean isModified = isModifiedSince(lastModified, ifModifiedSince);
            if (((ifNoneMatch != null) && eTagMatches) ||
                    ((ifNoneMatch == null) && (ifModifiedSince != null) && !isModified)) {
                response.resume(Response.status(Status.NOT_MODIFIED).
                        tag(eTag).
                        lastModified((lastModified == null) ? null : lastModified.toDate()).
                        build());
                LOG.debug("getNode: NOT MODIFIED");
                return Futures.successful(null);
            }

            // Get the result: can be a tree (with modified time) or a node.
            final NodeDTO result = new NodeDTO(resultNode);
            result.validate();
            response.resume(Response.status(Status.OK).entity(result).
                    tag(eTag).
                    lastModified((lastModified == null) ? null : lastModified.toDate()).
                    build());
            LOG.debug("getNode: OK, result={}", result);
            return Futures.successful(null);
        });
    }

    /**
     * Create an ETag value for an object.
     *
     * @param object Object to create an ETag for.
     * @return ETag string (quoted).
     */
    @Nonnull
    private static String calculateETag(@Nonnull final Object object) {
        final String json = Json.toJson(object);
        final SHA1Hash hash = SHA1Hash.saltedHash(json, HASH_SALT);
        return '"' + hash.toString() + '"';
    }

    /**
     * Return whether the configuration has changed.
     *
     * @param modified        Last modification time.
     * @param ifModifiedSince HTTP header parameter.
     * @return Return true if configuration changed on or after ifModifiedSince.
     */
    private boolean isModifiedSince(
            @Nullable final DateTime modified,
            @Nullable final String ifModifiedSince) {

        if (modified == null) {
            return true;
        } else {
            if (ifModifiedSince == null) {
                return true;
            } else {
                try {
                    final Date date;

                    // SimpleDateFormat is not thread-safe.
                    synchronized (formatIfModifiedSince) {
                        date = formatIfModifiedSince.parse(ifModifiedSince);
                    }
                    final DateTime ifModfiedSinceDateTime = UTCTime.from(date);
                    final boolean isModified = modified.isAfter(ifModfiedSinceDateTime) || modified.isEqual(ifModfiedSinceDateTime);
                    LOG.debug("isModifiedSince: isModified={}, If-Modified-Since={} <= {}",
                            isModified, ifModfiedSinceDateTime, modified);
                    return isModified;
                } catch (final ParseException ignored) {

                    // Provided header was incorrectly formatted, err on the safe side.
                    LOG.info("isModifiedSince: incorrectly formatted If-Modified-Since={}", ifModifiedSince);
                    return true;
                }
            }
        }
    }
}

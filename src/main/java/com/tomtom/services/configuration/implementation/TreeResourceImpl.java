/**
 * Copyright (C) 2016, TomTom International BV (http://www.tomtom.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.tomtom.services.configuration.implementation;

import akka.dispatch.Futures;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    @Nonnull
    private final SimpleDateFormat formatIfModifiedSince = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    /**
     * This variable is filled by JAX-RS and contains the URI context.
     */
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
            @Nullable final String ifModifiedSince,
            @Nullable final String ifNoneMatch,
            @Nonnull final UriInfo uriInfo,
            @Nonnull final AsyncResponse response) {

        // If no query parameters were specified, use getNode() instead.
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        if ((queryParameters == null) || queryParameters.keySet().isEmpty()) {
            getNode("", ifModifiedSince, ifNoneMatch, uriInfo, response);
            return;
        }

        processor.process("findBestMatch", LOG, response, () -> {
            LOG.info("findBestMatch: query={}, if-modified-since={}, if-none-match={}", queryParameters.keySet(), ifModifiedSince, ifNoneMatch);

            // Get all parameter names (which are the level names).
            final Set<String> levelNames = queryParameters.keySet();

            // Determine how many searches are specified.
            int nrOfSearches = 0;
            for (final String levelName : levelNames) {
                final Iterable<String> terms = Splitter.on(SEPARATOR_QUERY).trimResults().split(queryParameters.getFirst(levelName));
                nrOfSearches = Math.max(nrOfSearches, Iterables.size(terms));
            }

            // Now create a full set of search maps with (level-name: search-term).
            final List<Map<String, String>> levelSearchTermsList = new ArrayList<>();
            for (int i = 0; i < nrOfSearches; ++i) {
                final Map<String, String> levelSearchTerms = new HashMap<>();
                for (final String levelName : levelNames) {
                    final String searchTerm;
                    final List<String> terms = Lists.newArrayList(Splitter.on(SEPARATOR_QUERY).trimResults().split(queryParameters.getFirst(levelName)));
                    if (terms.isEmpty()) {

                        // If no terms are supplied for this level, provide an empty search term.
                        searchTerm = "";
                    } else {

                        if (terms.size() > i) {

                            // If a search terms is available at this level, use it.
                            searchTerm = terms.get(i);
                        } else {

                            // If not, re-use the last value.
                            searchTerm = terms.get(terms.size() - 1);
                        }
                    }

                    // Update last used search term and add to map of (level-name, search-term).
                    levelSearchTerms.put(levelName, searchTerm);
                    if (searchTerm.indexOf(SEPARATOR_WRONG) >= 0) {
                        throw new ApiParameterSyntaxException(levelName, searchTerm, "Search terms cannot contain '" + SEPARATOR_WRONG + "'.");
                    }
                }
                assert levelSearchTerms.size() == levelNames.size();

                // Add the search to the list of searches.
                levelSearchTermsList.add(levelSearchTerms);
            }
            assert levelSearchTermsList.size() == nrOfSearches;

            // First try and find the response.
            final SearchResultsDTO foundResults = configuration.matchNode(levelSearchTermsList);
            if (foundResults.isEmpty()) {
                throw new ApiNotFoundException("No result found: query=" + levelSearchTermsList);
            }

            // Check if the ETag matches (make sure we remove the "--gzip" suffix from the ETag).
            final String eTag = calculateETag(foundResults);
            final boolean eTagMatches;
            if (ifNoneMatch != null) {
                final String ifNoneMatchCleaned = removeQuotedAndLowercase(ifNoneMatch);

                // Use "startsWith" to strip of "--gzip" suffixes etc.
                eTagMatches = eTag.startsWith(ifNoneMatchCleaned);
            } else {

                // Missing ETag, so consider it different.
                eTagMatches = false;
            }
            LOG.debug("findBestMatch: etag='{}', matches={}", eTag, eTagMatches);

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
                LOG.debug("findBestMatch: NOT MODIFIED");
                return Futures.successful(null);
            }

            if (foundResults.size() == 1) {
                final SearchResultDTO entity = foundResults.get(0);
                entity.validate();
                LOG.debug("findBestMatch: OK, entity={}", entity);
                response.resume(Response.status(Status.OK).entity(entity).
                        tag(eTag).
                        lastModified((lastModified == null) ? null : lastModified.toDate()).
                        build());
            } else {
                foundResults.validate();
                LOG.debug("findBestMatch: OK, found={}", foundResults);
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
            @Nullable final String ifModifiedSince,
            @Nullable final String ifNoneMatch,
            @Nonnull final UriInfo uriInfo,
            @Nonnull final AsyncResponse response) {

        // Keep URI parameters.
        final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        processor.process("getNode", LOG, response, () -> {
            LOG.info("getNode: fullNodePath={}, if-modified-since={}, if-none-match={}", fullNodePath, ifModifiedSince, ifNoneMatch);

            // Make sure no search parameters are specified.
            if (!queryParameters.keySet().isEmpty()) {
                throw new ApiForbiddenException("Can't specify search parameters when retrieving specific configuration tree nodes");
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
     * Create an ETag value for an object. Not quoted.
     *
     * @param object Object to create an ETag for.
     * @return ETag string (not quoted).
     */
    @Nonnull
    private static String calculateETag(@Nonnull final Object object) {
        final String json = Json.toJson(object);
        final SHA1Hash hash = SHA1Hash.saltedHash(json, HASH_SALT);
        return hash.toString();
    }

    /**
     * Unquote a string.
     *
     * @param quotedString Quoted input string.
     * @return Trimmed, unquoted and lowercase string.
     */
    @Nonnull
    private static String removeQuotedAndLowercase(@Nonnull final String quotedString) {
        final String result;
        final String trimmed = quotedString.trim();
        if (trimmed.lastIndexOf('"') > trimmed.indexOf('"')) {
            assert trimmed.length() > 2;
            result = trimmed.substring(1, trimmed.length() - 1);
        } else {
            result = trimmed;
        }
        return result.toLowerCase();
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

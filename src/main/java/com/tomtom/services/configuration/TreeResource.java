/**
 * Copyright (C) 2016, TomTom International BV (http://www.tomtom.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.services.configuration;

import com.tomtom.services.configuration.dto.SearchResultsDTO;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * This class defines the methods to query the configurations search tree.
 */
@Path("/tree")
@SuppressWarnings("squid:S1214")
public interface TreeResource {

    String PATH_PARAM = "path";
    String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    String HEADER_IF_NONE_MATCH = "If-None-Match";

    char SEPARATOR_QUERY = ',';
    char SEPARATOR_WRONG = ';';
    char SEPARATOR_PATH = '/';

    /**
     * Try to match a search query string with the search tree and return the deepest
     * level parameters found in the tree.
     * <p>
     * Return codes:
     * 200 - Match (exact or partial) found.
     * 304 - Not newer than If-Modified-Since time or no different ETag than If-None-Match.
     * 404 - No match found.
     * <p>
     * The return message has format {@link SearchResultsDTO} and specifies the full
     * path of the deepest node matching the search path.
     * <p>
     * Important: as the URI overlaps with the "getNode" URI for the root node, this method actually
     * returns the root node of the search tree, if no search parameters were provided.
     *
     * @param ifModifiedSince Return parameters only if the configuration is newer than this.
     * @param ifNoneMatch     Return parameters only if the ETag of the response is different from the supplied ETag.
     * @param uriInfo         Includes search parameters (if empty, returns root node).
     * @param response        Deepest level parameters, format {@link SearchResultsDTO}.
     */
    @GET
    @Path("")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    void findBestMatch(
            @Nullable @HeaderParam(HEADER_IF_MODIFIED_SINCE) String ifModifiedSince,
            @Nullable @HeaderParam(HEADER_IF_NONE_MATCH) String ifNoneMatch,
            @Nonnull @Context UriInfo uriInfo,
            @Suspended @Nonnull AsyncResponse response);

    /**
     * Get a specific node from the search tree, given a search path.
     * <p>
     * Return codes:
     * 200 - Exact match found.
     * 304 - Not newer than If-Modified-Since time or no different ETag than If-None-Match.
     * 404 - No match found.
     *
     * @param fullNodePath    Full path of node.
     * @param ifModifiedSince Return parameters only if the configuration is newer than this.
     * @param ifNoneMatch     Return parameters only if the ETag of the response is different from the supplied ETag.
     * @param uriInfo         Includes search parameters (which should not be present).
     * @param response        Deepest level parameters, format {@link SearchResultsDTO}.
     */
    @GET
    @Path('{' + PATH_PARAM + ": [^?]*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    void getNode(
            @Nonnull @PathParam(PATH_PARAM) String fullNodePath,
            @Nullable @HeaderParam(HEADER_IF_MODIFIED_SINCE) String ifModifiedSince,
            @Nullable @HeaderParam(HEADER_IF_NONE_MATCH) String ifNoneMatch,
            @Nonnull @Context UriInfo uriInfo,
            @Suspended @Nonnull AsyncResponse response);
}

/**
 * Copyright (C) 2019. TomTom NV (http://www.tomtom.com)
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

package com.tomtom.services.configuration;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

/**
 * This class defines a number of helper methods for the service, such as for getting help text,
 * version and status information.
 */
@Path("/")
public interface HelperResource {

    /**
     * This method provides help info.
     *
     * @return Returns help text as HTML.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Nonnull
    String getHelpHTML();

    /**
     * Additional method: this method returns the current version of the application.
     * <p>
     * Return HTTP status 200.
     *
     * @param response Version, {@link com.tomtom.services.configuration.dto.VersionDTO}.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("version")
    void getVersion(@Suspended @Nonnull AsyncResponse response);

    /**
     * This method returns whether the service is operational or not (status code 200 is OK).
     *
     * @param response Returns a version number as JSON.
     */
    @GET
    @Path("status")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    void getStatus(@Suspended @Nonnull AsyncResponse response);
}

/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface RootResource {

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

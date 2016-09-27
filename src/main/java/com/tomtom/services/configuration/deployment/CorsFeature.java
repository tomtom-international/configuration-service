/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.deployment;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * This class adds the Cross-Origin Resource Sharing feature to allow Javascript to call
 * this service from another domain. It essentially add the calling domain to the list
 * of allowed origins in the HTTP header.
 *
 * The feature is enabled by adding it to web.xml
 *
 * <pre>
 *   &lt;context-param&gt;
 *     &lt;param-name&gt;resteasy.providers&lt;/param-name&gt;
 *     &lt;param-value&gt;com.mapcode.services.deployment.CorsFeature&lt;/param-value&gt;
 *   &lt;/context-param&gt;
 * </pre>
 */
@Provider
public class CorsFeature implements Feature {

    @Override
    public boolean configure(@Nonnull final FeatureContext featureContext) {

        // Add the appropriate CORS header.
        final CorsFilter corsFilter = new CorsFilter();
        corsFilter.getAllowedOrigins().add("*");
        featureContext.register(corsFilter);
        return true;
    }
}

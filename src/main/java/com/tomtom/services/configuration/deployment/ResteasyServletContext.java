/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.deployment;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

@Provider
@ServerInterceptor
public class ResteasyServletContext implements ContainerRequestFilter {

    @Inject
    ServletContext servletContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Map<Class<?>, Object> contextDataMap = ResteasyProviderFactory.getContextDataMap();
        contextDataMap.put(ServletContext.class, servletContext);
    }
}

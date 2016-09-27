/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.deployment;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Binder;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.RootResource;
import com.tomtom.services.configuration.TreeResource;
import com.tomtom.services.configuration.implementation.Configuration;
import com.tomtom.services.configuration.implementation.RootResourceImpl;
import com.tomtom.services.configuration.implementation.TreeResourceImpl;
import com.tomtom.speedtools.guice.GuiceConfigurationModule;
import com.tomtom.speedtools.json.Json;
import com.tomtom.speedtools.rest.GeneralExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import javax.ws.rs.core.Response.Status;


/**
 * This class defines the deployment configuration for Google Guice.
 *
 * The deployment module "bootstraps" the whole Guice injection process.
 *
 * It bootstraps the Guice injection and specifies the property files to be read. It also needs to bind the tracer, so
 * they can be used early on in the app. Finally, it can bind a "startup check" (example provided) as an eager
 * singleton, so the system won't start unless a set of basic preconditions are fulfilled.
 *
 * The "speedtools.default.properties" is required, but its values may be overridden in other property files.
 */
public class DeploymentModule extends GuiceConfigurationModule {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentModule.class);

    public DeploymentModule() {
        super(
                "classpath:configuration-service.default.properties", // Default set required by SpeedTools.
                "classpath:configuration-service.properties");         // Additional property file(s).
    }

    @Override
    public void configure(@Nonnull final Binder binder) {
        assert binder != null;
        super.configure(binder);

        // Make sure incorrect JSON doesn't return a HTTP 500, but HTTP 400 code.
        GeneralExceptionMapper.addCustomException(JsonParseException.class, false, Status.BAD_REQUEST);

        // Bind APIs to their implementation.
        binder.bind(RootResource.class).to(RootResourceImpl.class).in(Singleton.class);
        binder.bind(TreeResource.class).to(TreeResourceImpl.class).in(Singleton.class);

        // Bind properties.
        binder.bind(ConfigurationServiceProperties.class).in(Singleton.class);

        // Bind tree data as eager singleton tor read config data immediately.
        binder.bind(Configuration.class).asEagerSingleton();

        // Bind start-up checking class (example).
        binder.bind(StartupCheck.class).asEagerSingleton();

        final ObjectMapper jsonMapper = Json.getCurrentJsonObjectMapper();
        jsonMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        Json.getCurrentJsonObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
}

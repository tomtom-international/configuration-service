/*
 * Copyright (C) 2012-2021, TomTom (http://tomtom.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.services.configuration.deployment;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Binder;
import com.tomtom.services.configuration.ConfigurationServiceProperties;
import com.tomtom.services.configuration.HelperResource;
import com.tomtom.services.configuration.TreeResource;
import com.tomtom.services.configuration.implementation.Configuration;
import com.tomtom.services.configuration.implementation.HelperResourceImpl;
import com.tomtom.services.configuration.implementation.TreeResourceImpl;
import com.tomtom.speedtools.guice.GuiceConfigurationModule;
import com.tomtom.speedtools.json.Json;
import com.tomtom.speedtools.rest.GeneralExceptionMapper;

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

    public DeploymentModule() {
        super(
                "classpath:configuration-service.default.properties", // Default set required by SpeedTools.
                "classpath:configuration-service.properties");         // Additional property file(s).
    }

    @Override
    public void configure(@Nonnull final Binder binder) {
        assert binder != null;

        /**
         * Important: the next call will fail if tbe properties file is not found.
         * Normally, the properties file should NOT be part of the WAR file and must
         * be added to the deployment manually so the service is able to find it.
         * See also the README file in the 'src/main/resource' directory.
         */
        super.configure(binder);

        // Make sure incorrect JSON doesn't return a HTTP 500, but HTTP 400 code.
        GeneralExceptionMapper.addCustomException(JsonParseException.class, false, Status.BAD_REQUEST);

        // Bind APIs to their implementation.
        binder.bind(HelperResource.class).to(HelperResourceImpl.class).in(Singleton.class);
        binder.bind(TreeResource.class).to(TreeResourceImpl.class).in(Singleton.class);

        // Bind properties.
        binder.bind(ConfigurationServiceProperties.class).in(Singleton.class);

        // Bind tree data as eager singleton tor read config data immediately.
        binder.bind(Configuration.class).asEagerSingleton();

        // Bind start-up checking class (example).
        binder.bind(StartupCheck.class).asEagerSingleton();

        final ObjectMapper jsonMapper = Json.getCurrentJsonObjectMapper();
        jsonMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
}

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

package com.tomtom.services.configuration;

import com.tomtom.speedtools.guice.HasProperties;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class contains the properties which are specific for the Configuration Service.
 * It uses SpeedTools and Guice to inject the properties in the constructor.
 */
public class ConfigurationServiceProperties implements HasProperties {

    @Nonnull
    private final String startupConfigurationURI;

    @Inject
    public ConfigurationServiceProperties(
            @Named("ConfigurationService.startupConfigurationURI") @Nonnull final String startupConfigurationURI) {
        this.startupConfigurationURI = startupConfigurationURI.trim();
    }

    @Nonnull
    public String getStartupConfigurationURI() {
        return startupConfigurationURI;
    }
}

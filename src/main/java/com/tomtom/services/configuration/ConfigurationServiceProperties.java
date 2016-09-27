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

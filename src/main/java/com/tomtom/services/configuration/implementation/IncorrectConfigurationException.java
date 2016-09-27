/*
 * Copyright (C) 2016, TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.implementation;

import javax.annotation.Nonnull;

/**
 * This exception is thrown for incorrect configurations.
 */
public class IncorrectConfigurationException extends Exception {

    public IncorrectConfigurationException(@Nonnull final String message) {
        super(message);
    }
}

/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.tomtom.services.configuration.dto.ParameterDTO;
import com.tomtom.speedtools.json.Json;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * This class contains a single parameter, which is a
 * key, value pair.
 */
@Immutable
@JsonInclude(Include.NON_EMPTY)
final public class Parameter {

    @Nonnull
    final private String key;

    @Nonnull
    final private String value;

    public Parameter(
            @Nonnull final String key,
            @Nonnull final String value) {
        this.key = key;
        this.value = value;
    }

    public Parameter(@Nonnull final ParameterDTO parameterDTO) {
        this(parameterDTO.getKey(), parameterDTO.getValue());
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    @Override
    @Nonnull
    public String toString() {
        return Json.toJson(this);
    }
}

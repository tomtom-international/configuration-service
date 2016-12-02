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
public final class Parameter {

    @Nonnull
    private final String key;

    @Nonnull
    private final String value;

    public Parameter(
            @Nonnull final String key,
            @Nonnull final String value) {
        this.key = key;
        this.value = value;
    }

    public Parameter(@Nonnull final ParameterDTO parameterDTO) {
        //noinspection ConstantConditions Already checked in Configuration during load
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

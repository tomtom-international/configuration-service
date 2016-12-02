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

package com.tomtom.services.configuration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tomtom.services.configuration.domain.Parameter;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "NullableProblems", "NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "parameter")
@XmlAccessorType(XmlAccessType.FIELD)
public final class ParameterDTO extends ApiDTO implements SupportsInclude {

    /**
     * Key name. Cannot be null or empty after parsing includes.
     */
    @JsonProperty("key")
    @XmlElement(name = "key")
    @Nullable
    private String key;

    /**
     * Value. Cannot be null after parsing includes, but can be empty.
     */
    @JsonProperty("value")
    @XmlElement(name = "value")
    @Nullable
    private String value;

    /**
     * Include name. Can be null.
     */
    @JsonProperty("include")
    @XmlElement(name = "include")
    @Nullable
    private String include;

    /**
     * Include name. Can be null.
     */
    @JsonProperty("include_array")
    @XmlElement(name = "include_array")
    @Nullable
    private String includeArray;

    /**
     * For an explanation of validate(), see {@link NodeDTO}.
     */
    @Override
    public void validate() {
        validator().start();

        // This validation is ONLY executed after includes have been expanded, so they must be null.
        validator().checkNull(true, "include_array", includeArray);
        validator().checkNull(true, "include", include);
        validator().checkNotNull(true, "key", key);
        validator().checkString(true, "key", key, 1, Integer.MAX_VALUE);
        validator().checkNotNull(true, "value", value);
        validator().done();
    }

    public ParameterDTO(
            @Nonnull final String key,
            @Nonnull final String value) {
        super(false);
        setKey(key);
        setValue(value);
    }

    /**
     * Convert a Parameter object into a ParameterDTO.
     *
     * @param parameter Parameter to convert.
     */
    public ParameterDTO(@Nonnull final Parameter parameter) {
        this(parameter.getKey(), parameter.getValue());
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    ParameterDTO() {
        // Default constructor required by JAX-B.
        super(false);
    }

    @Override
    @Nullable
    public String getIncludeArray() {
        beforeGet();
        return includeArray;
    }

    public void setIncludeArray(@Nonnull final String includeArray) {
        beforeSet();
        this.includeArray = includeArray.trim();
    }

    @Override
    @Nullable
    public String getInclude() {
        beforeGet();
        return include;
    }

    public void setInclude(@Nonnull final String include) {
        beforeSet();
        this.include = include.trim();
    }

    @Nullable
    public String getKey() {
        beforeGet();
        return key;
    }

    public void setKey(@Nonnull final String key) {
        beforeSet();
        this.key = key.trim();
    }

    @Nullable
    public String getValue() {
        beforeGet();
        return value;
    }

    public void setValue(@Nonnull final String value) {
        beforeSet();
        this.value = StringUtils.trim(value);  // Empty string allowed.
    }
}

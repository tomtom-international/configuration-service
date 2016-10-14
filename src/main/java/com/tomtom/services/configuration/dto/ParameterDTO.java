/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
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
public class ParameterDTO extends ApiDTO {

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
        if (include != null) {
            validator().checkNotNull(true, "include", include);
            validator().checkNull(true, "include_array", includeArray);
            validator().checkNull(true, "key", key);
            validator().checkNull(true, "value", value);
        } else if (includeArray != null) {
            validator().checkNotNull(true, "include_array", includeArray);
            validator().checkNull(true, "include", include);
            validator().checkNull(true, "key", key);
            validator().checkNull(true, "value", value);
        } else {
            validator().checkNull(true, "include_array", includeArray);
            validator().checkNull(true, "include", include);
            validator().checkNotNull(true, "key", key);
            validator().checkString(true, "key", key, 1, Integer.MAX_VALUE);
            validator().checkNotNull(true, "value", value);
        }
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

    @Nullable
    public String getIncludeArray() {
        beforeGet();
        return includeArray;
    }

    public void setIncludeArray(@Nonnull final String includeArray) {
        beforeSet();
        this.includeArray = includeArray.trim();
    }

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

    @Nonnull
    public String getValue() {
        beforeGet();
        return value;
    }

    public void setValue(@Nonnull final String value) {
        beforeSet();
        this.value = StringUtils.trim(value);                       // Empty string allowed.
    }
}

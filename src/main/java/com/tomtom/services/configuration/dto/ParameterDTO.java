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
     * Key name. Cannot be null or empty.
     */
    @JsonProperty("key")
    @XmlElement(name = "key")
    @Nonnull
    private String key;

    /**
     * Value. Cannot be null, but can be empty.
     */
    @JsonProperty("value")
    @XmlElement(name = "value")
    @Nonnull
    private String value;

    /**
     * For an explanation of validate(), see {@link NodeDTO}.
     */
    @Override
    public void validate() {
        validator().start();
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

    @Nonnull
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

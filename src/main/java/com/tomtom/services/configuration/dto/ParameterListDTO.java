/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tomtom.speedtools.apivalidation.ApiListDTO;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "parameters")
@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterListDTO extends ApiListDTO<ParameterDTO> {

    /**
     * This class does not have properties itself: it is just a
     * list of elements (represented by 'this').
     */

    /**
     * For an explanation of validate(), see {@link NodeDTO}.
     *
     * Contrary to an ApiDTO object, the class ApiListDTO handles validate() and ta derived
     * class only needs to implement a method to check a single list element.
     */
    @Override
    public void validateOne(@Nonnull final ParameterDTO elm) {
        validator().checkNotNullAndValidate(false, "parameter", elm);
    }

    public ParameterListDTO(@Nonnull final List<ParameterDTO> parameters) {
        super(parameters);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    ParameterListDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @JsonProperty("parameters")
    @JsonUnwrapped
    @XmlElement(name = "parameter")
    @Nonnull
    public List<ParameterDTO> getParameters() {
        return this;
    }
}

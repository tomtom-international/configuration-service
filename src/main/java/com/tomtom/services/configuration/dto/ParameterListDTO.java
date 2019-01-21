/**
 * Copyright (C) 2016, TomTom NV (http://www.tomtom.com)
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
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tomtom.speedtools.apivalidation.ApiListDTO;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * This class does not have properties itself: it is just a
 * list of elements (represented by 'this').
 *
 * For an explanation of validate(), see {@link NodeDTO}.
 *
 * Contrary to an ApiDTO object, the class ApiListDTO handles validate() and ta derived
 * class only needs to implement a method to check a single list element.
 */

@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "parameters")
@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterListDTO extends ApiListDTO<ParameterDTO> {

    public ParameterListDTO(@Nonnull final List<ParameterDTO> parameters) {
        super(false, parameters);
    }

    @SuppressWarnings({"UnusedDeclaration", "squid:MissingDeprecatedCheck", "squid:S1133"})
    @Deprecated
    ParameterListDTO() {
        // Default constructor required by JAX-B.
        super(false);
    }

    @Override
    public void validateOne(@Nonnull final ParameterDTO elm) {
        validator().checkNotNullAndValidate(false, "parameter", elm);
    }

    @JsonProperty("parameters")
    @JsonUnwrapped
    @XmlElement(name = "parameter")
    @Nonnull
    public List<ParameterDTO> getParameters() {
        return this;
    }
}

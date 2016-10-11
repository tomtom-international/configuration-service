/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.services.configuration.domain.Parameter;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * This class represents a single element in the response of a 'GET' call to query the
 * search tree, with multiple search criteria.
 * The response includes (any number) of parameters and an optional 'matchedPath'
 * element, which specifies the fallback node of the list of parameters in the
 * search tree.
 */
@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "searchResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResultDTO extends ApiDTO {

    @JsonProperty("parameters")
    @JsonUnwrapped
    @XmlElementWrapper(name = "parameters")
    @XmlElement(name = "parameter")
    @Nullable
    private ParameterListDTO parameters;

    /**
     * Optional element, which is used to specify the path of the node which contains the specified
     * parameters.
     */
    @JsonProperty("matched")
    @JsonInclude(Include.ALWAYS)
    @XmlElement(name = "matched")
    @Nullable
    private String matched;

    /**
     * Reference to original node. This field is not serialized.
     */
    @SuppressWarnings("NullableProblems")
    @JsonIgnore
    @Nonnull
    transient private Node node;

    /**
     * For an explanation of validate(), see {@link NodeDTO}.
     */
    @Override
    public void validate() {
        validator().start();
        validator().checkNotNullAndValidate(false, "parameters", parameters);
        validator().checkNotNull(false, "matched", matched);
        validator().checkNotNull(true, "node", node);
        validator().done();
    }

    public SearchResultDTO(
            @Nullable final ParameterListDTO parameters,
            @Nullable final String matched,
            @Nonnull final Node node) {
        super(false);
        setParameters(parameters);
        setMatched(matched);
        setNode(node);
    }

    public SearchResultDTO(@Nonnull final Node node) {
        final Set<Parameter> parameters = node.getParameters();
        final List<ParameterDTO> parameterDTOs = new ArrayList<>();
        if (parameters == null) {
            this.parameters = null;
        } else {
            for (final Parameter parameter : parameters) {
                parameterDTOs.add(new ParameterDTO(parameter));
            }
        }
        setParameters(new ParameterListDTO(parameterDTOs));
        setMatched(null);
        setNode(node);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    SearchResultDTO() {
        // Default constructor required by JAX-B.
        super(false);
    }

    @Nullable
    public ParameterListDTO getParameters() {
        beforeGet();
        return parameters;
    }

    public void setParameters(@Nullable final ParameterListDTO parameters) {
        beforeSet();
        this.parameters = ((parameters == null) || parameters.isEmpty()) ? null : parameters;
    }

    @Nullable
    public String getMatched() {
        beforeGet();
        return matched;
    }

    public void setMatched(@Nullable final String matched) {
        beforeSet();
        this.matched = nullToEmpty(StringUtils.trim(matched));
    }

    @Nonnull
    public Node getNode() {
        beforeGet();
        return node;
    }

    public void setNode(@Nonnull final Node node) {
        beforeSet();
        this.node = node;
    }
}

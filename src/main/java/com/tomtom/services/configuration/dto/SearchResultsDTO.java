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

/**
 * This class represents the response of a 'GET' call to query the
 * search tree.
 */
@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "searchResults")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchResultsDTO extends ApiListDTO<SearchResultDTO> {

    /**
     * This class does not have properties itself: it is just a
     * list of elements (represented by 'this').
     */

    /**
     * For an explanation of validate(), see {@link NodeDTO}.
     */
    @Override
    public void validateOne(@Nonnull final SearchResultDTO elm) {
        validator().checkNotNullAndValidate(false, "searchResult", elm);
    }

    public SearchResultsDTO(@Nonnull final List<SearchResultDTO> searchResults) {
        super(searchResults);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    SearchResultsDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    @JsonProperty("searchResults")
    @JsonUnwrapped
    @XmlElement(name = "searchResult")
    @Nonnull
    public List<SearchResultDTO> getSearchResults() {
        return this;
    }
}
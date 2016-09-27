/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import com.tomtom.services.configuration.domain.Node;
import com.tomtom.speedtools.objects.Immutables;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchResultDTOTest {
    private static final Logger LOG = LoggerFactory.getLogger(SearchResultDTOTest.class);

    @Test
    public void testEquals() {
        final Node root = new Node("root");
        final SearchResultDTO x1 = new SearchResultDTO(new ParameterListDTO(Immutables.listOf(new ParameterDTO("x", "1"), new ParameterDTO("y", "2"))), "a/b/c", root);
        final SearchResultDTO x2 = new SearchResultDTO(new ParameterListDTO(Immutables.listOf(new ParameterDTO("x", "1"), new ParameterDTO("y", "2"))), "a/b/c", root);
        final SearchResultDTO x3 = new SearchResultDTO(new ParameterListDTO(Immutables.listOf(new ParameterDTO("x", "1"), new ParameterDTO("y", "X"))), "a/b/c", root);
        final SearchResultDTO x4 = new SearchResultDTO(new ParameterListDTO(Immutables.listOf(new ParameterDTO("x", "1"), new ParameterDTO("y", "2"))), "a/X/c", root);
        Assert.assertTrue(x1.equals(x2));
        Assert.assertTrue(x2.equals(x1));
        Assert.assertFalse(x1.equals(x3));
        Assert.assertFalse(x3.equals(x1));
        Assert.assertFalse(x1.equals(x4));
        Assert.assertFalse(x4.equals(x1));

        Assert.assertTrue(x1.hashCode() == x2.hashCode());
        Assert.assertTrue(x2.hashCode() == x1.hashCode());
        Assert.assertFalse(x1.hashCode() == x3.hashCode());
        Assert.assertFalse(x3.hashCode() == x1.hashCode());
        Assert.assertFalse(x1.hashCode() == x4.hashCode());
        Assert.assertFalse(x4.hashCode() == x1.hashCode());
    }
}

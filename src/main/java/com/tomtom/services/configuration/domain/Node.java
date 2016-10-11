/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tomtom.services.configuration.dto.NodeDTO;
import com.tomtom.speedtools.json.Json;
import com.tomtom.speedtools.objects.Immutables;
import com.tomtom.speedtools.time.UTCTime;
import com.tomtom.speedtools.xmladapters.DateTimeAdapter.JsonDateTimeStringDeserializer;
import com.tomtom.speedtools.xmladapters.DateTimeAdapter.JsonSerializerWithSecondsResolution;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * This class represents nodes in the search tree. A node has a name, a list of children nodes
 * optionally a number of parameters and a last modified time.
 */
@Immutable
@JsonInclude(Include.NON_EMPTY)
final public class Node {

    /**
     * Node match string. The match is null for the root node and non-null,
     * non-empty for other nodes.
     */
    @Nullable
    final private String match;

    /**
     * Children nodes (optional), null if none. The collection cannot be empty (only null).
     */
    @Nullable
    final private Set<Node> nodes;

    /**
     * Parameters leaf (optional), null if none. The collection cannot be empty (only null).
     */
    @Nullable
    final private Set<Parameter> parameters;

    /**
     * The last modified date of this node. This is the latest modified date of the node itself
     * and all of its children.
     */
    @JsonSerialize(using = JsonSerializerWithSecondsResolution.class)
    @JsonDeserialize(using = JsonDateTimeStringDeserializer.class)
    @Nullable
    final private DateTime modified;

    /**
     * Order of node level names. The map cannot be empty (only null). This property is actually
     * only allowed for the root node.
     */
    @Nullable
    final private List<String> levels;

    /**
     * The parentNode property holds a convenient link to the parent of this node.
     * It's null for the root node only. This link may be used to traverse the tree
     * all the way up to the root, for example, to find the applicable 'modified' time.
     */
    @JsonIgnore
    @Nullable
    transient final private Node parentNode;

    public Node(
            @Nullable final String match,
            @Nullable final Collection<Node> nodes,
            @Nullable final Collection<Parameter> parameters,
            @Nullable final DateTime modified,
            @Nullable final List<String> levels,
            @Nullable final Node parentNode) {
        this.match = match;
        this.nodes = ((nodes == null) || nodes.isEmpty()) ? null : Immutables.setOf(nodes);
        this.parameters = ((parameters == null) || parameters.isEmpty()) ? null : Immutables.setOf(parameters);
        this.modified = modified;
        this.levels = ((levels == null) || levels.isEmpty()) ? null : Immutables.listOf(levels);
        this.parentNode = parentNode;
    }

    public Node(@Nonnull final String match) {
        this(match, null, null, null, null, null);
    }

    /**
     * Create a Node object from a NodeDTO.
     *
     * @param nodeDTO    Node to convert.
     * @param parentNode Parent of node to create, null for the root node.
     */
    public Node(@Nonnull final NodeDTO nodeDTO, @Nullable Node parentNode) {

        // Set name. Replace null-name (always root) with root name.
        this.match = nodeDTO.getMatch();

        // Set parameters, create an immutable list.
        final Collection<Parameter> parameters = new ArrayList<>();
        if (nodeDTO.getParameters() != null) {
            nodeDTO.getParameters().stream().forEach(parameterDTO -> {
                parameters.add(new Parameter(parameterDTO));
            });
        }
        this.parameters = parameters.isEmpty() ? null : Immutables.setOf(parameters);

        // Set modified date/time, get latest modified from children as well.
        this.modified = (nodeDTO.getModified() == null) ? null : UTCTime.from(ISODateTimeFormat.dateTimeParser().parseDateTime(nodeDTO.getModified()));

        // Set ordering of level names.
        this.levels = (nodeDTO.getLevels() == null) ? null : Immutables.listOf(nodeDTO.getLevels());

        // Set the parent node.
        this.parentNode = parentNode;

        // Set nodes, create an immutable list.
        final Collection<Node> childNodes = new ArrayList<>();
        if (nodeDTO.getNodes() != null) {
            for (final NodeDTO childNodeDTO : nodeDTO.getNodes()) {
                final Node childNode = new Node(childNodeDTO, this);
                childNodes.add(childNode);
            }
        }
        this.nodes = childNodes.isEmpty() ? null : Immutables.setOf(childNodes);
    }

    @Nullable
    public String getMatch() {
        return match;
    }

    @Nullable
    public Set<Node> getNodes() {
        return nodes;
    }

    @Nullable
    public Set<Parameter> getParameters() {
        return parameters;
    }

    @Nullable
    public DateTime getModified() {
        return modified;
    }

    @Nullable
    public List<String> getLevels() {
        return levels;
    }

    @Nullable
    public Node getParentNode() {
        return parentNode;
    }

    @Override
    @Nonnull
    public String toString() {
        return Json.toJson(this);
    }

    /**
     * Convenience method to retrieve the applicable 'modified' time for this node.
     * Search all the way up to the root until we find one.
     *
     * @return Applicable 'modified' time, or null if none exists.
     */
    @Nullable
    public DateTime searchModifiedUpToRoot() {
        Node other = this;
        while (true) {
            if (other.modified != null) {
                return other.modified;
            } else if (other.parentNode != null) {
                other = other.parentNode;
            } else {
                return null;
            }
        }
    }
}

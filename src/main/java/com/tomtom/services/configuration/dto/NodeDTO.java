/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tomtom.services.configuration.domain.Node;
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.objects.Immutables;
import com.tomtom.speedtools.time.UTCTime;
import com.tomtom.speedtools.utils.StringUtils;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "node")
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeDTO extends ApiDTO {

    /**
     * Node name, can only be null for root node. Cannot be empty, only null.
     */
    @JsonProperty("name")
    @XmlElement(name = "name")
    @Nullable
    private String name;

    /**
     * List of nodes, can be null, but not empty.
     */
    @JsonProperty("nodes")
    @XmlElementWrapper(name = "nodes")
    @XmlElement(name = "node")
    @Nullable
    private List<NodeDTO> nodes;

    /**
     * List of parameters, can be null, but not empty.
     */
    @JsonProperty("parameters")
    @JsonUnwrapped
    @XmlElementWrapper(name = "parameters")
    @XmlElement(name = "parameter")
    @Nullable
    private ParameterListDTO parameters;

    /**
     * Last modified date/time of this node. Can be null.
     */
    @JsonProperty("modified")
    @XmlElement(name = "modified")
    @Nullable
    private String modified;

    /**
     * List of nodes level names, can be null, but not empty.
     */
    @JsonProperty("levels")
    @XmlElementWrapper(name = "levels")
    @XmlElement(name = "level")
    @Nullable
    private List<String> levels;

    /**
     * Path of include file. This property is mutually exclusive with name/nodes/parameters/modified:
     * the contents of the include file effectively replace those attributes.
     */
    @JsonProperty("include")
    @XmlElement(name = "include")
    @Nullable
    private String include;

    /**
     * The method validate() is called to check the validity of the DTO object. Note that the DTO
     * objects are treated as immutable objects. This means that the normal sequence of operation
     * is:
     * - The constructor gets called when an object gets read (e.g. by Jackson), or created to be output.
     * - Zero or more setters get called (either by Jackson or the code that produces an output object).
     * - The validate() method is called to accumulate validation errors throw a BadRequest exception.
     * - Zero or more getters are called to retrieve the values of read objects.
     *
     * The SpeedTools API framework enforces that this order is not violated. So:
     * - No setters are allowed to be called after the validate() method, or any of the getters.
     * - No getters is allowed before the objects has been validated.
     *
     * In order to be able to modify a DTO object tree after reading it in and getting some properties,
     * the framework allows you to 'reset the validator', which effectively means you can set properties
     * of the object again. After setting the objects, at some point, you will need to call the validate()
     * again to check if you modified the object according to the validation rules. Don't call validate()
     * too often: this may be an expensive call. So, preferably do that at the end only, just after no more
     * setters will be called.
     */
    @Override
    public void validate() {
        validator().start();
        if (include == null) {

            // No include specified.
            validator().checkString(false, "name", name, 1, Integer.MAX_VALUE);
            if (nodes != null) {
                validator().checkNotNullAndValidateAll(false, "nodes", Immutables.listOf(nodes));
            }
            validator().checkNotNullAndValidate(false, "parameters", parameters);
            validator().checkNull(false, "include", include);
            if (modified != null) {

                // Check if the modified date/time can be parsed.
                boolean ok = false;
                try {
                    UTCTime.from(ISODateTimeFormat.dateTimeParser().parseDateTime(modified));
                    ok = true;
                } catch (final IllegalArgumentException ignored) {
                    // Ignore, check is below.
                }

                // If it can't be parsed, output an understandable message.
                if (!ok) {
                    validator().checkAllowedValues(true, "modified", modified, "YYYY-MM-DDTHH:mm:ssZ");
                }

                // Check length of string (longer than 20 chars indicates non-'Z' timezone.
                validator().checkString(true, "modified", ok ? modified : "", 20, 20);
            }
            validator().checkNotNull(false, "levels", levels);
        } else {

            // Include specified: all others must be null.
            validator().checkNull(false, "name", name);
            validator().checkNull(false, "nodes", nodes);
            validator().checkNull(false, "parameters", parameters);
            validator().checkNull(false, "modified", modified);
            validator().checkNull(false, "levels", levels);
            validator().checkString(true, "include", include, 1, Integer.MAX_VALUE);
        }
        validator().done();
    }

    public NodeDTO(
            @Nullable final String name,
            @Nullable final List<NodeDTO> nodes,
            @Nullable final ParameterListDTO parameters,
            @Nullable final String modified,
            @Nullable final List<String> levels,
            @Nullable final String include) {
        super(false);
        setName(name);
        setNodes(nodes);
        setParameters(parameters);
        setModified(modified);
        setLevels(levels);
        setInclude(include);
    }

    /**
     * Create a NodeDTO from a Node object.
     *
     * @param node Node to be converted.
     */
    public NodeDTO(@Nonnull final Node node) {

        // Set name.
        setName(node.getName());

        // Copy nodes.
        if (node.getNodes() == null) {
            setNodes(null);
        } else {
            final List<NodeDTO> nodeDTOs = new ArrayList<>();
            node.getNodes().stream().forEach(childNode -> {
                nodeDTOs.add(new NodeDTO(childNode));
            });
            setNodes(nodeDTOs);
        }

        // Copy parameters.
        if (node.getParameters() == null) {
            setParameters(null);
        } else {
            final List<ParameterDTO> parameterDTOs = new ArrayList<>();
            node.getParameters().stream().forEach(parameter -> {
                parameterDTOs.add(new ParameterDTO(parameter));
            });
            setParameters(new ParameterListDTO(parameterDTOs));
        }

        // Set modified date/time.
        setModified((node.getModified() == null) ? null : ISODateTimeFormat.dateTimeNoMillis().print(node.getModified()));

        // Set level name order.
        if (node.getLevels() == null) {
            setLevels(null);
        } else {
            setLevels(node.getLevels());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    NodeDTO() {
        // Default constructor required by JAX-B.
        super(false);
    }

    @Nullable
    public String getName() {
        beforeGet();
        return name;
    }

    public void setName(@Nullable final String name) {
        beforeSet();
        this.name = StringUtils.emptyToNull(StringUtils.trim(name));
    }

    @Nullable
    public List<NodeDTO> getNodes() {
        beforeGet();
        return nodes;
    }

    public void setNodes(@Nullable final List<NodeDTO> nodes) {
        beforeSet();
        this.nodes = nodes;
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
    public String getInclude() {
        beforeGet();
        return include;
    }

    public void setInclude(@Nullable final String include) {
        beforeSet();
        this.include = StringUtils.emptyToNull(StringUtils.trim(include));
    }

    @Nullable
    public String getModified() {
        beforeGet();
        return modified;
    }

    public void setModified(@Nullable final String modified) {
        beforeSet();
        this.modified = StringUtils.emptyToNull(StringUtils.trim(modified));
    }

    @Nullable
    public List<String> getLevels() {
        beforeGet();
        return levels;
    }

    public void setLevels(@Nullable final List<String> levels) {
        beforeSet();
        if ((levels == null) || levels.isEmpty()) {
            this.levels = null;
        } else {
            this.levels = new ArrayList<>();
            for (final String level : levels) {
                this.levels.add(level.trim());
            }
        }
    }
}
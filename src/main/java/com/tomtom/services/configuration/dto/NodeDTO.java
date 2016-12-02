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
public final class NodeDTO extends ApiDTO implements SupportsInclude {

    /**
     * Node match string, can only be null for root node. Cannot be empty, only null.
     */
    @JsonProperty("match")
    @XmlElement(name = "match")
    @Nullable
    private String match;

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
     * List of nodes level names, can be null, but not empty. This field is only
     * allowed for the root level node.
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
     * Path of include file. This property is mutually exclusive with name/nodes/parameters/modified:
     * the contents of the include file replace this node entirely.
     */
    @JsonProperty("include_array")
    @XmlElement(name = "include_array")
    @Nullable
    private String includeArray;

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

        // This validation is ONLY executed after includes have been expanded, so they must be null.
        validator().checkString(false, "match", match, 1, Integer.MAX_VALUE);
        if (nodes != null) {
            validator().checkNotNullAndValidateAll(false, "nodes", Immutables.listOf(nodes));
        }
        validator().checkNotNullAndValidate(false, "parameters", parameters);
        validator().checkNull(true, "include_array", includeArray);
        validator().checkNull(true, "include", include);
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
        //noinspection VariableNotUsedInsideIf
        if (match == null) {
            validator().checkNotNull(false, "levels", levels);
        } else {
            validator().checkNull(true, "levels", levels);
        }
        validator().done();
    }

    public NodeDTO(
            @Nullable final String match,
            @Nullable final List<NodeDTO> nodes,
            @Nullable final ParameterListDTO parameters,
            @Nullable final String modified,
            @Nullable final List<String> levels,
            @Nullable final String include,
            @Nullable final String includeArray) {
        super(false);
        setMatch(match);
        setNodes(nodes);
        setParameters(parameters);
        setModified(modified);
        setLevels(levels);
        setInclude(include);
        setIncludeArray(includeArray);
    }

    /**
     * Create a NodeDTO from a Node object.
     *
     * @param node Node to be converted.
     */
    public NodeDTO(@Nonnull final Node node) {

        // Set name.
        setMatch(node.getMatch());

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
    public String getMatch() {
        beforeGet();
        return match;
    }

    public void setMatch(@Nullable final String match) {
        beforeSet();
        this.match = StringUtils.emptyToNull(StringUtils.trim(match));
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

    @Override
    @Nullable
    public String getInclude() {
        beforeGet();
        return include;
    }

    public void setInclude(@Nullable final String include) {
        beforeSet();
        this.include = StringUtils.emptyToNull(StringUtils.trim(include));
    }

    @Override
    @Nullable
    public String getIncludeArray() {
        beforeGet();
        return includeArray;
    }

    public void setIncludeArray(@Nullable final String includeArray) {
        beforeSet();
        this.includeArray = StringUtils.emptyToNull(StringUtils.trim(includeArray));
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

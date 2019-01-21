/**
 * Copyright (C) 2019. TomTom NV (http://www.tomtom.com)
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
import com.tomtom.speedtools.apivalidation.ApiDTO;
import com.tomtom.speedtools.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class provides the DTO for the 'version' call. The version call returns the POM
 * version of the service, as well as the full path of the configuration file (as that
 * defines the version of the configuration itself).
 */
@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "squid:S2160"})
@JsonInclude(Include.NON_EMPTY)
@XmlRootElement(name = "version")
@XmlAccessorType(XmlAccessType.FIELD)
public final class VersionDTO extends ApiDTO {
    public static final int API_VERSION_MAX_LENGTH = 25;
    public static final int API_VERSION_MIN_LENGTH = 0;

    /**
     * Version string of service. No assumptions can be made on its format.
     */
    @JsonProperty("version")
    @XmlElement(name = "version")
    @Nullable
    private String version;

    /**
     * The URI of the startup configuration, which was read during startup of the service.
     * If not start configuration was specified, this element is empty.
     */
    @XmlElement(name = "startupConfigurationURI")
    @Nullable
    private String startupConfigurationURI;

    public VersionDTO(
            @Nonnull final String version,
            @Nullable final String startupConfigurationURI) {
        super();
        setVersion(version);
        setStartupConfigurationURI(startupConfigurationURI);
    }

    @SuppressWarnings({"UnusedDeclaration", "squid:MissingDeprecatedCheck", "squid:S1133"})
    @Deprecated
    VersionDTO() {
        // Default constructor required by JAX-B.
        super();
    }

    /**
     * For an explanation of validate(), see {@link NodeDTO}.
     */
    @Override
    public void validate() {
        validator().start();
        validator().checkString(true, "version", version,
                API_VERSION_MIN_LENGTH, API_VERSION_MAX_LENGTH);
        validator().checkString(false, "startupConfigurationURI", startupConfigurationURI,
                0, Integer.MAX_VALUE);
        validator().done();
    }

    @Nonnull
    public String getVersion() {
        beforeGet();
        //noinspection ConstantConditions
        return version;                             // Cannot be null after validation.
    }

    public void setVersion(@Nonnull final String version) {
        beforeSet();
        this.version = StringUtils.trim(version);
    }

    @Nullable
    public String getStartupConfigurationURI() {
        beforeGet();
        return startupConfigurationURI;
    }

    public void setStartupConfigurationURI(@Nullable final String startupConfigurationURI) {
        beforeSet();
        this.startupConfigurationURI = StringUtils.emptyToNull(StringUtils.trim(startupConfigurationURI));
    }
}

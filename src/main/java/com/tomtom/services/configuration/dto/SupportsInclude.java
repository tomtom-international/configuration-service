/**
 * Copyright (C) 2016, TomTom International BV (http://www.tomtom.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.tomtom.services.configuration.dto;


import javax.annotation.Nullable;

/**
 * Interface that allows access to the include parameters. To be implemented by any type that
 * should be replaceable by the include processing logic.
 */
public interface SupportsInclude {

    /**
     * Get the include string.
     *
     * @return Value of "include" (non-empty), or null if no include was specified
     */
    @Nullable
    String getInclude();

    /**
     * Get the include string (non-empty), or null if no include was specified.
     * @return Value of "include_array" (non-empty), or null if no include was specified.

     */
    @Nullable  String getIncludeArray();
}

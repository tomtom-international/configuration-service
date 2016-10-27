/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration.dto;

import com.sun.istack.Nullable;

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
    @Nullable String getInclude();

    /**
     * Get the include string (non-empty), or null if no include was specified.
     * @return Value of "include_array" (non-empty), or null if no include was specified.

     */
    @Nullable  String getIncludeArray();
}

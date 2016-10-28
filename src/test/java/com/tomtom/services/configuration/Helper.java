/*
 * Copyright (C) 2016. TomTom International BV. All rights reserved.
 */

package com.tomtom.services.configuration;

import javax.annotation.Nonnull;

public final class Helper {

    @Nonnull
    public static String quoted(@Nonnull final String s) {
        return '"' + s + '"';
    }
}

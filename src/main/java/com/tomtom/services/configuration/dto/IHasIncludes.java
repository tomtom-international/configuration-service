package com.tomtom.services.configuration.dto;

/**
 * Interface that allows access to the include parameters. To be implemented by any type that should be replaceable by
 * the include processing logic.
 */
public interface IHasIncludes {
    String getInclude();
    String getIncludeArray();
}

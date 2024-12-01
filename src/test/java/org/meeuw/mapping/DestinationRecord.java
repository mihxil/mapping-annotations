package org.meeuw.mapping;

import org.meeuw.mapping.annotations.Source;

@lombok.Builder
public record DestinationRecord(
    @Source
    String title
) {
}

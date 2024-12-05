package org.meeuw.mapping;

import lombok.Data;

import org.meeuw.mapping.annotations.Source;

@Data
public abstract class AbstractDestination {


    @Source(jsonPointer ="/title")
    String title;
}

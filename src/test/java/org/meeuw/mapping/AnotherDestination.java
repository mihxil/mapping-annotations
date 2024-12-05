package org.meeuw.mapping;

import lombok.Getter;
import lombok.Setter;
import org.meeuw.mapping.annotations.Source;

@Getter@Setter
@Source(field = "json")
public class AnotherDestination extends  AbstractDestination {

    public AnotherDestination() {

    }


    @Source(jsonPointer ="/description")
    String description;


}

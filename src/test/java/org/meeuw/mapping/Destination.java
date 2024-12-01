package org.meeuw.mapping;

import lombok.Getter;
import lombok.Setter;
import org.meeuw.mapping.annotations.Source;

@Getter@Setter
public class Destination {

    public Destination() {

    }
    
    @Source(field = "moreJson", pointer ="/title")
    @Source(field = "json", pointer ="/title", sourceClass = SourceObject.class)
    String title;


    @Source(field = "anotherJson", pointer ="/a/b/value")
    String description;

}

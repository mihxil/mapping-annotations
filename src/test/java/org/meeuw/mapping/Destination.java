package org.meeuw.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.meeuw.mapping.annotations.Source;

@Getter@Setter
public class Destination {

    public Destination() {

    }
    
    @Source(field = "anotherJson", pointer ="/title") // doesn't exist in SourceObject
    @Source(field = "json", pointer ="/title", sourceClass = SourceObject.class)
    String title;


    @Source(field = "moreJson", pointer ="/a/b/value")
    String description;

    @Source()
    JsonNode moreJson;
    
    @Source(field = "subObject", path="id", sourceClass = ExtendedSourceObject.class)
    Long id;
    
    @Source(field = "moreJson", pointer ="/nisv.currentbroadcaster")
    List<SubObject> list;

}

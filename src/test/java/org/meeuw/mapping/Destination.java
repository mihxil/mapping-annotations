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
    
    @Source(field = "anotherJson", jsonPointer ="/title") // doesn't exist in SourceObject
    @Source(field = "json", jsonPointer ="/title", sourceClass = SourceObject.class)
    String title;


    @Source(field = "moreJson", jsonPointer ="/a/b/value")
    String description;

    @Source()
    JsonNode moreJson;
    
    @Source(field = "subObject", path="id", sourceClass = ExtendedSourceObject.class)
    Long id;
    
    @Source(field = "moreJson", jsonPointer ="/nisv.currentbroadcaster", groups = Test1Class.class)
    List<SubObject> list;
    
    @Source(field = "moreJson", jsonPath ="['nisv.currentbroadcaster'][*]['currentbroadcaster.broadcaster']", groups = Test2Class.class)
    List<SubObject> list2;


}

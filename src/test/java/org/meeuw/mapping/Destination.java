package org.meeuw.mapping;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import org.meeuw.mapping.annotations.Source;

import com.fasterxml.jackson.databind.JsonNode;

@Getter
@Setter
public class Destination {

    public Destination() {

    }

    @Source(field = "anotherJson", jsonPointer ="/title") // doesn't exist in SourceObject
    @Source(field = "json", jsonPointer ="/title", sourceClass = SourceObject.class)
    String title;


    @Source(field = "moreJson", jsonPointer ="/a/b/value")
    String description;

    @Source(jsonPointer = "/")
    JsonNode moreJson;

    @Source(field = "subObject", path="id", sourceClass = ExtendedSourceObject.class)
    Long id;

    @Source(field = "moreJson", jsonPointer ="/nisv.currentbroadcaster", groups = Test1Class.class)
    List<SubSourceObject> list;

    @Source(field = "moreJson", jsonPath ="['nisv.currentbroadcaster'][*]['currentbroadcaster.broadcaster']", groups = Test2Class.class)
    List<SubSourceObject> list2;

    @Source(field = "json", jsonPointer = "")
    SubDestination sub;

    @Source(field = "json", jsonPath = "subs")
    List<SubDestination> subs;



}

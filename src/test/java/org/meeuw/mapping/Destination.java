package org.meeuw.mapping;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.meeuw.mapping.annotations.Source;

import com.fasterxml.jackson.databind.JsonNode;

import nl.vpro.xml.bind.LocalDateXmlAdapter;

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

    @Source(field = "json", jsonPointer = "/sub")
    SubDestination sub;

    @Source(field = "json", jsonPath = "subs")
    List<SubDestination> subs;

    @Source(field ="json", jsonPath = "enum")
    ExampleEnum enumValue;


    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    @Source(field ="moreJson", jsonPath = "date")
    LocalDate localDate;

}

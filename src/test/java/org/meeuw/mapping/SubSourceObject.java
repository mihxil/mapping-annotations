package org.meeuw.mapping;

import lombok.*;

import org.meeuw.mapping.annotations.Source;

import com.fasterxml.jackson.databind.JsonNode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubSourceObject {

    @Source(sourceClass = JsonNode.class, jsonPointer = "/currentbroadcaster.broadcaster/resolved_value")
    String broadcaster;



    @Source(sourceClass = JsonNode.class, jsonPointer = "/resolved_value")
    String broadcaster2;

    long id;
}

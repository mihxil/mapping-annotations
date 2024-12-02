package org.meeuw.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.meeuw.mapping.annotations.Source;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubObject {
    
    @Source(sourceClass = JsonNode.class, pointer = "/currentbroadcaster.broadcaster/value")
    String broadcaster;    
        
    long id;
}

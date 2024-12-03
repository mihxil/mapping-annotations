package org.meeuw.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.meeuw.mapping.annotations.Source;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubObject {
    
    @Source(sourceClass = JsonNode.class, jsonPointer = "/currentbroadcaster.broadcaster/resolved_value")
    String broadcaster;    
    
    
        
    @Source(sourceClass = JsonNode.class, jsonPointer = "/resolved_value")
    String broadcaster2;    
        
    long id;
}

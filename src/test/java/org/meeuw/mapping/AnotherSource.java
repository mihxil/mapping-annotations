package org.meeuw.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnotherSource {

    static final ObjectMapper MAPPER = new ObjectMapper();

    JsonNode anotherJson;

    @SneakyThrows
    public static AnotherSource of(String json) {
        return new AnotherSource(MAPPER.readTree(json));
    }

}

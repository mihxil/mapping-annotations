package org.meeuw.mapping.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.meeuw.mapping.Destination;
import org.meeuw.mapping.ExtendedSourceObject;

class JsonUtilTest {

    @Test
    public void unwrap() throws JsonProcessingException {
        JsonNode node = JsonUtil.MAPPER.readTree("""
           [
           null,
           true,
           1,
           1.0,
           [1, 2, 3],
           "text"
           ]
           """);
       List<Object> unwrapped = (List<Object>) JsonUtil.unwrapJson(node);
       assertThat(unwrapped).containsExactly(
           null,
           Boolean.TRUE,
           1,
           1.0,
           List.of(1, 2, 3),
           "text"
       );
    }

    @Test
    void getValue() throws NoSuchFieldException {
        ExtendedSourceObject sourceObject = new ExtendedSourceObject();
        sourceObject.setJson("{'title': 'foobar'}".getBytes(StandardCharsets.UTF_8));


        Optional<Object> title = JsonUtil.getSourceValue(sourceObject, Destination.class.getDeclaredField("title"));
        assertThat(title).contains("foobar");
    }

}
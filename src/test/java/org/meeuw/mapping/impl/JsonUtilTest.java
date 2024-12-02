package org.meeuw.mapping.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.meeuw.mapping.*;

@Log4j2
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
    
    
    @Test
    public void mapJsonObject() throws JsonProcessingException {
        SubObject subObject = new SubObject();

        JsonNode node = new ObjectMapper().readTree(""" 
          {
               "currentbroadcaster.broadcaster": {
                            "value": "209345",
                              "origin": "https://lab-vapp-bng-01.mam.beeldengeluid.nl/api/metadata/thesaurus/~THE30/209345",
                              "resolved_value": "VPRO"
                            }
                        
          }
          """);
        Mapper.map(node, subObject);

        assertThat(subObject.getBroadcaster()).isEqualTo("VPRO");
        
    }
    
    @Test
    void list() throws NoSuchFieldException {
        SourceObject source = new SourceObject();
        source.setMoreJson(""" 
          {
            "nisv.currentbroadcaster": [
                          {
                            "currentbroadcaster.broadcaster": {
                              "value": "209345",
                              "origin": "https://lab-vapp-bng-01.mam.beeldengeluid.nl/api/metadata/thesaurus/~THE30/209345",
                              "resolved_value": "VPRO"
                            }
                          },
                          {
                            "currentbroadcaster.broadcaster": {
                              "value": "209346",
                              "origin": "https://lab-vapp-bng-01.mam.beeldengeluid.nl/api/metadata/thesaurus/~THE30/209346",
                              "resolved_value": "TROS"
                            }
                          }
                        ]
          }
          """);


           Optional<Object> list = JsonUtil.getSourceValue(source, Destination.class.getDeclaredField("list"));
        
           log.info("{}", list);
        
        
    }

}
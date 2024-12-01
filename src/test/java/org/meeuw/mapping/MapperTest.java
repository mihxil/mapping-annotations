package org.meeuw.mapping;

import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

@Log4j2
class MapperTest {

    @Test
    public void test() {
        Destination destination = new Destination();
        SourceObject sourceObject = new SourceObject();
        sourceObject.setJson("{'title': 'foobar'}".getBytes(StandardCharsets.UTF_8));

        Mapper.map(sourceObject, destination);;
        log.info("{}", destination);
        assertThat(destination.getTitle()).isEqualTo("foobar");
    }
  
}
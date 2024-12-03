package org.meeuw.mapping;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(destination.getMoreJson()).isEqualTo(sourceObject.getMoreJson());

    }



    @Test
    public void test2() {
        Destination destination = new Destination();
        AnotherSource sourceObject = AnotherSource.of("""
        {"title": "foobar"}
        """
        );

        Mapper.map(sourceObject, destination);;
        log.info("{}", destination);
        assertThat(destination.getTitle()).isEqualTo("foobar");
    }

    @Test
    public void time() {
        Instant start = Instant.now();
        for (int i = 0; i < 1_000_000; i++) {
            Destination destination = new Destination();
            ExtendedSourceObject sourceObject = new ExtendedSourceObject();
            sourceObject.setTitle("foobar");
            sourceObject.setSubObject(new SubObject("a", null, 1L ));
            sourceObject.setMoreJson("""
            {"title": "foobar"}
            """
            );
            Mapper.map(sourceObject, destination);

            log.debug("{}", destination);
        }
        log.info("Took {}", Duration.between(start, Instant.now()));
    }

    @Test
    public void toRecord() {
        SourceObject sourceObject = new SourceObject();
        sourceObject.setTitle("bla bla");
        var builder = DestinationRecord.builder();
        Mapper.map(sourceObject, builder);
        var r = builder.build();
        assertThat(r.title()).isEqualTo("bla bla");
    }


}
package org.meeuw.mapping;

import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.meeuw.mapping.Mapper.MAPPER;

@Log4j2
class MapperTest {

    @Test
    public void test() {
        Destination destination = new Destination();
        SourceObject sourceObject = new SourceObject();
        sourceObject.json("{'title': 'foobar'}".getBytes(StandardCharsets.UTF_8));

        MAPPER.map(sourceObject, destination);;
        log.info("{}", destination);
        assertThat(destination.title()).isEqualTo("foobar");
        assertThat(destination.moreJson()).isEqualTo(sourceObject.moreJson());

    }



    @Test
    public void test2() {
        Destination destination = new Destination();
        AnotherSource sourceObject = AnotherSource.of("""
        {"title": "foobar"}
        """
        );

        MAPPER.map(sourceObject, destination);;
        log.info("{}", destination);
        assertThat(destination.title()).isEqualTo("foobar");
    }

    @Test
    public void time() {
        String moreJson = """
            {"title": "foobar"}
            """;
        Mapper mapper = MAPPER.withClearJsonCacheEveryTime(false).withSupportJaxbAnnotations(false);
        Instant start = Instant.now();
        for (int i = 0; i < 1_000; i++) {
            Destination destination = new Destination();
            ExtendedSourceObject sourceObject = new ExtendedSourceObject();
            sourceObject.title("foobar");
            sourceObject.subObject(new SubSourceObject("a", null, 1L ));
            sourceObject.moreJson(moreJson);

            mapper.map(sourceObject, destination);

            log.debug("{}", destination);
        }
        log.info("Took {}", Duration.between(start, Instant.now()));
    }

    @Test
    public void toRecord() {
        SourceObject sourceObject = new SourceObject().title("bla bla");
        var builder = DestinationRecord.builder();
        MAPPER.map(sourceObject, builder);
        var r = builder.build();
        assertThat(r.title()).isEqualTo("bla bla");
    }



   @Test
   void getMappedDestinationProperties() {
       assertThat(MAPPER.getMappedDestinationProperties(
           ExtendedSourceObject.class,
           Destination.class
       ).keySet()).containsExactlyInAnyOrder("title", "description", "moreJson", "id", "list", "list2", "sub", "subs", "enumValue");

       assertThat(MAPPER.getMappedDestinationProperties(
           SourceObject.class,
           Destination.class
       ).keySet()).containsExactlyInAnyOrder("title", "description", "moreJson", "list", "list2", "sub", "subs", "enumValue");
   }

    @Test
    void getMappedDestinationProperties2() {
        assertThat(MAPPER.getMappedDestinationProperties(AnotherSource.class , Destination.class).keySet()).containsExactlyInAnyOrder("title");
    }


    @Test
    void withDefaults() {
        SourceObject sourceObject = new SourceObject();
        sourceObject.json("""
            {
                title: "foo",
                description: "bar"
            }
            """.getBytes(StandardCharsets.UTF_8));

        AnotherDestination anotherDestination = MAPPER.map(sourceObject, AnotherDestination.class);
        assertThat(anotherDestination.title()).isEqualTo("foo");
        assertThat(anotherDestination.description()).isEqualTo("bar");

    }

    @Test
    void mapException() {
        assertThatThrownBy(() -> {
            MAPPER.map(new Object(), ThrowingDestination.class);
        }).isInstanceOf(MapException.class);
    }

    @Test
    void customMapping() {


        Mapper mapper = MAPPER.withCustomJsonMapper(SubDestination.class, (json, field) -> {
            SubDestination so =  new SubDestination();
            so.a(json.get("title").asText() + "/" + json.get("description").asText());
            return Optional.of(so);

            }
        );

        SourceObject sourceObject = new SourceObject();
        sourceObject.json("""
            { sub: {
                title: "foo",
                description: "bar"
                }
            }
            """.getBytes(StandardCharsets.UTF_8));

        Destination destination = mapper.map(sourceObject, Destination.class);
        assertThat(destination.sub().a()).isEqualTo("foo/bar");
    }



    @Test
    void customMappingForList() {

        Mapper mapper = MAPPER.withCustomJsonMapper(SubDestination.class, (json, field) -> {
            if (json.isObject() && json.has("title") && json.has("description")) {
                SubDestination so = new SubDestination();
                so.a(json.get("title").asText() + "/" + json.get("description").asText());
                return Optional.of(so);
            } else {
                return Optional.empty();
            }

            }
        );

        SourceObject sourceObject = new SourceObject();
        sourceObject.json("""
            { "subs" : [
            {
                title: "foo",
                description: "bar"
            }]}
            """.getBytes(StandardCharsets.UTF_8));

        Destination destination = mapper.map(sourceObject, Destination.class);
        assertThat(destination.subs().get(0).a()).isEqualTo("foo/bar");
    }


    @Test
    void enums() {

        Mapper mapper = MAPPER;


        SourceObject sourceObject = new SourceObject();
        {
            sourceObject.json("""
                { "enum" : "a" }
                """.getBytes(StandardCharsets.UTF_8));

            Destination destination = mapper.map(sourceObject, Destination.class);
            assertThat(destination.enumValue()).isEqualTo(ExampleEnum.a);
        }

        {
            sourceObject.json("""
                { "enum" : "alfa" }
                """.getBytes(StandardCharsets.UTF_8));

            Destination destination = mapper.map(sourceObject, Destination.class);
            assertThat(destination.enumValue()).isEqualTo(ExampleEnum.a);

            Destination destination2 = mapper.withSupportJaxbAnnotations(false).map(sourceObject, Destination.class);
            assertThat(destination2.enumValue()).isNull();
        }
    }




}

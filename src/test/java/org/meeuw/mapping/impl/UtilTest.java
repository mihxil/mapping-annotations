package org.meeuw.mapping.impl;

import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.meeuw.mapping.*;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
class UtilTest {



   @Test
   void getSourceField() {
       {
           Field value = Util.getSourceField(AnotherSource.class, "anotherJson").orElseThrow();
           assertThat(value.getName()).isEqualTo("anotherJson");
           assertThat(value.getDeclaringClass()).isEqualTo(AnotherSource.class);
       }
       {
           Field value = Util.getSourceField(ExtendedSourceObject.class, "moreJson").orElseThrow();
           assertThat(value.getName()).isEqualTo("moreJson");
           assertThat(value.getDeclaringClass()).isEqualTo(SourceObject.class);
       }
   }

   @Test
   void getSourceValue() {
       SourceObject source = new SourceObject();
       source.moreJson("{'title': 'foobar'}");
       Optional<Object> moreJson = Util.getSourceValue(source, "moreJson");
       assertThat(moreJson).contains(source.moreJson());
   }

    @Test
    void getExtendedSourceValue() {
        ExtendedSourceObject source = new ExtendedSourceObject();
        source.moreJson("{'title': 'foobar'}");

        Optional<Object> moreJson = Util.getSourceValue(source, "moreJson");
        assertThat(moreJson).contains(source.moreJson());
    }
    @Test
    void withPath() {
        ExtendedSourceObject source = new ExtendedSourceObject();
        source.subObject(new SubSourceObject(null, null, 123L));


        Optional<Object> id = Util.getSourceValue(source, "subObject", "id");
        assertThat(id).contains(123L);
    }

    @Test
    void smartPath() {
       var test = """
           {
                "creator.serialnumber" : {
                  "value" : "1"
                },
                "creator.name" : {
                  "value" : "79767",
                  "origin" : "https://lab-vapp-bng-01.mam.beeldengeluid.nl/api/metadata/thesaurus/~THE22/79767",
                  "resolved_value" : "Bijster, Jacob"
                },
                "creator.role" : {
                  "value" : "1909263",
                  "origin" : "https://lab-vapp-bng-01.mam.beeldengeluid.nl/api/metadata/dictionary/~DIC47/1909263",
                  "resolved_value" : "componist"
                }
              }
           """;
        JsonPath path = JsonPath.compile("*[?(@.origin =~ /.*\\/metadata\\/dictionary\\/.*/)].value");
        Object read = JsonPath.parse(test).read(path);
        log.info("{}",  "" +  read);

    }



}

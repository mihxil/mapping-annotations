package org.meeuw.mapping.impl;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.meeuw.mapping.*;

import static org.assertj.core.api.Assertions.assertThat;

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
        source.subObject(new SubObject(null, null, 123L));


        Optional<Object> id = Util.getSourceValue(source, "subObject", "id");
        assertThat(id).contains(123L);
    }



}

package org.meeuw.mapping.impl;

import java.lang.reflect.Field;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.meeuw.mapping.*;

class UtilTest {



   @Test
   public void getSourceField() {

       {
           Field value = Util.getSourceField(AnotherSource.class, "moreJson").orElseThrow();
           assertThat(value.getName()).isEqualTo("moreJson");
           assertThat(value.getDeclaringClass()).isEqualTo(AnotherSource.class);
       }
       {
           Field value = Util.getSourceField(ExtendedSourceObject.class, "anotherJson").orElseThrow();
           assertThat(value.getName()).isEqualTo("anotherJson");
           assertThat(value.getDeclaringClass()).isEqualTo(SourceObject.class);
       }
   }

   @Test
   public void getSourceValue() {
       SourceObject source = new SourceObject();
       source.setAnotherJson("{'title': 'foobar'}");
       Optional<Object> moreJson = Util.getSourceValue(source, "anotherJson");
       assertThat(moreJson).contains(source.getAnotherJson());
   }

    @Test
    public void getExtendedSourceValue() {
        ExtendedSourceObject source = new ExtendedSourceObject();
       source.setAnotherJson("{'title': 'foobar'}");
       Optional<Object> moreJson = Util.getSourceValue(source, "anotherJson");
       assertThat(moreJson).contains(source.getAnotherJson());
   }





}
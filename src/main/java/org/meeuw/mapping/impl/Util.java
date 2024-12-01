/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.impl;

import java.lang.reflect.Field;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.annotations.Sources;

@Slf4j
public class Util {

    private Util() {
        // no instances allowed
    }



    public static Optional<Source> getAnnotation(Class<?> sourceClass, Field f) {
        Source s = null;
        {

            Sources sources = f.getAnnotation(Sources.class);
            if (sources != null) {
                for (Source proposal : sources.value()) {
                    if (proposal.sourceClass().isAssignableFrom(sourceClass)) {
                        if (s == null) {
                            s = proposal;
                        } else {
                            if (s.sourceClass().isAssignableFrom(proposal.sourceClass())) {
                                // this means proposal is more specific
                                s = proposal;
                            }
                        }
                    }
                }
            } else {
                s = f.getAnnotation(Source.class);
            }
        }
        return Optional.ofNullable(s);
    }
    //private

    public static Optional<Field> getSourceField(final Class<?> sourceClass, String sourceField) {

        Class<?> clazz= sourceClass;

        while(clazz != null) {
            try {
                Field declaredField = clazz.getDeclaredField(sourceField);
                declaredField.setAccessible(true);
                return Optional.of(declaredField);
            } catch (NoSuchFieldException nsfe) {

            }
            clazz = clazz.getSuperclass();
        }
        log.warn("No source field {} found for {}", sourceField, sourceClass);
        return Optional.empty();
    }

     public static Optional<Object> getSourceValue(Object source, String sourceField) {

         return getSourceField(source.getClass(), sourceField).flatMap(f -> getSourceValue(source, f));
    }
    public static Optional<Object> getSourceValue(Object source, Field f) {
          try {
              return Optional.ofNullable(f.get(source));
          } catch (IllegalAccessException e) {
              log.warn(e.getMessage());
              return Optional.empty();
          }
    }





}

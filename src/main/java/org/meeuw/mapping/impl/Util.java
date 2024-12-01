/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.impl;

import java.lang.reflect.Field;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {

    private Util() {
        // no instances allowed
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

         return getSourceField(source.getClass(), sourceField)
             .map(f -> {
                 try {
                     return f.get(source);
                 } catch (IllegalAccessException e) {
                     log.warn(e.getMessage());
                     return null;
                 }
             });
    }





}

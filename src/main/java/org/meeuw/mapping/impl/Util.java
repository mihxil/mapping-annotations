/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.impl;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.annotations.Sources;

@Slf4j
public class Util {

    private Util() {
        // no instances allowed
    }


    public static Set<String> getMappedDestinationProperties(Class<?> sourceClass, Class<?> destinationClass) {
        Set<String> result = new HashSet<>();
        Class<?> superClass = sourceClass.getSuperclass();
        if (superClass != null) {
            result.addAll(getMappedDestinationProperties(superClass, destinationClass));
        }
        for (Field field : destinationClass.getDeclaredFields()) {
            getAnnotation(sourceClass, field).ifPresent(a -> {
                result.add(field.getName());
            });
        }
        return Collections.unmodifiableSet(result);

    }

    public static Optional<String> getSource(Class<?> sourceClass, Class<?> destinationClass, String field) {

        return null;
    }


    public static Optional<Source> getAnnotation(Class<?> sourceClass, Field f) {
        Source s = null;
        Sources sources = f.getAnnotation(Sources.class);
        if (sources != null) {
            for (Source proposal : sources.value()) {

                if (matches(proposal, sourceClass, f.getName())) {
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
            Source proposal = f.getAnnotation(Source.class);
            s =  matches(proposal, sourceClass, f.getName()) ? proposal : null;
        }
        return Optional.ofNullable(s);
    }
    private static boolean matches(Source source, Class<?> sourceClass, String destinationField) {
        String field = source.field();
        if ("".equals(field)) {
            field = destinationField;
        }
        return source.sourceClass().isAssignableFrom(sourceClass) &&
            getSourceField(sourceClass, field).isPresent();

    }

    // caches make test in MapperTest about 10 times as fast.
    private static final Map<Class<?>, Map<String, Optional<Field>>> cache = new ConcurrentHashMap<>();

    public static Optional<Field> getSourceField(final Class<?> sourceClass, String sourceField) {
        // to disable cache and measure its effect
        //return _getSourceField(sourceClass, sourceField);
        Map<String, Optional<Field>> c =  cache.computeIfAbsent(sourceClass, cl -> new ConcurrentHashMap<>());

        return c.computeIfAbsent(sourceField, f -> _getSourceField(sourceClass, f));
    }

    private static Optional<Field> _getSourceField(final Class<?> sourceClass, String sourceField) {
         Class<?> clazz = sourceClass;

            while (clazz != null) {
                try {
                    Field declaredField = clazz.getDeclaredField(sourceField);
                    declaredField.setAccessible(true);
                    return Optional.of(declaredField);
                } catch (NoSuchFieldException nsfe) {

                }
                clazz = clazz.getSuperclass();
            }
            log.debug("No source field {} found for {}", sourceField, sourceClass);
            return Optional.empty();
    }

    public static Optional<Object> getSourceValue(Object source, String sourceField) {

         return getSourceField(source.getClass(), sourceField)
             .flatMap(f -> getSourceValue(source, f));
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

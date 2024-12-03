/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.*;
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

    public static Map<String, Field> getMappedDestinationProperties(Class<?> sourceClass, Class<?> destinationClass, Class<?>... groups) {
        Map<String, Field> result = new HashMap<>();
        Class<?> superClass = sourceClass.getSuperclass();
        if (superClass != null) {
            result.putAll(getMappedDestinationProperties(superClass, destinationClass));
        }
        for (Field field : destinationClass.getDeclaredFields()) {
            getAnnotation(sourceClass, field, groups)
                .ifPresent(a -> {
                    result.put(field.getName(), field);
                });
        }
        return Collections.unmodifiableMap(result);

    }


    public static Optional<Source> getAnnotation(Class<?> sourceClass, Field destinationField, Class<?>... groups) {
        destinationField =  associatedBuilderField(destinationField).orElse(destinationField);
        Source s = null;
        for (Source proposal : getAllSourceAnnotations(destinationField)) {
            if (matches(proposal, sourceClass, destinationField.getName(), groups)) {
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
        return Optional.ofNullable(s);
    }

    public static List<Source> getAllSourceAnnotations(Field destField) {
        Sources sources = destField.getAnnotation(Sources.class);
        if (sources != null) {
            return List.of(sources.value());
        }
        Source source = destField.getAnnotation(Source.class);
        if (source == null) {
            return List.of();
        } else {
            return List.of(source);
        }

    }

    private static Optional<Field> associatedBuilderField(Field f) {
        if (f != null && f.getAnnotations().length == 0) {
            Class<?> clazz = f.getDeclaringClass();
            if (clazz.getName().endsWith("Builder")) {
                try {
                    Method build = clazz.getDeclaredMethod("build");
                    if (Modifier.isPublic(build.getModifiers()) && ! Modifier.isStatic(build.getModifiers())) {
                        Class<?> targetClass = build.getReturnType();
                        Field buildField = targetClass.getDeclaredField(f.getName());
                        return Optional.of(buildField);
                    }
                } catch (NoSuchMethodException | NoSuchFieldException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isJsonField(Class<?> clazz) {
        if (JsonNode.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;

    }


    private static boolean matches(Source source, Class<?> sourceClass, String destinationField, Class<?>... groups) {
        if (source == null) {
            return false;
        }
        if (source.groups().length > 0 && groups.length > 0) {
            boolean foundMatch = false;
            OUTER:
            for (Class<?> group : source.groups()) {
                for (Class<?> groupOfSource : source.groups()) {
                    if (group.isAssignableFrom(groupOfSource)) {
                        foundMatch = true;
                        break OUTER;
                    }
                }
            }
            if (!foundMatch) {
               return false; 
            }
        }
        String field = source.field();

        if ("".equals(field)) {
            if (isJsonField(sourceClass))  {
                return true;
            } else {
                field = destinationField;
            }
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

    public static Optional<Object> getSourceValue(Object source, String sourceField, String... path) {

         return getSourceField(source.getClass(), sourceField)
             .flatMap(f -> getSourceValue(source, f, path));
    }

    public static Optional<Object> getSourceValue(Object source, Field sourceField, String... path) {
          try {
              sourceField.setAccessible(true);
              Object value = sourceField.get(source);
              for (String p : path) {
                  if (value != null) {
                      Field su = value.getClass().getDeclaredField(p);
                      su.setAccessible(true);
                      value = su.get(value);
                  }
              }
              return Optional.ofNullable(value);
          } catch (IllegalAccessException | NoSuchFieldException e) {
              log.warn(e.getMessage());
              return Optional.empty();
          }
          
    }





}

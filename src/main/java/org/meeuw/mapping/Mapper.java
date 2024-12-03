/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.impl.JsonUtil;
import static org.meeuw.mapping.impl.Util.*;

/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Slf4j
public class Mapper {

    private Mapper() {
        // no instances
    }

    /**
     * Creates a new instance (using the no-args constructor) and copies all {@link Source} annotated fields (that match) from source to it.
     * @param source The source object copy data from
     * @param destinationClass The class to create a destination object for
     * @see #map(Object, Object, Class...)
     */
    public static Object map(Object source, Class<?> destinationClass, Class<?>... groups) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object destination = destinationClass.getDeclaredConstructor().newInstance();
        map(source, destination, groups);
        return destination;
    }

    /**
     * Maps all fields in {@code destination} that are annoted with a {@link Source} that matched a field in {@code source}
     * @param source The source object
     * @param destination The destination object
     */
    public static void map(Object source, Object destination, Class<?>... groups) {
        map(source, destination, destination.getClass(), groups);
    }

    /**
     * Helper class for {@link #map(Object, Object, Class...)}, recursively called for the class and superclass of the destination
     * object.
     */
    private static void map(Object source, Object destination, Class<?> forClass, Class<?>... groups) {
        Class<?> sourceClass = source.getClass();
        Class<?> superClass = forClass.getSuperclass();
        if (superClass != null) {
            map(source, destination, superClass, groups);
        }
        for (Field f: forClass.getDeclaredFields()) {
            getAndSet(f, sourceClass, source, destination, groups);
        }
    }


    /**
     * For a field in the destination object, try to get value from the source, and set
     * this value in destination. Or do nohting if there is no match found
     */
    private static void getAndSet(
        Field destinationField,
        Class<?> sourceClass,
        Object source, 
        Object destination,
        Class<?>... groups) {
        Optional<Function<Object, Optional<Object>>> getter = sourceGetter(destinationField, sourceClass, groups);

        if (getter.isPresent()) {
            Optional<Object> value = getter.get().apply(source);
            value.ifPresentOrElse(v ->
                    destinationSetter(destinationField, sourceClass).accept(destination, v),
                () -> log.debug("No field found for {} ({}) {}", destinationField.getName(), getAllSourceAnnotations(destinationField), sourceClass));
        } else {
            log.debug("Ignored destination field {} (No (matching) @Source annotation for {})", destinationField, sourceClass);
        }
    }


    private static final Map<Field, Map<Class<?>, Optional<Function<Object, Optional<Object>>>>> GETTER_CACHE = new ConcurrentHashMap<>();

    /**
     * Return a function that will using reflection get the value from a source object that maps to the destination field.
     *
     */
    public static Optional<Function<Object, Optional<Object>>> sourceGetter(Field destinationField, Class<?> sourceClass, Class<?>... groups) {
        Map<Class<?>, Optional<Function<Object, Optional<Object>>>> c = GETTER_CACHE.computeIfAbsent(destinationField, (fi) -> new ConcurrentHashMap<>());
        return c.computeIfAbsent(sourceClass, cl -> _sourceGetter(destinationField, sourceClass));

    }

    /**
     * Uncached version of {@link #sourceGetter(Field, Class)}
     */
    private static Optional<Function<Object, Optional<Object>>> _sourceGetter(Field destinationField, Class<?> sourceClass, Class<?>... groups) {
        Optional<Source> annotation = getAnnotation(sourceClass, destinationField, groups);
        if (annotation.isPresent()) {
            final Source s = annotation.get();
            String sourceFieldName = s.field();
            if (isJsonField(sourceClass)) {
              return Optional.of(JsonUtil.valueFromJsonGetter(s));
            }
            if ("".equals(sourceFieldName)) {
                sourceFieldName = destinationField.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                final Field sf = sourceField.get();
                
                if ("".equals(s.jsonPointer()) && "".equals(s.jsonPath())) {
                    return Optional.of(source -> getSourceValue(source, sf, s.path()));
                } else {
                    return Optional.of(source -> JsonUtil.getSourceJsonValue(s, source, sf, destinationField));
                }
            }
        }
        return Optional.empty();
    }

    private static final Map<Field, Map<Class<?>, BiConsumer<Object, Object>>> SETTER_CACHE = new ConcurrentHashMap<>();

    /**
     * Returns a BiConsumer, that for a certain {@code destinationField} consumes a destination object, and sets a value
     * for the given field.
     * @param destinationField The field to set
     * @param sourceClass The currently matched class of the source object
     */
    private static  BiConsumer<Object, Object> destinationSetter(Field destinationField, Class<?> sourceClass) {
        Map<Class<?>, BiConsumer<Object, Object>> cache = SETTER_CACHE.computeIfAbsent(destinationField, fi -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(sourceClass, c -> _destinationSetter(destinationField, c));
    }

    /**
     * Uncached version of {@link #destinationSetter(Field, Class)}
     */
    private static  BiConsumer<Object, Object> _destinationSetter(Field f, Class<?> sourceClass) {
        Optional<Source> annotation = getAnnotation(sourceClass, f);
        if (annotation.isPresent()) {
            Source s = annotation.get();
            String sourceFieldName = s.field();
            if (isJsonField(sourceClass)) {
                f.setAccessible(true);
                return (destination, o) -> {
                    try {
                        f.set(destination, o);
                    } catch (Exception e) {
                        log.warn("When setting {} in {}: {}", o, f, e.getMessage());
                    }
                };
            }
            if ("".equals(sourceFieldName)) {
                sourceFieldName = f.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                f.setAccessible(true);
                return (destination, o) -> {
                    try {
                        f.set(destination, o);
                    } catch (Exception e) {
                        log.warn("When setting '{}' in {}: {}", o, f, e.getMessage());
                    }
                };
            }
        }
        return (d, v) -> {};
    }
    

}

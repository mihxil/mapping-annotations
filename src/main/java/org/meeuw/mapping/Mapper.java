/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.impl.JsonUtil;
import static org.meeuw.mapping.impl.Util.*;

/**
 * Static utilities to do the actual mapping using {@link Source}
 *
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
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     * @see #map(Object, Object, Class...)
     * @return a new object of class {@code destinationClass} which all fields filled that are found in {@code source}
     */
    public static Object map(Object source, Class<?> destinationClass, Class<?>... groups)  {
        try {
            Object destination = destinationClass.getDeclaredConstructor().newInstance();
            map(source, destination, groups);
            return destination;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new MapException(e);
        }

    }

    /**
     * Maps all fields in {@code destination} that are annoted with a {@link Source} that matched a field in {@code source}
     * @param source The source object
     * @param destination The destination object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
    public static void map(Object source, Object destination, Class<?>... groups) {
        map(source, destination, destination.getClass(), groups);
    }

    /**
     * Given a {@code sourceClass} and a {@code destinationClass} will indicate which fields  (in the destination) will be mapped.
     * @param sourceClass Class of a source object
     * @param destinationClass Class of a destination object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
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

    /**
     * Returns a function that will use reflection get the value from a source object that maps to the destination field.
     * @param destinationField Field of the destination
     * @param sourceClass Class of a source object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
    public static Optional<Function<Object, Optional<Object>>> sourceGetter(Field destinationField, Class<?> sourceClass, Class<?>... groups) {
        Map<Class<?>, Optional<Function<Object, Optional<Object>>>> c = GETTER_CACHE.computeIfAbsent(destinationField, (fi) -> new ConcurrentHashMap<>());
        return c.computeIfAbsent(sourceClass, cl -> _sourceGetter(destinationField, sourceClass));

    }




    ///  PRIVATE METHODS

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
     * Uncached version of {@link #sourceGetter(Field, Class, Class[])}
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

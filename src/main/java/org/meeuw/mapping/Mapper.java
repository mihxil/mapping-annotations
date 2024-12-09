/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.meeuw.functional.Functions;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.impl.EffectiveSource;
import org.meeuw.mapping.impl.JsonUtil;

import com.fasterxml.jackson.databind.JsonNode;

import static org.meeuw.mapping.annotations.Source.UNSET;
import static org.meeuw.mapping.impl.Util.*;

/**
 * Utilities to do the actual mapping using {@link Source}
 * A {@code Mappper} is thread safe. It only contains (unmodifiable) configuration.
 * <p>
 * New mappers (with different configuration) can be created using {@link #builder()} or using 'withers' ({@link #withClearJsonCache(boolean)}) from an existing one.
 *
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Slf4j
@AllArgsConstructor
@lombok.Builder
@Getter
public class Mapper {

    /**
     * The default {@link Mapper} instance. Mappers are stateless, but can contain some configuration.
     */
    public static final Mapper MAPPER = Mapper.builder().build();

    /**
     * The Mapper currently effective. A {@link ThreadLocal}. Defaults to {@link #MAPPER}
     */
    public static final ThreadLocal<Mapper> CURRENT = ThreadLocal.withInitial(() -> MAPPER);

    @With
    private final boolean clearJsonCache;

    @With
    @lombok.Builder.Default
    private final boolean supportXmlTypeAdapters = true;

    @With(AccessLevel.PACKAGE)
    @lombok.Builder.Default
    private final Map<Class<?>, BiFunction<JsonNode, Field, Optional<Object>>> customJsonMappers = Collections.emptyMap();


    /**
     * Creates a new instance (using the no-args constructor) and copies all {@link Source} annotated fields (that match) from source to it.
     * @param source The source object copy data from
     * @param destinationClass The class to create a destination object for
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     * @param <T> Type of the destination object
     * @see #map(Object, Object, Class...)
     * @return a new object of class {@code destinationClass} which all fields filled that are found in {@code source}
     */
    public <T> T map(Object source, Class<T> destinationClass, Class<?>... groups)  {
        try {
            T destination = destinationClass.getDeclaredConstructor().newInstance();
            map(source, destination, groups);
            return destination;
        } catch (ReflectiveOperationException e) {
            throw new MapException(e);
        }

    }

    /**
     * Just like {@link #map(Object, Class, Class[])}, but the json cache will not be deleted, and {@link #CURRENT} will not be
     * set nor removed. This is basically meant to be called by sub mappings.
     * @param source The source object copy data from
     * @param destinationClass The class to create a destination object for
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     * @param <T> Type of the destination object
     * @return A new instance of {@code destinationClass}, fill using {@code source}
     */
    public <T> T subMap(Object source, Class<T> destinationClass, Field destinationField, Class<?>... groups)  {

        if (source instanceof JsonNode json){
            BiFunction<JsonNode, Field, Optional<Object>> jsonNodeOptionalFunction = customJsonMappers.get(destinationClass);
            if (jsonNodeOptionalFunction != null) {
                Optional<Object>o = jsonNodeOptionalFunction.apply(json, destinationField);
                if (o.isPresent()) {
                    return (T) o.get();
                }
            }
        }
        try {
            T destination = destinationClass.getDeclaredConstructor().newInstance();
            subMap(source, destination, groups);
            return destination;
        } catch (ReflectiveOperationException e) {
            throw new MapException(e);
        }

    }

    /**
     * Maps all fields in {@code destination} that are annoted with a {@link Source} that matched a field in {@code source}
     * @param source The source object
     * @param destination The destination object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
    public void map(Object source, Object destination, Class<?>... groups) {
        try {
            CURRENT.set(this);
            subMap(source, destination, groups);
        } finally {
            CURRENT.remove();
            if (clearJsonCache) {
                JsonUtil.clearCache();
            }
        }
    }

    /**
     * Just like {@link #map(Object, Object, Class[])}, but the json cache will not be deleted, and {@link #CURRENT} will not be
     * set nor removed. This is basically meant to be called by sub mappings.
     * @param source The source object
     * @param destination The destination object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
     public void subMap(Object source, Object destination, Class<?>... groups) {
         map(source, destination, destination.getClass(), groups);
    }

    /**
     * Given a {@code sourceClass} and a {@code destinationClass} will indicate which fields  (in the destination) will be mapped.
     * @param sourceClass Class of a source object
     * @param destinationClass Class of a destination object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
    public Map<String, Field> getMappedDestinationProperties(Class<?> sourceClass, Class<?> destinationClass, Class<?>... groups) {
        Map<String, Field> result = new HashMap<>();
        Class<?> superClass = sourceClass.getSuperclass();
        if (superClass != null) {
            result.putAll(getMappedDestinationProperties(superClass, destinationClass));
        }
        for (Field field : destinationClass.getDeclaredFields()) {
            getAnnotation(sourceClass, destinationClass, field, groups)
                .ifPresent(a -> result.put(field.getName(), field));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns a function that will use reflection get the value from a source object that maps to the destination field.
     * @param destinationClass Field of the destination
     * @param destinationField Field of the destination
     * @param sourceClass Class of a source object
     * @param groups If not empty, only mapping is done if one (or more) of the given groups matches one of the groups of the source annotations.
     */
    public Optional<Function<Object, Optional<Object>>> sourceGetter(Class<?> destinationClass, Field destinationField, Class<?> sourceClass, Class<?>... groups) {
        Map<Class<?>, Optional<Function<Object, Optional<Object>>>> c = GETTER_CACHE.computeIfAbsent(destinationField, (fi) -> new ConcurrentHashMap<>());
        return c.computeIfAbsent(sourceClass, cl -> _sourceGetter(destinationClass, destinationField, sourceClass));

    }
    /*
    public Optional<Function<Object, Optional<Object>>> sourceGetter(Field destinationField, Class<?> sourceClass, Class<?>... groups) {

    }
*/


    public Mapper withCustomJsonMapper(Class<?> destinationClass, BiFunction<JsonNode, Field, Optional<Object>> mapper) {
        var current = new HashMap<>(customJsonMappers());
        current.put(destinationClass, mapper);
        return withCustomJsonMappers(Collections.unmodifiableMap(current));
    }

    public Mapper withCustomJsonMapper(Class<?> destinationClass, Function<JsonNode, Optional<Object>> mapper) {
        return withCustomJsonMapper(destinationClass, Functions.ignoreArg2(mapper));
    }



    ///  PRIVATE METHODS

    /**
     * Helper class for {@link #map(Object, Object, Class...)}, recursively called for the class and superclass of the destination
     * object.
     */
    private void map(Object source, Object destination, Class<?> forClass, Class<?>... groups) {
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
    private void getAndSet(
        Field destinationField,
        Class<?> sourceClass,
        Object source,
        Object destination,
        Class<?>... groups) {
        Optional<Function<Object, Optional<Object>>> getter = sourceGetter(destination.getClass(), destinationField, sourceClass, groups);

        if (getter.isPresent()) {
            Optional<Object> value = getter.get().apply(source);
            value.ifPresentOrElse(v ->
                    destinationSetter(destination.getClass(), destinationField, sourceClass).accept(destination, v),
                () -> log.debug("No field found for {} ({}) {}", destinationField.getName(), getAllSourceAnnotations(destinationField), sourceClass));
        } else {
            log.debug("Ignored destination field {} (No (matching) @Source annotation for {})", destinationField, sourceClass);
        }
    }


    private final Map<Field, Map<Class<?>, Optional<Function<Object, Optional<Object>>>>> GETTER_CACHE = new ConcurrentHashMap<>();



    /**
     * Uncached version of {@link #sourceGetter(Class, Field, Class, Class[])}
     */
    private Optional<Function<Object, Optional<Object>>> _sourceGetter(Class<?> destinationClass, Field destinationField, Class<?> sourceClass, Class<?>... groups) {
        Optional<EffectiveSource> annotation = getAnnotation(sourceClass, destinationClass, destinationField, groups);
        if (annotation.isPresent()) {
            final EffectiveSource s = annotation.get();
            String sourceFieldName = s.field();
            if (isJsonField(sourceClass)) {
              return Optional.of(JsonUtil.valueFromJsonGetter(s));
            }
            if (UNSET.equals(sourceFieldName)) {
                sourceFieldName = destinationField.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                final Field sf = sourceField.get();

                if (UNSET.equals(s.jsonPointer()) && UNSET.equals(s.jsonPath())) {
                    return Optional.of(source -> getSourceValue(source, sf, s.path()));
                } else {
                    return Optional.of(source -> JsonUtil.getSourceJsonValue(s, source, sf, destinationField));
                }
            }
        }
        return Optional.empty();
    }

    private final Map<Class<?>, Map<Field, Map<Class<?>, BiConsumer<Object, Object>>>> SETTER_CACHE = new ConcurrentHashMap<>();

    /**
     * Returns a BiConsumer, that for a certain {@code destinationField} consumes a destination object, and sets a value
     * for the given field.
     * @param destinationField The field to set
     * @param sourceClass The currently matched class of the source object
     */
    private  BiConsumer<Object, Object> destinationSetter(Class<?> destinationClass, Field destinationField, Class<?> sourceClass) {
        Map<Field, Map<Class<?>, BiConsumer<Object, Object>>> classCache = SETTER_CACHE.computeIfAbsent(destinationClass, fi -> new ConcurrentHashMap<>());
        Map<Class<?>, BiConsumer<Object, Object>> cache = classCache.computeIfAbsent(destinationField, fi -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(sourceClass, c -> _destinationSetter(destinationClass, destinationField, c));
    }

    /**
     * Uncached version of {@link #destinationSetter(Class, Field, Class)}
     */
    private  BiConsumer<Object, Object> _destinationSetter(Class<?> destinationClass, Field destinationField, Class<?> sourceClass) {
        Optional<EffectiveSource> annotation = getAnnotation(sourceClass, destinationClass, destinationField);
        if (annotation.isPresent()) {
            EffectiveSource s = annotation.get();
            String sourceFieldName = s.field();
            if (isJsonField(sourceClass)) {
                destinationField.setAccessible(true);
                return (destination, o) -> {
                    try {
                        destinationField.set(destination, valueFor(s, destinationField, o));
                    } catch (Exception e) {
                        log.warn("When setting {} in {}: {}", o, destinationField, e.getMessage());
                    }
                };
            }
            if (UNSET.equals(sourceFieldName)) {
                sourceFieldName = destinationField.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                destinationField.setAccessible(true);
                return (destination, o) -> {
                    try {
                        destinationField.set(destination, valueFor(s, destinationField, o));
                    } catch (Exception e) {
                        log.warn("When setting '{}' in {}: {}", o, destinationField, e.getMessage());
                    }
                };
            }
        }
        return (d, v) -> {};
    }


    private static final Map<Field, Optional<XmlAdapter>> ADAPTERS = new ConcurrentHashMap<>();

    Object valueFor(EffectiveSource source, Field destinationField, Object o) throws Exception {
        if (supportXmlTypeAdapters) {
            XmlAdapter adapter = ADAPTERS.computeIfAbsent(destinationField, (field) -> {
                XmlJavaTypeAdapter annotation = field.getAnnotation(XmlJavaTypeAdapter.class);
                if (annotation != null) {
                    try {
                        XmlAdapter xmlAdapter = annotation.value().getDeclaredConstructor().newInstance();
                        return Optional.of(xmlAdapter);
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
                return Optional.empty();
            }).orElse(null);
            if (adapter != null) {
                o = adapter.unmarshal(o);
            }
        }
        if (o instanceof  JsonNode json) {
            BiFunction<JsonNode, Field, Optional<Object>> customMapper = customJsonMappers.get(destinationField.getType());
            if (customMapper != null) {
                Optional<Object> tryMap = customMapper.apply(json, destinationField);
                if (tryMap.isPresent()) {
                    o = tryMap.get();
                } else {
//                    if ()
                }
            }

        }
        return o;
    }




}

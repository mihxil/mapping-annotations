package org.meeuw.mapping.impl;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.meeuw.mapping.MapException;
import org.meeuw.mapping.Mapper;

import com.fasterxml.jackson.databind.JsonNode;

import static org.meeuw.mapping.Mapper.getCurrent;

@Slf4j
public class ValueMapper {

    private static final Map<Field, Optional<XmlAdapter>> ADAPTERS = new ConcurrentHashMap<>();


    public static  Object valueFor(Mapper mapper, EffectiveSource source, Field destinationField, Class<?> destinationClass,  Object o) throws Exception {
        if (mapper.supportXmlTypeAdapters()) {
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
            BiFunction<Object, Field, Optional<Object>> customMapper = mapper.customMappers().get(destinationClass);
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


    static Object unwrapCollections(EffectiveSource effectiveSource, Object possiblyACollection, Field destination) {
        if (possiblyACollection instanceof Collection<?> list) {
            if (destination.getType() == List.class) {
                ParameterizedType genericType = (ParameterizedType) destination.getGenericType();
                Class<?> genericClass = (Class<?>) genericType.getActualTypeArguments()[0];
                if (genericClass != Object.class) {
                    return list.stream()
                        .map(o -> {
                                try {
                                    Object mapped = subMap(getCurrent(), effectiveSource, o, genericClass, destination);
                                    return mapped;
                                } catch (MapException me) {
                                    log.warn(me.getMessage(), me);
                                    return null;
                                } catch (Exception e) {
                                    log.warn(e.getMessage(), e);
                                    return null;
                                }
                            }
                        ).toList();
                }
            }

        }
        return possiblyACollection;

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
    public static <T> T subMap(Mapper mapper, EffectiveSource effectiveSource,  Object source, Class<T> destinationClass, Field destinationField, Class<?>... groups)  {

        try {
            source = ValueMapper.valueFor(mapper, effectiveSource, destinationField, destinationClass, source);
            if (destinationClass.isInstance(source)) {
                return (T) source;
            }
            T destination = destinationClass.getDeclaredConstructor().newInstance();
            mapper.subMap(source, destination,  destinationClass, groups);
            return destination;
        } catch (ReflectiveOperationException e) {
            throw new MapException(e);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }


    }

}

package org.meeuw.mapping.impl;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.meeuw.mapping.MapException;
import org.meeuw.mapping.Mapper;

import static org.meeuw.mapping.Mapper.current;

@Slf4j
public class ValueMapper {



    public static  Object valueFor(Mapper mapper,  Field destinationField, Class<?> destinationClass,  Object o) throws Exception {
        if (mapper.supportsJaxbAnnotations()) {
           o = considerXmlAdapter(o, destinationField);
        }
        o = considerEnums(o, destinationField, mapper.supportsJaxbAnnotations());
        o = considerJson(mapper, o, destinationField, destinationClass);
        return o;
    }

    private static final Map<Field, Optional<XmlAdapter<?, ?>>> ADAPTERS = new ConcurrentHashMap<>();

    private static Object considerXmlAdapter(Object o, Field destinationField) throws Exception {
        Optional<XmlAdapter<?, ?>> adapter = ADAPTERS.computeIfAbsent(destinationField, (field) -> {
            XmlJavaTypeAdapter annotation = field.getAnnotation(XmlJavaTypeAdapter.class);
            if (annotation != null) {
                try {
                    XmlAdapter<?, ?> xmlAdapter = annotation.value().getDeclaredConstructor().newInstance();
                    return Optional.of(xmlAdapter);
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }
            }
            return Optional.empty();
        });
        if (adapter.isPresent()) {
            //noinspection unchecked,rawtypes
            o = ((XmlAdapter) adapter.get()).unmarshal(o);
        }
        return o;
    }

    @SuppressWarnings("unchecked")
    private static Object considerEnums(Object o, Field destinationField, boolean considerXmlEnum) throws NoSuchFieldException {
        if (destinationField.getType().isEnum() && o instanceof String string) {
            Class<Enum<?>> enumClass = (Class<Enum<?>>) destinationField.getType();
            if (considerXmlEnum) {
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    Field f = enumConstant.getDeclaringClass().getField(enumConstant.name());
                    XmlEnumValue xmlValue = f.getAnnotation(XmlEnumValue.class);
                    if (xmlValue != null && xmlValue.value().equals(string)) {
                        return enumConstant;
                    }
                }
            }
            for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equals(string)) {
                    return enumConstant;
                }
            }

        }
        return o;
    }


    private static Object considerJson(Mapper mapper, Object o, Field destinationField, Class<?> destinationClass) {
        List<BiFunction<Object, Field, Optional<Object>>> customMappers = mapper.customMappers().get(destinationClass);
        if (customMappers != null) {
            for (BiFunction<Object, Field, Optional<Object>> customMapper: customMappers){
                Optional<Object> tryMap = customMapper.apply(o, destinationField);
                if (tryMap.isPresent()) {
                    o = tryMap.get();
                }
            }
        }
        return o;
    }


    static Object unwrapCollections(Object possiblyACollection, Field destination) {
        if (possiblyACollection instanceof Collection<?> list) {
            if (destination.getType() == List.class) {
                ParameterizedType genericType = (ParameterizedType) destination.getGenericType();
                Class<?> genericClass = (Class<?>) genericType.getActualTypeArguments()[0];
                if (genericClass != Object.class) {
                    return list.stream()
                        .map(o -> {
                                try {
                                    Object mapped = subMap(current(), o, genericClass, destination);
                                    return mapped;
                                } catch (MapException me) {
                                    log.warn(me.getMessage(), me);
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
     *
     */
    @SuppressWarnings({"ReassignedVariable", "unchecked"})
    public static <T> T subMap(Mapper mapper, Object source, Class<T> destinationClass, Field destinationField, Class<?>... groups)  {

        try {
            source = ValueMapper.valueFor(mapper, destinationField, destinationClass, source);
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

package org.meeuw.mapping;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import static org.meeuw.mapping.impl.Util.*;
import org.meeuw.mapping.json.JsonUtil;

@Slf4j
public class Mapper {

    private Mapper() {
        // no instances
    }


    public static <SOURCE, DESTINATION> void map(SOURCE source, DESTINATION destination) {
        Class<SOURCE> clazz = (Class<SOURCE>) source.getClass();
        for (Field f: destination.getClass().getDeclaredFields()) {
            getAndSet(f, clazz, source, destination);
        }
    }

    public static <SOURCE, DESTINATION> BiConsumer<SOURCE, DESTINATION> getterAndSetter(Field f, Class<SOURCE> sourceClass) {
        f.setAccessible(true);
        return (source, destination) -> getAndSet(f, sourceClass, source, destination);
    }

    private static void getAndSet(Field f, Class<?> sourceClass, Object source, Object destination) {
        Function<Object, Optional<Object>> getter = sourceGetter(f, sourceClass);
        Optional<Object> value = getter.apply(source);
        value.ifPresent( v-> {
            destinationSetter(f, sourceClass).accept(destination, v);
        });
    }


    private static final Map<Field, Map<Class<?>, Function<Object, Optional<Object>>>> getterCache = new ConcurrentHashMap<>();
    public static Function<Object, Optional<Object>> sourceGetter(Field f, Class<?> sourceClass) {
        Map<Class<?>, Function<Object, Optional<Object>>> c = getterCache.computeIfAbsent(f, (fi) -> new ConcurrentHashMap<>());
        return c.computeIfAbsent(sourceClass, cl -> _sourceGetter(f, sourceClass));

    }
    private static  Function<Object, Optional<Object>> _sourceGetter(Field f, Class<?> sourceClass) {
        Optional<Source> annotation = getAnnotation(sourceClass, f);
        if (annotation.isPresent()) {
            Source s = annotation.get();
            String sourceFieldName = s.field();
            if ("".equals(sourceFieldName)) {
                sourceFieldName = f.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                Field sf = sourceField.get();

                if ("".equals(s.pointer())) {
                    return source -> getSourceValue(source, sf);
                } else {
                    return source -> JsonUtil.getSourceJsonValue(source, sf, s.pointer());
                }
            }
        }
        return s -> Optional.empty();
    }

    private static final Map<Field, Map<Class<?>, BiConsumer<Object, Object>>> setterCache = new ConcurrentHashMap<>();

    private static  BiConsumer<Object, Object> destinationSetter(Field f, Class<?> sourceClass) {
        Map<Class<?>, BiConsumer<Object, Object>> cache = setterCache.computeIfAbsent(f, fi -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(sourceClass, c -> _destinationSetter(f, c));
    }
    private static <DESTINATION, SOURCE> BiConsumer<DESTINATION, Object> _destinationSetter(Field f, Class<SOURCE> sourceClass) {
        Optional<Source> annotation = getAnnotation(sourceClass, f);
        if (annotation.isPresent()) {
            Source s = annotation.get();
            String sourceFieldName = s.field();
            if ("".equals(sourceFieldName)) {
                sourceFieldName = f.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                f.setAccessible(true);
                return new BiConsumer<>() {
                    @Override
                    public void accept(DESTINATION destination, Object o) {
                        try {
                            f.set(destination, o);
                        } catch (Exception e) {
                            log.warn(e.getMessage());
                        }
                    }
                };
            }
        }
        return (d, v) -> {};
    }
}

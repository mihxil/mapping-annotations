package org.meeuw.mapping;

import java.lang.reflect.Field;
import java.util.Optional;
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
        return new BiConsumer<>() {
            @Override
            public void accept(SOURCE source, DESTINATION destination) {
                getAndSet(f, sourceClass, source, destination);
            }
        };


    }

    private static <SOURCE, DESTINATION> void getAndSet(Field f, Class<SOURCE> sourceClass, SOURCE source, DESTINATION destination) {
        Function<Object, Optional<Object>> getter = sourceGetter(f, sourceClass);
        Optional<Object> value = getter.apply(source);
        value.ifPresent( v-> {
            destinationSetter(f, sourceClass).accept(destination, v);
        });
    }

    public static <SOURCE> Function<Object, Optional<Object>> sourceGetter(Field f, Class<? extends SOURCE> sourceClass) {
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

    private static <DESTINATION, SOURCE> BiConsumer<DESTINATION, Object> destinationSetter(Field f, Class<SOURCE> sourceClass) {
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

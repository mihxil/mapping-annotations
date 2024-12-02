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
     * @see #map(Object, Object)
     */
    public static Object map(Object source, Class<?> destinationClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object destination = destinationClass.getDeclaredConstructor().newInstance();
        map(source, destination);
        return destination;
    }

    /**
     * Maps all fields in {@code destination} that are annoted with a {@link Source} that matched a field in {@code source}
     * @param source The source object
     * @param destination The destination object
     */
    public static void map(Object source, Object destination) {
        map(source, destination, destination.getClass());
    }

    /**
     * Helper class for {@link #map(Object, Object)}, recursively called for the class and superclass of the destination
     * object.
     */
    private static void map(Object source, Object destination, Class<?> forClass) {
        Class<?> sourceClass = source.getClass();
        Class<?> superClass = forClass.getSuperclass();
        if (superClass != null) {
            map(source, destination, superClass);
        }
        for (Field f: forClass.getDeclaredFields()) {
            getAndSet(f, sourceClass, source, destination);
        }
    }


    /**
     * For a field in the destination object, try to get value from the source, and set
     * this value in destination. Or do nohting if there is no match found
     */
    private static void getAndSet(
        Field destinationField,
        Class<?> sourceClass,
        Object source, Object destination) {
        Function<Object, Optional<Object>> getter = sourceGetter(destinationField, sourceClass);
        
        Optional<Object> value = getter.apply(source);
        value.ifPresentOrElse( v-> 
            destinationSetter(destinationField, sourceClass).accept(destination, v), 
            () -> log.warn("No field found for {} ({}) {}", destinationField.getName(), getAllSourceAnnotations(destinationField),  sourceClass));
    }


    private static final Map<Field, Map<Class<?>, Function<Object, Optional<Object>>>> GETTER_CACHE = new ConcurrentHashMap<>();

    /**
     * Return a function that will using reflection get the value from a source object that maps to the destination field.
     *
     */
    public static Function<Object, Optional<Object>> sourceGetter(Field destinationField, Class<?> sourceClass) {
        Map<Class<?>, Function<Object, Optional<Object>>> c = GETTER_CACHE.computeIfAbsent(destinationField, (fi) -> new ConcurrentHashMap<>());
        return c.computeIfAbsent(sourceClass, cl -> _sourceGetter(destinationField, sourceClass));

    }

    /**
     * Uncached version of {@link #sourceGetter(Field, Class)}
     */
    private static  Function<Object, Optional<Object>> _sourceGetter(Field destinationField, Class<?> sourceClass) {
        Optional<Source> annotation = getAnnotation(sourceClass, destinationField);
        if (annotation.isPresent()) {
            Source s = annotation.get();
            String sourceFieldName = s.field();
            if (isJsonField(sourceClass)) {
              return JsonUtil.valueFromJsonGetter(s);
            }
            if ("".equals(sourceFieldName)) {
                sourceFieldName = destinationField.getName();
            }
            Optional<Field> sourceField = getSourceField(sourceClass, sourceFieldName);
            if (sourceField.isPresent()) {
                Field sf = sourceField.get();

                if ("".equals(s.pointer())) {
                    return source -> getSourceValue(source, sf, s.path());
                } else {
                    return source -> JsonUtil.getSourceJsonValue(source,  sf, s.path(), s.pointer());
                }
            }
        }
        return s -> Optional.empty();
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
                        log.warn("When setting {} in {}: {}", o, f, e.getMessage());
                    }
                };
            }
        }
        return (d, v) -> {};
    }
    

}

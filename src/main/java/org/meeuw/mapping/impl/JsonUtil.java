/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.MapException;
import org.meeuw.mapping.Mapper;
import static org.meeuw.mapping.impl.Util.getAnnotation;

/**
 * Mapping supports also picking up fields from fields that contain json.
 * This class contains utilities related to that.
 *
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Slf4j
public class JsonUtil {



    private JsonUtil() {
        // no instances
    }
    /**
     * Lenient json mapper
     */
    static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    private static final Configuration JSONPATH_CONFIGURATION = Configuration.builder()
        .mappingProvider(new JacksonMappingProvider(MAPPER))
        .jsonProvider(new JacksonJsonNodeJsonProvider(MAPPER))

        .build();

    static Optional<Object> getSourceValueFromJson(Object source, Field destination, List<String> path, Class<?>... groups) {
        EffectiveSource annotation = getAnnotation(source.getClass(), destination, groups).orElseThrow();
        String field = annotation.field();
        if ("".equals(field)) {
            field = destination.getName();
        } 
        Field sourceField = Util.getSourceField(source.getClass(), field).orElseThrow();
        log.debug("Found source field {}", sourceField);

        return getSourceJsonValue(annotation, source, sourceField, destination);

    }

    public static Optional<Object> getSourceJsonValue(EffectiveSource annotation, Object source, Field sourceField, Field destination) {
           if (!"".equals(annotation.jsonPath())) {
            if (! "".equals(annotation.jsonPointer())) {
                throw new IllegalStateException();
            }
            return getSourceJsonValueByPath(source, sourceField, annotation.path(), annotation.jsonPath())
                .map(o -> unwrapJsonIfPossible(o, destination));
        } else {
            return getSourceJsonValueByPointer(source, sourceField, annotation.path(), annotation.jsonPointer())
                .map(o -> unwrapJsonIfPossible(o, destination));
        }
    }


    private static Optional<Object> getSourceJsonValueByPointer(Object source, Field sourceField, String[] path, String pointer) {

         return getSourceJsonValue(source, sourceField, path)
             .map(jn -> {
                 JsonNode at = jn.at(pointer);

                 return at;
             })
             .map(JsonUtil::unwrapJson);
    }

    // jsonpath would have its own cache, but it may be used by other
    // stuff. Since we know that there is a limited number of JsonPath object caused by us, we just use our hown cache, without any limitations.
    private static final Map<String, JsonPath> JSONPATH_CACHE = new ConcurrentHashMap<>();
    private static Optional<Object> getSourceJsonValueByPath(Object source, Field sourceField, String[] path, String jsonPath) {

         return getSourceJsonValue(source, sourceField, path)
             .map(jn -> getByJsonPath(jn, jsonPath))
             .map(JsonUtil::unwrapJson);
    }

    private static JsonNode getByJsonPath(JsonNode jn, String jsonPath) {
        try {
            return JsonPath.using(JSONPATH_CONFIGURATION).parse(jn).read(JSONPATH_CACHE.computeIfAbsent(jsonPath,
                JsonPath::compile));
        } catch (PathNotFoundException pathNotFoundException) {
            log.debug(pathNotFoundException.getMessage());
            return MAPPER.nullNode();
        }
    }

    static Object unwrapJsonIfPossible(Object json, Field destination) {
        if (json instanceof ObjectNode) {
            
        } else if (json instanceof List<?> list) {
            if (destination.getType() == List.class) {
                ParameterizedType genericType = (ParameterizedType) destination.getGenericType();
                Class<?> genericClass = (Class<?>) genericType.getActualTypeArguments()[0];
                if (genericClass != Object.class) {
                    return list.stream()
                        .map(o -> {
                                try {
                                    return Mapper.CURRENT.get()
                                        .selfMap(o, genericClass);
                                } catch (MapException me) {
                                    log.warn(me.getMessage(), me);
                                    return null;
                                }
                            }
                        ).toList();
                } 
            }
            
        }
        return json;
        
    }

    static class Key {
        final Object object;

        Key(Object object) {
            this.object = object;
        }

        @Override
        public boolean equals(Object object){
            return object instanceof Key other && this.object == other.object;
        }
        @Override
        public int hashCode() {
            return object.hashCode();
        }
    }

    public static final ThreadLocal<Map<Key, JsonNode>> JSON_CACHE = ThreadLocal.withInitial(HashMap::new);


    public static void clearCache() {
        JSON_CACHE.get().clear();
    }

    static Optional<JsonNode> getSourceJsonValue(Object source, Field sourceField, String... path) {

        return Util.getSourceValue(source, sourceField, path)
            .map(json -> {
                Key k = new Key(json);
                return  JSON_CACHE.get().computeIfAbsent(k, (key) -> {
                    try {
                        if (json instanceof byte[] bytes) {
                            return MAPPER.readTree(bytes);
                        } else if (json instanceof String string) {
                            return MAPPER.readTree(string);
                        } else if (json instanceof JsonNode n) {
                            return n;
                        } else {
                            throw new IllegalStateException("%s could not be mapped to json %s -> %s".formatted(sourceField, json, json));
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                });
            });
   }


   public static Function<Object, Optional<Object>> valueFromJsonGetter(EffectiveSource s) {
       UnaryOperator<JsonNode> withField = UnaryOperator.identity();
       if (!"".equals(s.field())) {
           withField = o -> o.get(s.field());
       }
       for (String p : s.path()) {
           final UnaryOperator<JsonNode> prev = withField;
           withField = o -> prev.apply(o).get(p);
       }
       final UnaryOperator<JsonNode> finalWithFieldAndPath = withField;
       if ("".equals(s.jsonPointer()) && "".equals(s.jsonPath())) {
           return o -> {
               JsonNode value = finalWithFieldAndPath.apply((JsonNode) o);
               return Optional.ofNullable(unwrapJson(value));
           };
       } else if (! "".equals(s.jsonPointer())) {
           return o -> {
               JsonNode value = finalWithFieldAndPath.apply((JsonNode) o);
               return Optional.ofNullable(unwrapJson(value.at(s.jsonPointer())));
           };
       } else {
            return o -> {
               JsonNode value = finalWithFieldAndPath.apply((JsonNode) o);
               return Optional.ofNullable(unwrapJson(
                   getByJsonPath(value, s.jsonPath())));
            };
       }
   }

    static Object unwrapJson(JsonNode jsonNode) {
        if (jsonNode.isMissingNode()) {
            log.debug("Missing node!");
            return null;
        }
        if (jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isObject())  {
            return jsonNode;
        }
        if (jsonNode.isLong()) {
            return jsonNode.asLong();
        }
        if (jsonNode.isInt()) {
            return jsonNode.asInt();
        }
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        if (jsonNode.isTextual()) {
            return jsonNode.asText();
        }
        if (jsonNode.isDouble()) {
            return jsonNode.asDouble();
        }
        if (jsonNode.isFloat()) {
            return jsonNode.asDouble();
        }
        if (jsonNode.isArray()) {
            List<Object> result = new ArrayList<>();
            jsonNode.forEach(e -> {
                result.add(unwrapJson(e));
            });
            return result;
        }
        if (jsonNode.isObject()) {
            return jsonNode;
        }
        // Not complete, I suppose
        return jsonNode.asText();

    }
}

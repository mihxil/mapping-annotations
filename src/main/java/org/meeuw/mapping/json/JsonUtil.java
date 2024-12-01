package org.meeuw.mapping.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.impl.Util;
import static org.meeuw.mapping.impl.Util.getAnnotation;

@Slf4j
public class JsonUtil {
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



    static Optional<Object> getSourceValue(Object source, Field destination) {
        Source annotation = getAnnotation(source.getClass(), destination).orElseThrow();

        Field f = Util.getSourceField(source.getClass(), annotation.field()).orElseThrow();
        return getSourceJsonValue(source, f, annotation.pointer());
    }


    public static Optional<Object> getSourceJsonValue(Object source, Field sourceField, String pointer) {

         return getSourceJsonValue(source, sourceField)
             .map(jn -> jn.at(pointer))
             .map(JsonUtil::unwrapJson);
    }


    static Optional<JsonNode> getSourceJsonValue(Object source, Field sourceField) {

        return Util.getSourceValue(source, sourceField)
            .map(json -> {
                JsonNode node;
                try {
                    if (json instanceof byte[] bytes) {
                        node = MAPPER.readTree(bytes);
                    } else if (json instanceof String string) {
                        node = MAPPER.readTree(string);
                    } else if (json instanceof JsonNode n) {
                        node = n;
                    } else {
                        throw new IllegalStateException("%s could not be mapped to json %s -> %s".formatted(sourceField, json, json));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException();
                }
                return node;
            });
   }

    static Object unwrapJson(JsonNode jsonNode) {
        if (jsonNode.isNull()) {
            return null;
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
        // Not complete, I suppose
        return jsonNode.asText();

    }
}

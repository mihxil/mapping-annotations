package org.meeuw.mapping;

import java.lang.reflect.Field;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import static org.meeuw.mapping.impl.Util.*;
import org.meeuw.mapping.json.JsonUtil;

@Slf4j
public class Mapper {

    private Mapper() {
        // no instances
    }


    public static void map(Object source, Object destination) {

        for (Field f: destination.getClass().getDeclaredFields()) {

            f.setAccessible(true);
            Optional<Source> annotation = getAnnotation(source.getClass(), f);
            annotation.ifPresent(s -> {
                String sourceFieldName = s.field();
                if ("".equals(sourceFieldName)) {
                    sourceFieldName = f.getName();
                }
                Optional<Field> sourceField = getSourceField(source.getClass(), sourceFieldName);
                sourceField.ifPresent(sf -> {
                    Optional<Object> value;
                    if ("".equals(s.pointer())) {
                        value = getSourceValue(source, sf);
                    } else {
                        value = JsonUtil.getSourceJsonValue(source, sf, s.pointer());
                    }
                    value.ifPresent(v -> {
                        try {
                            f.set(destination, v);
                        } catch (IllegalAccessException e) {
                            log.warn(e.getMessage(), e);
                        }
                    });
                });
            });

        }

    }
}

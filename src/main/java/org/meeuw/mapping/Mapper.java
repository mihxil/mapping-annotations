package org.meeuw.mapping;

import java.lang.reflect.Field;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.meeuw.mapping.annotations.Source;
import org.meeuw.mapping.impl.Util;
import org.meeuw.mapping.json.JsonUtil;

@Slf4j
public class Mapper {

    public static void map(Object source, Object destination) {

        for (Field f: destination.getClass().getDeclaredFields()) {

            Optional<Source> annotation = org.meeuw.mapping.impl.Util.getAnnotation(source.getClass(), f);
            annotation.ifPresent(s -> {

                Optional<Field> sourceField = Util.getSourceField(source.getClass(), s.field());
                sourceField.ifPresent(sf -> {
                    Optional<Object> value;
                    if ("".equals(s.pointer())) {
                        value = Util.getSourceValue(source, sf);
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

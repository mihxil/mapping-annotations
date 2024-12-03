/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.annotations;

import java.lang.annotation.*;

/**
 * This annotation can be put on a field of some destination object, to indicate
 * from which other object's field it must come.
 *
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE_PARAMETER})
@Repeatable(Sources.class)
public @interface Source {

    /**
     * Json path inside the other field. If not specified, it may be the entire field
     * Requires jways
     * @since 0.2
     */
    String jsonPath() default "";

    /**
     * Json path inside the other field. If not specified, it may be the entire field
     * @since 0.2
     */
    String jsonPointer() default "";


    /**
     * Name of the field in the source class, if not specified this means either
     * 1. The source field has the same name as the name of the field in the destination
     * 2. The source has become some json object and is not any more associated with a concrete field
     * (an example to explain this would probably be welcome)
     */
    String field() default "";


    /**
     * 'Deeper' values can be addresses via a path of more fields.
     */
    String[] path() default {};

    /**
     * The source class in with the other field can be found
     * <p>
     * Optional, normally just in the source class of the source object
     * But multiple source annotations can be present, and the one where the sourceClass
     * matches the actual class of the source object will be used then.
     */
    Class<?> sourceClass() default Object.class;


    /**
     * Similar to 'groups' in jakarta.validation. The idea is that certain mappings can be disabled/enabled
     * but specifying groups.
     *
     * EXPERIMENTAL
     * @since 0.2
     *
     */

    Class<?>[] groups() default {};

}

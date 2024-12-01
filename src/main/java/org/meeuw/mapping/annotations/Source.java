/*
 * Copyright (C) 2024 Licensed under the Apache License, Version 2.0
 */
package org.meeuw.mapping.annotations;

import java.lang.annotation.*;
/**
 * This annotation can be put on a field of some destination object, to indicate
 * from which other object's field it must come.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(Sources.class)
public @interface Source {

    /**
     * Json pointer inside the other field. If not specified, it may be the entire field
     */
    String pointer() default "";

    /**
     * Name of the field in the source class
     */
    String field() default "";

    /**
     * The source class in with the other field can be found
     * <p>
     * Optional, normally just in the source class of the source object
     * But multiple source annotations can be present, and the one where the sourceClass
     * matches the actual class of the source object will be used then.
     */
    Class<?> sourceClass() default Object.class;
}

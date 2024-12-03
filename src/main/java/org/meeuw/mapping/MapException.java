package org.meeuw.mapping;

/**
 * May get thrown by {@link Mapper}, currently only wrapping {@link ReflectiveOperationException}s.
 * @since 0.2
 */
public class MapException extends RuntimeException {

    /**
     * Constructor
     * @param e cause
     */
    public MapException(ReflectiveOperationException e) {
        super(e);
    }
}

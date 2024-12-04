/**
 * See {@link org.meeuw.mapping.Mapper}
 */
module org.meeuw.mapping.annotations {
    requires static lombok;
    requires transitive org.slf4j;

    requires transitive com.fasterxml.jackson.databind;
    requires json.path;

    exports org.meeuw.mapping.annotations;
    exports org.meeuw.mapping;
}

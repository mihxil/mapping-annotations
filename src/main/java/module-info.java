module org.meeuw.mapping.annotations {
    requires static lombok;
    requires org.slf4j;

    requires transitive com.fasterxml.jackson.databind;

    exports org.meeuw.mapping.annotations;
    exports org.meeuw.mapping;

}
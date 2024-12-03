module org.meeuw.mapping.annotations {
    requires static lombok;
    requires org.slf4j;

    requires transitive com.fasterxml.jackson.databind;
    requires java.desktop;
    requires json.path;

    exports org.meeuw.mapping.annotations;
    exports org.meeuw.mapping;

}
module org.meeuw.mapping.annotations {
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports org.meeuw.mapping.annotations;
    exports org.meeuw.mapping;
    exports org.meeuw.mapping.json;

}
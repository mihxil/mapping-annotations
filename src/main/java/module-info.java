module org.meeuw.annotations {
    requires static lombok;

    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports org.meeuw.mapping.json;
    exports org.meeuw.mapping.impl;
    exports org.meeuw.mapping.annotations;

}
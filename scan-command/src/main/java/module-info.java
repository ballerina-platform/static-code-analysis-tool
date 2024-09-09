module io.ballerina.scan{
    uses io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;
    requires io.ballerina.cli;
    requires io.ballerina.lang;
    requires io.ballerina.parser;
    requires io.ballerina.tools.api;
    requires io.ballerina.toml;
    requires info.picocli;
    requires com.google.gson;
    requires org.apache.commons.io;

    exports io.ballerina.scan;
}

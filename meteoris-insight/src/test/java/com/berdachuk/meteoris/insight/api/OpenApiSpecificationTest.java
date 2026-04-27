package com.berdachuk.meteoris.insight.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiSpecificationTest {

    @Test
    void openapiYamlParsesAsYaml() throws Exception {
        Path spec = Path.of("api/openapi.yaml");
        assertThat(Files.exists(spec)).isTrue();
        Object parsed = new Yaml().load(Files.readString(spec));
        assertThat(parsed).isNotNull();
    }

    @Test
    void openapiYamlIsValidOpenApi3() {
        Path spec = Path.of("api/openapi.yaml").toAbsolutePath();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(spec.toString(), null, options);
        assertThat(result.getMessages()).as("parse messages: %s", result.getMessages()).isEmpty();
        assertThat(result.getOpenAPI()).isNotNull();
        assertThat(result.getOpenAPI().getOpenapi()).startsWith("3.");
    }
}

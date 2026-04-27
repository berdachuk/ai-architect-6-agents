# Modulith + Spring AI Tool Boundaries (Meteoris Insight)

## Description

How to prevent Spring AI `@Tool` annotations from leaking Modulith module boundaries.

## When to use

When adding `@Tool` methods to classes in feature modules (weather, news, memory).

## Instructions

- `@Tool` is **NOT** `@Inherited` — placing it on an interface or superclass does **not** propagate to implementations.
- Do **NOT** put `@Component` on tool classes in feature modules — this would cause Spring component scanning to pick them up inside the module, violating the Modulith boundary (internal packages should remain internal).
- Create **plain Java tool classes** in feature modules (no Spring annotations):
  - Only constructor / field injection of module-local services if needed.
  - Keep the class `public` so the agent module can instantiate it.
- Register them as **@Bean factory methods** in `agent.LiveAgentConfiguration` (or a dedicated `@Configuration` class inside the `app-agent-core` module):
  - The factory method constructs the plain tool class and exposes it as a Spring bean.
  - Only the agent module imports `org.springframework.ai.tool.annotation.Tool`.
- This pattern keeps feature modules free of Spring AI dependencies and prevents accidental cross-module component scanning.

## Example

```java
// Inside app-weather-agent (plain Java, no Spring annotations)
package meteoris.weather.agent.tools;

public class WeatherTools {
    private final WeatherService weatherService;

    public WeatherTools(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    public String fetchCurrentWeather(String city) {
        return weatherService.getCurrent(city);
    }
}
```

```java
// Inside app-agent-core (Spring configuration)
package meteoris.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveAgentConfiguration {

    @Bean
    public WeatherTools weatherTools(WeatherService weatherService) {
        return new WeatherTools(weatherService);
    }

    @Bean
    public Object weatherToolTarget(WeatherTools weatherTools) {
        // Spring AI ToolCallback or functional wrapper
        return weatherTools;
    }
}
```

## Boundaries

- Does **not** replace `mcp-integration` skill (MCP transport and `@Tool` wrappers for external APIs are documented there).
- Does **not** define LLM prompts or chat client configuration — use `agentic-patterns` for that.

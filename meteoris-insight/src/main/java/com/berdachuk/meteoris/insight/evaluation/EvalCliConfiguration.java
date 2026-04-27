package com.berdachuk.meteoris.insight.evaluation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("eval-cli")
public class EvalCliConfiguration {

    @Bean
    ApplicationRunner evalCliRunner(
            EvaluationService evaluationService,
            ApplicationArguments applicationArguments,
            ConfigurableApplicationContext applicationContext,
            @Value("${meteoris.eval.cli.exit-jvm:true}") boolean exitJvm,
            @Value("${meteoris.eval.cli.skip-exit:false}") boolean skipExit) {
        return args -> {
            String dataset =
                    applicationArguments.containsOption("meteoris.eval.dataset")
                            ? applicationArguments.getOptionValues("meteoris.eval.dataset").getFirst()
                            : "meteoris-eval-v1";
            String profile =
                    applicationArguments.containsOption("meteoris.eval.profile")
                            ? applicationArguments.getOptionValues("meteoris.eval.profile").getFirst()
                            : "stub-ai";
            String report = evaluationService.run(dataset, profile);
            System.out.println(report);
            if (!skipExit) {
                int code = SpringApplication.exit(applicationContext, () -> 0);
                if (exitJvm) {
                    System.exit(code);
                }
            }
        };
    }
}

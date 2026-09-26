package com.backendprinciple.playground.lab;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.lab")
public record LabProperties(@Positive int maxProjectsPerUser,
                            @Positive int maxFilesPerProject,
                            @Positive int maxFileBytes,
                            @Positive long maxTotalBytes) {
}

package com.ruici.ai.common.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.structured")
public class StructuredOutputProperties {

    private int maxAttempts = 2;
    private boolean includeLastError = true;
    private boolean retryUseRepairPrompt = true;
    private boolean retryAppendStrictJsonInstruction = true;
    private int errorMessageMaxLength = 200;
    private boolean metricsEnabled = true;
}

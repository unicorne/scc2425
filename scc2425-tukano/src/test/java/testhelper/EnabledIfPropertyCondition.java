package testhelper;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import utils.ResourceUtils;

import java.util.Properties;

public class EnabledIfPropertyCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
         EnabledIfProperty annotation = context.getElement()
                .map(e -> e.getAnnotation(EnabledIfProperty.class))
                .orElse(null);

        if (annotation == null) {
            return ConditionEvaluationResult.enabled("No condition found, enabling test");
        }

        String property = annotation.property();
        String expectedValue = annotation.value();
        String filePath = annotation.file();

        Properties properties = new Properties();
        ResourceUtils.loadPropertiesFromResources(properties, filePath);
        String actualValue = properties.getProperty(property);
        if (expectedValue.equals(actualValue)) {
            return ConditionEvaluationResult.enabled("Property matches expected value");
        } else {
            return ConditionEvaluationResult.disabled("Property does not match expected value");
        }
    }
}


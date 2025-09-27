package com.platform.shared.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect for validating input parameters to prevent injection attacks.
 * Automatically validates method parameters annotated with @ValidateInput.
 */
@Aspect
@Component
public class InputValidationAspect {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationAspect.class);

    @Autowired
    private SqlInjectionValidator sqlInjectionValidator;

    @Around("@annotation(validateInput)")
    public Object validateInput(ProceedingJoinPoint joinPoint, ValidateInput validateInput) throws Throwable {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg != null) {
                validateObject(arg, validateInput);
            }
        }

        return joinPoint.proceed();
    }

    private void validateObject(Object obj, ValidateInput validateInput) throws IllegalArgumentException {
        if (obj instanceof String) {
            validateStringInput((String) obj, "parameter", validateInput);
        } else {
            // Validate fields of complex objects
            validateObjectFields(obj, validateInput);
        }
    }

    private void validateObjectFields(Object obj, ValidateInput validateInput) throws IllegalArgumentException {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(obj);
                if (fieldValue instanceof String) {
                    validateStringInput((String) fieldValue, field.getName(), validateInput);
                }
            } catch (IllegalAccessException e) {
                logger.warn("Could not access field {} for validation", field.getName());
            }
        }
    }

    private void validateStringInput(String input, String fieldName, ValidateInput validateInput)
            throws IllegalArgumentException {

        if (input == null || input.trim().isEmpty()) {
            return; // Skip validation for null/empty inputs
        }

        // Check for SQL injection patterns
        if (validateInput.checkSqlInjection()) {
            sqlInjectionValidator.validateInput(input, fieldName);
        }

        // Check for XSS patterns
        if (validateInput.checkXss()) {
            validateXssInput(input, fieldName);
        }

        // Check maximum length
        if (validateInput.maxLength() > 0 && input.length() > validateInput.maxLength()) {
            throw new IllegalArgumentException(
                String.format("Field '%s' exceeds maximum length of %d characters",
                    fieldName, validateInput.maxLength())
            );
        }

        // Check against allowed patterns
        if (!validateInput.allowedPattern().isEmpty()) {
            if (!input.matches(validateInput.allowedPattern())) {
                throw new IllegalArgumentException(
                    String.format("Field '%s' contains invalid characters", fieldName)
                );
            }
        }
    }

    private void validateXssInput(String input, String fieldName) throws IllegalArgumentException {
        // Basic XSS pattern detection
        String[] xssPatterns = {
            "<script", "</script>", "javascript:", "vbscript:", "onload=", "onerror=",
            "onclick=", "onmouseover=", "onfocus=", "eval(", "expression("
        };

        String lowercaseInput = input.toLowerCase();
        for (String pattern : xssPatterns) {
            if (lowercaseInput.contains(pattern)) {
                throw new IllegalArgumentException(
                    String.format("Field '%s' contains potentially malicious content", fieldName)
                );
            }
        }
    }

    /**
     * Annotation for marking methods that require input validation.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValidateInput {

        /**
         * Check for SQL injection patterns
         */
        boolean checkSqlInjection() default true;

        /**
         * Check for XSS patterns
         */
        boolean checkXss() default true;

        /**
         * Maximum allowed length for string inputs
         */
        int maxLength() default 0;

        /**
         * Regex pattern that input must match
         */
        String allowedPattern() default "";
    }
}
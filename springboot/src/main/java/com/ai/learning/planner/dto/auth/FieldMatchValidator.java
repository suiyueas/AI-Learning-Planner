package com.ai.learning.planner.dto.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String firstFieldName;
    private String secondFieldName;
    private String message;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.firstFieldName = constraintAnnotation.first();
        this.secondFieldName = constraintAnnotation.second();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field firstField = ReflectionUtils.findField(value.getClass(), firstFieldName);
            Field secondField = ReflectionUtils.findField(value.getClass(), secondFieldName);
            if (firstField == null || secondField == null) {
                return true;
            }
            firstField.setAccessible(true);
            secondField.setAccessible(true);
            Object firstValue = firstField.get(value);
            Object secondValue = secondField.get(value);
            boolean isValid = firstValue == null && secondValue == null
                    || firstValue != null && firstValue.equals(secondValue);
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(secondFieldName)
                        .addConstraintViolation();
            }
            return isValid;
        } catch (Exception e) {
            return true;
        }
    }
}
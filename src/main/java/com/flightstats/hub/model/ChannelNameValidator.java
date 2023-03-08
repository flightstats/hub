package com.flightstats.hub.model;

import com.flightstats.hub.constant.ContentConstant;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ChannelNameValidator implements ConstraintValidator<ChannelName, String> {
    @Override
    public void initialize(ChannelName constraintAnnotation) {
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.matches(ContentConstant.VALID_NAME);
    }

}

package com.flightstats.hub.model;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({ METHOD, FIELD, PARAMETER})
@Constraint(validatedBy = ChannelNameValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChannelName {
    String message() default "must be a valid alphanumeric name. found: ${validatedValue}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

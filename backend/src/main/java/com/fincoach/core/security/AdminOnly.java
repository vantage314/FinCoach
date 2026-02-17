package com.fincoach.core.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminOnly {
    /**
     * Reserved for future permission codes (e.g., "PORTFOLIO_DEBUG").
     */
    String value() default "";
}

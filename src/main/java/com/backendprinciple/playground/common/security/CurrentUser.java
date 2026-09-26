package com.backendprinciple.playground.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on a controller parameter of type {@link AuthUser} to receive the logged-in user:
 * {@code public X get(@CurrentUser AuthUser me)}. Resolved by {@link CurrentUserArgumentResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

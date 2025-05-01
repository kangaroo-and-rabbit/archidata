package org.atriasoft.archidata.interceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.ValidGroup;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

/**
 * A JAX-RS {@link ReaderInterceptor} that performs conditional validation
 * on deserialized request bodies using custom validation groups defined by {@link ValidGroup}.
 * <p>
 * This interceptor supplements or overrides the behavior of {@code @Valid} by allowing
 * explicit group-based validation logic via an annotation placed on resource method parameters.
 * <p>
 * The interceptor runs after JSON deserialization and uses a {@link Validator}
 * to validate the entity.
 * <p>
 * If any constraint violations are found, a {@link ConstraintViolationException} is thrown,
 * which can be handled by an {@code ExceptionMapper} to return a suitable HTTP response.
 */
@Provider
public class ValidGroupInterceptor implements ReaderInterceptor {
	/**
	 * The Bean Validation validator used to perform constraint checks.
	 */
	@Inject
	private Validator validator;

	/**
	 * Intercepts the request after JSON deserialization and performs validation using
	 * groups specified by the {@link ValidGroup} annotation.
	 *
	 * @param context the interceptor context containing the deserialized entity and annotations
	 * @return the validated entity if no constraint violations are found
	 * @throws IOException if an I/O error occurs
	 * @throws WebApplicationException if constraint violations are detected
	 */
	@Override
	public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
		final Object entity = context.proceed();
		final ValidGroup validGroup = extractValidationGroups(context);
		if (validGroup == null) {
			return entity;
		}
		final Set<ConstraintViolation<Object>> violations = this.validator.validate(entity, validGroup.groups());
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException("Validation error", violations);
		}
		return entity;
	}

	/**
	 * Extracts the {@link ValidGroup} annotation from the resource method parameter annotations.
	 * <p>
	 * This determines whether a group-based validation should be applied to the request body.
	 *
	 * @param context the interceptor context containing parameter annotations
	 * @return the {@link ValidGroup} annotation if present, or {@code null} if not
	 */
	private ValidGroup extractValidationGroups(final ReaderInterceptorContext context) {
		// Analyse de la méthode ciblée
		for (final Annotation annotation : context.getAnnotations()) {
			if (annotation instanceof final ValidGroup validGroup) {
				return validGroup;
			}
		}
		// fallback
		return null;
	}
}

package org.atriasoft.archidata.checker;

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

@Provider
public class ValidGroupInterceptor implements ReaderInterceptor {
	@Inject
	private Validator validator;

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

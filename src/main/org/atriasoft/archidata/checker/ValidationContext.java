package org.atriasoft.archidata.checker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class ValidationContext implements AutoCloseable {

	private static final ThreadLocal<ValidationContext> context = new ThreadLocal<>();

	private final Map<String, Object> data = new HashMap<>();
	private final ValidatorFactory factory;
	private final Validator validator;

	private ValidationContext(final boolean owner) {
		this.factory = Validation.buildDefaultValidatorFactory();
		this.validator = this.factory.getValidator();
	}

	public static ValidationContext open() {
		final ValidationContext ctx = new ValidationContext(true);
		context.set(ctx);
		return ctx;
	}

	public static ValidationContext current() {
		return context.get();
	}

	public static boolean exists() {
		return context.get() != null;
	}

	public ValidationContext put(final String key, final Object value) {
		this.data.put(key, value);
		return this;
	}

	public Object get(final String key) {
		return this.data.get(key);
	}

	@SuppressWarnings("unchecked")
	public <TYPE_RETURN> TYPE_RETURN get(final String key, final Class<?> clazz) {
		final Object value = this.data.get(key);
		if (value == null) {
			return null;
		}
		if (clazz.isInstance(value)) {
			return (TYPE_RETURN) clazz.cast(value);
		}
		throw new RuntimeException("ValidationContext: value for key '%s' is not of type %s, but %s".formatted(key,
				clazz.getSimpleName(), value.getClass().getSimpleName()));
	}

	public boolean contains(final String key) {
		return this.data.containsKey(key);
	}

	public <TYPE_CLASS_VALIDATE> Set<ConstraintViolation<TYPE_CLASS_VALIDATE>> validate(
			final TYPE_CLASS_VALIDATE data,
			final Class<?>... groups) {
		if (data == null) {
			return Set.of();
		}
		return this.validator.validate(data, groups);
	}

	public <TYPE_CLASS_VALIDATE> void validateThrow(final TYPE_CLASS_VALIDATE data, final Class<?>... groups) {
		final Set<ConstraintViolation<TYPE_CLASS_VALIDATE>> violations = this.validate(data, groups);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException("Validation failed for " + data.getClass().getCanonicalName(),
					violations);
		}
	}

	@Override
	public void close() {
		context.remove();
		this.factory.close();
	}
}
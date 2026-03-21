package org.atriasoft.archidata.checker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Thread-local validation context that provides Jakarta Bean Validation support
 * along with a key-value data store for sharing contextual information.
 *
 * <p>
 * Use {@link #open()} to create and bind a context to the current thread, and
 * {@link #close()} to release it. The context implements {@link AutoCloseable}
 * for use with try-with-resources.
 * </p>
 */
public class ValidationContext implements AutoCloseable {

	private static final ThreadLocal<ValidationContext> context = new ThreadLocal<>();

	private final Map<String, Object> data = new HashMap<>();
	private final ValidatorFactory factory;
	private final Validator validator;

	private ValidationContext(final boolean owner) {
		this.factory = Validation.buildDefaultValidatorFactory();
		this.validator = this.factory.getValidator();
	}

	/**
	 * Opens a new validation context and binds it to the current thread.
	 *
	 * @return the newly created validation context
	 */
	public static ValidationContext open() {
		final ValidationContext ctx = new ValidationContext(true);
		context.set(ctx);
		return ctx;
	}

	/**
	 * Returns the validation context bound to the current thread.
	 *
	 * @return the current context, or {@code null} if none is bound
	 */
	public static ValidationContext current() {
		return context.get();
	}

	/**
	 * Checks whether a validation context exists for the current thread.
	 *
	 * @return {@code true} if a context is bound to the current thread
	 */
	public static boolean exists() {
		return context.get() != null;
	}

	/**
	 * Stores a key-value pair in this context.
	 *
	 * @param key the key to store
	 * @param value the value to associate with the key
	 * @return this context for method chaining
	 */
	public ValidationContext put(final String key, final Object value) {
		this.data.put(key, value);
		return this;
	}

	/**
	 * Retrieves a value by key from this context.
	 *
	 * @param key the key to look up
	 * @return the associated value, or {@code null} if not found
	 */
	public Object get(final String key) {
		return this.data.get(key);
	}

	/**
	 * Retrieves a typed value by key from this context.
	 *
	 * @param <TYPE_RETURN> the expected return type
	 * @param key the key to look up
	 * @param clazz the expected class of the value
	 * @return the value cast to the expected type, or {@code null} if not found
	 * @throws RuntimeException if the value is not of the expected type
	 */
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

	/**
	 * Checks whether this context contains a value for the given key.
	 *
	 * @param key the key to check
	 * @return {@code true} if the key exists in this context
	 */
	public boolean contains(final String key) {
		return this.data.containsKey(key);
	}

	/**
	 * Validates the given data object against the specified validation groups.
	 *
	 * @param <TYPE_CLASS_VALIDATE> the type of the object to validate
	 * @param data the object to validate
	 * @param groups the validation groups to apply
	 * @return the set of constraint violations, or an empty set if valid or data is null
	 */
	public <TYPE_CLASS_VALIDATE> Set<ConstraintViolation<TYPE_CLASS_VALIDATE>> validate(
			final TYPE_CLASS_VALIDATE data,
			final Class<?>... groups) {
		if (data == null) {
			return Set.of();
		}
		return this.validator.validate(data, groups);
	}

	/**
	 * Validates the given data object and throws an exception if violations are found.
	 *
	 * @param <TYPE_CLASS_VALIDATE> the type of the object to validate
	 * @param data the object to validate
	 * @param groups the validation groups to apply
	 * @throws ConstraintViolationException if any constraint violations are detected
	 */
	public <TYPE_CLASS_VALIDATE> void validateThrow(final TYPE_CLASS_VALIDATE data, final Class<?>... groups) {
		final Set<ConstraintViolation<TYPE_CLASS_VALIDATE>> violations = this.validate(data, groups);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException("Validation failed for " + data.getClass().getCanonicalName(),
					violations);
		}
	}

	/**
	 * Closes this context, removing it from the current thread and releasing the validator factory.
	 */
	@Override
	public void close() {
		context.remove();
		this.factory.close();
	}
}

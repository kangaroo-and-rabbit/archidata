package org.atriasoft.archidata.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.ValidGroup;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Proxy class that validates method arguments annotated with Hibernate Validator constraints.
 */
public class ValidationProxy implements InvocationHandler {

	private final Object target;
	private final Validator validator;

	private ValidationProxy(final Object target) {
		this.target = target;
		final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = factory.getValidator();
	}

	/**
	 * Creates a proxy instance that automatically applies validation.
	 *
	 * @param target the target object implementing an interface
	 * @return a proxied instance
	 */
	@SuppressWarnings("unchecked")
	public static <TYPE_OBJECT, TYPE_INTERFACE> TYPE_INTERFACE create(
			final Class<TYPE_INTERFACE> interfaceClass,
			final TYPE_OBJECT target) {
		return (TYPE_INTERFACE) Proxy.newProxyInstance(target.getClass().getClassLoader(),
				new Class<?>[] { interfaceClass }, new ValidationProxy(target));
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		if (args != null) {
			final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
			for (int i = 0; i < args.length; i++) {
				final Object arg = args[i];
				if (arg != null) {
					final Set<ConstraintViolation<Object>> violations = new HashSet<>();
					for (final Annotation annotation : parameterAnnotations[i]) {
						if (annotation.annotationType() == Valid.class) {
							violations.addAll(this.validator.validate(arg));
						}
						if (annotation instanceof final ValidGroup validGroup) {
							violations.addAll(this.validator.validate(arg, validGroup.value()));
						}
					}
					if (!violations.isEmpty()) {
						throw new ConstraintViolationException(
								"Validation failed for " + arg.getClass().getCanonicalName(), violations);
					}
				}
			}
		}
		return method.invoke(this.target, args);
	}
}

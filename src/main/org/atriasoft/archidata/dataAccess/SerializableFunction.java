package org.atriasoft.archidata.dataAccess;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A {@link Function} that is also {@link Serializable}, enabling extraction of method
 * reference metadata via {@link java.lang.invoke.SerializedLambda}.
 *
 * <p>Used by {@link Filters} to resolve getter method references
 * (e.g. {@code User::getName}) to database field names.
 *
 * @param <T> the input type (entity class)
 * @param <R> the return type (property type)
 */
@FunctionalInterface
public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {}

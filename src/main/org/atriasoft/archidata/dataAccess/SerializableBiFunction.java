package org.atriasoft.archidata.dataAccess;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * A {@link BiFunction} that is also {@link Serializable}, enabling extraction of method
 * reference metadata via {@link java.lang.invoke.SerializedLambda}.
 *
 * <p>Used by {@link Filters} to resolve fluent setter method references
 * (e.g. {@code User::setName} where setName returns the entity for chaining)
 * to database field names.
 *
 * @param <T> the input type (entity class)
 * @param <V> the value type (property type)
 * @param <R> the return type (typically the entity class for fluent/builder-pattern setters)
 */
@FunctionalInterface
public interface SerializableBiFunction<T, V, R> extends BiFunction<T, V, R>, Serializable {}

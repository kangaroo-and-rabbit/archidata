package org.atriasoft.archidata.dataAccess;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * A {@link BiConsumer} that is also {@link Serializable}, enabling extraction of method
 * reference metadata via {@link java.lang.invoke.SerializedLambda}.
 *
 * <p>Used by {@link Filters} to resolve setter method references
 * (e.g. {@code User::setName}) to database field names.
 *
 * @param <T> the input type (entity class)
 * @param <V> the value type (property type)
 */
@FunctionalInterface
public interface SerializableBiConsumer<T, V> extends BiConsumer<T, V>, Serializable {}

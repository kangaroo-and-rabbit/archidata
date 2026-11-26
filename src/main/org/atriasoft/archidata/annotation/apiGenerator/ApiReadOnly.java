package org.atriasoft.archidata.annotation.apiGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or field as read-only in the generated API.
 *
 * <p>When applied, the annotated element will not be included in create or update
 * operations of the generated client code.
 *
 * <p>It also has effects on the server: a property sent by a client in a request body is ignored
 * (dropped by the JSON provider that {@code ContextGenericTools.addJsr310} registers; records and
 * {@code @JsonCreator} constructors are not filtered). Server code can still write it with an insert,
 * whereas an update skips it unless the {@code ForceReadOnlyField} option is passed.
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiReadOnly {}
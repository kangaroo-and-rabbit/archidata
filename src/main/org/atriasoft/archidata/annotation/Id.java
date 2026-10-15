package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the primary key of an entity.
 *
 * <p>archidata generates the key at insertion for an {@code ObjectId} ({@code new ObjectId()}), a
 * {@code Long} (sequence of the collection in the {@code counters} collection) or a {@code UUID}
 * ({@code UuidUtils.nextUUID()}). A key of another type ({@code String}, {@code Integer}...) is set
 * by the application and inserted with the {@code DirectPrimaryKey} option. The base classes
 * ({@code OIDGenericData}, {@code GenericData}, {@code UUIDGenericData} and their soft-delete
 * variants) already declare it; an entity declares at most one.
 *
 * <p>Example:
 * {@snippet lang = java :
 * public class Country {
 * 	@Id
 * 	@Field("_id")
 * 	public String code;
 * 	public String name;
 * }
 * }
 *
 * <p>Combine it with {@code @Field("_id")} to store the key in the MongoDB {@code _id} field (as
 * {@code OIDGenericData} does). Replaces {@code jakarta.persistence.Id}, which archidata no longer
 * reads.
 *
 * <p>Read on the Java field only (on a record, put it on the component, which propagates it to the
 * field): the target is therefore restricted to fields.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {}

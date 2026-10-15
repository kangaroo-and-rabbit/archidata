package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Name of the MongoDB collection that stores the entities of the annotated class.
 *
 * <p>Without this annotation the collection is named after the simple name of the class
 * ({@code Class.getSimpleName()}, e.g. {@code Media} for {@code com.example.Media}). The
 * annotation is read on the class itself only: a subclass does not inherit the collection of its
 * parent. A query can still target another collection with the {@code OverrideTableName} option.
 *
 * <p>Example:
 * {@snippet lang = java :
 * @Collection("media")
 * public class Media extends OIDGenericDataSoftDelete {
 * 	public String name;
 * }
 * }
 *
 * <p>Replaces {@code jakarta.persistence.Table(name = "...")}, which archidata no longer reads.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Collection {
	/**
	 * Name of the MongoDB collection.
	 *
	 * @return the collection name
	 */
	String value();
}

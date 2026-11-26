package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * In Document entity the relation is stored in the 2 part of the entity, then it
 * is needed to define the field that store the relation data value in the
 * remote elements.
 *
 * Technical note: this annotation is not needed for a unidirectional relation: use
 * {@link org.atriasoft.archidata.annotation.checker.CheckForeignKey} alone.
 *
 * <p>
 * Example:
 * <pre>{@code
 * public class ClassA {
 * 	@Id
 * 	ObjectId _id;
 * 	@ManyToManyDoc(targetEntity = ClassB.class, remoteField = "remoteFieldClassB")
 * 	List<@CheckForeignKey(target = ClassB.class) ObjectId> remoteFieldClassA;
 * }
 *
 * public class ClassB {
 * 	@Id
 * 	ObjectId _id;
 * 	@ManyToManyDoc(targetEntity = ClassA.class, remoteField = "remoteFieldClassA")
 * 	List<@CheckForeignKey(target = ClassA.class) ObjectId> remoteFieldClassB;
 * }
 * }</pre>
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface ManyToManyDoc {
	/**
	 * The entity class that is the target of the association.
	 * @return the target entity class
	 */
	Class<?> targetEntity();

	/**
	 * The field name in the remote entity that holds the reverse reference.
	 * Empty if the relationship is unidirectional.
	 * @return the remote field name
	 */
	String remoteField();
}

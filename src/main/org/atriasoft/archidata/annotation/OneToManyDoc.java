package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * In Document entity the relation is stored in the 2 part of the entity, then
 * it is needed to define the field that store the relation data value in the
 * remote elements.
 *
 * Technical note: The use of this annotation when unidirectional if not needed,
 * just use @Ref CheckForeignKey
 *
 * <p>
 * Example 1:
 * {@snippet :
 * public class ClassWithParent {
 * 	&#64;Id
 * 	ObjectId _id;
 * 	&#64;CheckForeignKey(ClassWithChilds.class)
 * 	&#64;ManyToOneDoc(targetEntity = ClassWithChilds.class, remoteField = "roots")
 * 	ObjectId parent;
 * }
 *
 * public class ClassWithChilds {
 * 	&#64;Id
 * 	ObjectId _id;
 * 	&#64;OneToManyDoc(targetEntity = ClassWithParent.class, remoteField = "parent")
 * 	List<@CheckForeignKey(ClassWithParent.class) ObjectId> roots;
 * }
 * }
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface OneToManyDoc {
	/**
	 * Defines the cascade behavior applied to remote objects when the parent entity
	 * is updated or deleted.
	 */
	public enum CascadeMode {
		/** The remote object is deleted. */
		DELETE,
		/** The remote object parent field is set to {@code null}. */
		SET_NULL,
		/** The remote object is left unchanged. */
		IGNORE
	}

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

	/**
	 * Whether the system automatically adds the link on children when the parent is created.
	 * @return true if the link should be added on creation, true by default
	 */
	boolean addLinkWhenCreate() default true;

	/**
	 * The cascade behavior to apply on child entities when the parent is updated.
	 * @return the cascade mode for updates, defaults to {@link CascadeMode#IGNORE}
	 */
	CascadeMode cascadeUpdate() default CascadeMode.IGNORE;

	/**
	 * The cascade behavior to apply on child entities when the parent is deleted.
	 * @return the cascade mode for deletions, defaults to {@link CascadeMode#IGNORE}
	 */
	CascadeMode cascadeDelete() default CascadeMode.IGNORE;

}

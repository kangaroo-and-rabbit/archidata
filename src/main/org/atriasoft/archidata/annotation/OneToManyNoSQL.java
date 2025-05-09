package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * In NoSql entity the relation is stored in the 2 part of the entity,
 * then it is needed to define the field that store the relation data value in the remote elements.
 *
 * <p>Example 1:
 * {@snippet :
 * @Entity
 * public class ClassWithParent {
 *     @Id
 *     ObjectId _id;
 *     @ManyToOneNoSQL(targetEntity = ClassWithChilds.class, remoteField = "roots")
 *     ObjectId parent;
 * }
 *
 * @Entity
 * public class ClassWithChilds {
 *     @Id
 *     ObjectId _id;
 *     @OneToManyNoSQL(targetEntity = ClassWithParent.class, remoteField = "parent")
 *     List<ObjectId> roots;
 * }
 * }
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface OneToManyNoSQL {
	public enum CascadeMode {
		DELETE_ON_REMOVE, // The remote object is deleted
		SET_NULL_ON_REMOVE, // The remote object parent field is set to `null`
		IGNORE_ON_REMOVE // The remote object is unchanged
	}

	/**
	 * The entity class that is the target of the
	 * association.
	 */
	Class<?> targetEntity();

	/**
	 * The field that owns the revert value. empty if the relationship is unidirectional.
	 */
	String remoteField();

	/**
	 * When list change, apply some update on child.
	 */
	CascadeMode cascade() default CascadeMode.IGNORE_ON_REMOVE;

	/**
	 * When add an element ignore the remote update ==> this is the responsibility of the child to register here...
	 */
	boolean ignoreRemoteUpdateWhenOnAddItem() default true;
}

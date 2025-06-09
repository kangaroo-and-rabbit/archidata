package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.glassfish.jersey.Beta;

/**
 * In Document entity the relation is stored in the 2 part of the entity, then it
 * is needed to define the field that store the relation data value in the
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
	public enum CascadeMode {
		DELETE_ON_REMOVE, // The remote object is deleted
		SET_NULL_ON_REMOVE, // The remote object parent field is set to `null`
		IGNORE_ON_REMOVE // The remote object is unchanged
	}

	/**
	 * The entity class that is the target of the association.
	 */
	Class<?> targetEntity();

	/**
	 * The field remote name that owns the revert value. empty if the relationship
	 * is unidirectional.
	 */
	String remoteField();

	/**
	 * When list change, apply some update on child.
	 */
	@Beta
	CascadeMode cascade() default CascadeMode.IGNORE_ON_REMOVE;

	/**
	 * When add an element ignore the remote update ==> this is the responsibility
	 * of the child to register here...
	 */
	@Beta
	boolean ignoreRemoteUpdateWhenOnAddItem() default true;
}

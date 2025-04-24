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
		DELETE_ON_REMOVE, SET_NULL_ON_REMOVE, IGNORE_ON_REMOVE
	}

	/**
	 * The entity class that is the target of the
	 * association.
	 */
	Class<?> targetEntity();

	/**
	 * The field that owns the revert value. empty if the relationship is unidirectional.
	 */
	String remoteField() default "";

	CascadeMode cascade() default CascadeMode.DELETE_ON_REMOVE;
}

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
 * Technical note: The use of this annotation when unidirectional if not needed,
 * just use @Ref CheckForeignKey
 *
 * <p>
 * Example:
 * {@snippet :
 *
 * public class ClassA {
 * 	&#64;Id
 * 	ObjectId _id;
 * 	&#64;ManyToManyDoc(targetEntity = ClassB.class, remoteField = "remoteFieldClassB")
 * 	List<@CheckForeignKey(ClassB.class) ObjectId> remoteFieldClassA;
 * }
 *
 * public class ClassB {
 * 	&#64;Id
 * 	ObjectId _id;
 * 	&#64;OneToManyDoc(targetEntity = ClassA.class, remoteField = "remoteFieldClassA")
 * 	List<@CheckForeignKey(ClassA.class) ObjectId> remoteFieldClassB;
 * }
 * }
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface ManyToManyDoc {
	/**
	 * The entity class that is the target of the association.
	 */
	Class<?> targetEntity();

	/**
	 * The field remote name that owns the revert value. empty if the relationship
	 * is unidirectional.
	 */
	String remoteField();
}

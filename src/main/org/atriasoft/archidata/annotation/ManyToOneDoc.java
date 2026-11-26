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
 * Technical note: this annotation is not needed for a unidirectional relation: use
 * {@link org.atriasoft.archidata.annotation.checker.CheckForeignKey} alone.
 *
 * <p>
 * Example:
 * <pre>{@code
 * public class ClassWithParent {
 * 	@Id
 * 	ObjectId _id;
 * 	@CheckForeignKey(target = ClassWithChilds.class)
 * 	@ManyToOneDoc(targetEntity = ClassWithChilds.class, remoteField = "roots")
 * 	ObjectId parent;
 * }
 *
 * public class ClassWithChilds {
 * 	@Id
 * 	ObjectId _id;
 * 	@OneToManyDoc(targetEntity = ClassWithParent.class, remoteField = "parent")
 * 	List<@CheckForeignKey(target = ClassWithParent.class) ObjectId> roots;
 * }
 * }</pre>
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface ManyToOneDoc {
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
	 * Whether the system automatically adds the link on the parent when the child is created.
	 * @return true if the link should be added on creation, true by default
	 */
	boolean addLinkWhenCreate() default true;

	/**
	 * Whether the system automatically removes the link from the parent when the child is deleted.
	 * @return true if the link should be removed on deletion, true by default
	 */
	boolean removeLinkWhenDelete() default true;

	/**
	 * Whether the system automatically updates the link on the parent when the child is updated.
	 * @return true if the link should be updated, true by default
	 */
	boolean updateLinkWhenUpdate() default true;

}

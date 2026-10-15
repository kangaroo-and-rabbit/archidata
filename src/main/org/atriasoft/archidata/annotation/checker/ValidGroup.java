package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the validation groups to apply when validating a parameter or type.
 *
 * <p>This annotation allows selecting which validation groups should be activated
 * during constraint validation.
 *
 * <p>The TypeScript/Zod generator appends the group names, without their {@code Group} prefix, to the generated
 * type ({@code GroupRead} adds nothing): {@code @Valid @ValidGroup(GroupCreate.class) User} is generated as
 * {@code UserCreate}. Use it on a data model that has create/update variants (see {@code ApiGenerationMode}),
 * not on a request DTO dedicated to one endpoint, whose name would get the suffix twice
 * ({@code UserCreateCreate}).
 */
@Target({ ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGroup {
	/**
	 * The validation groups to activate.
	 * @return an array of validation group classes
	 */
	Class<?>[] value();
}

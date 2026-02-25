package org.atriasoft.archidata.annotation.apiGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applied to a class that also has {@code @JsonInclude(JsonInclude.Include.NON_NULL)},
 * this annotation controls which validation groups produce {@code .nullable().optional()}
 * instead of just {@code .optional()} in the generated Zod schemas.
 *
 * <p>This is useful for frameworks like React Hook Form that send {@code null} explicitly
 * for empty fields, requiring the Zod schema to accept both {@code null} values and
 * absent keys.
 *
 * <p>Example:
 * <pre>{@code
 * @JsonInclude(JsonInclude.Include.NON_NULL)
 * @ApiOptionalIsNullable(groups = { GroupCreate.class, GroupUpdate.class })
 * public class MyModel {
 *     public String name;       // In Create/Update: .nullable().optional()
 *                                // In Read: .optional()
 *     @NotNull
 *     public String required;   // Always required, no suffix
 * }
 * }</pre>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiOptionalIsNullable {
	/**
	 * The validation groups for which optional fields should also be nullable.
	 * For example, {@code { GroupCreate.class, GroupUpdate.class }}.
	 */
	Class<?>[] groups() default {};
}

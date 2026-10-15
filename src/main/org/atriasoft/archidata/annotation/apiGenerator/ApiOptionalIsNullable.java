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
 *
 * <p>Which form to use:
 * <ul>
 * <li>a data model read, created and edited (one class generating {@code X}, {@code XCreate}, {@code XUpdate},
 * endpoints taking {@code @Valid @ValidGroup(GroupCreate.class)}): list the write groups, as above;</li>
 * <li>a request DTO (a record or class with one use, endpoint taking a plain {@code @Valid}): no {@code groups},
 * and no {@code @ValidGroup} on the endpoint. Without {@code groups} (the default), every usage of the class gets
 * {@code .nullable().optional()}. Adding {@code @ValidGroup(GroupCreate.class)} instead would rename the generated
 * type ({@code UserCreate} becomes {@code UserCreateCreate}).</li>
 * </ul>
 * See {@code doc/typescript_api_generation.md}, section "Forms (react-hook-form)".
 *
 * <p>Without {@code @JsonInclude(NON_NULL)} the annotation has no effect: an optional field is
 * already generated as {@code .nullable()}.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiOptionalIsNullable {
	/**
	 * The validation groups for which optional fields should also be nullable.
	 * For example, {@code { GroupCreate.class, GroupUpdate.class }}. Empty (the default): every usage.
	 * @return an array of validation group classes, empty by default
	 */
	Class<?>[] groups() default {};
}

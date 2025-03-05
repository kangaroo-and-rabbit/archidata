package org.kar.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.kar.archidata.dataAccess.options.CheckFunctionInterface;

/**
 * The Checker annotation is used to specify a checker class that automatically
 * validates data for a parent class. This annotation can be applied to both
 * classes and fields to enforce validation rules defined in the checker class.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to types (classes) and fields.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data validation logic.
 *
 * <p>Behavior:
 * - When applied to a class or field, the Checker annotation specifies a
 *   checker class that implements the CheckFunctionInterface. This checker
 *   class is responsible for validating the data associated with the annotated
 *   element.
 * - The validation is automatically triggered when the data of the parent class
 *   is validated, ensuring that the data adheres to the specified rules.
 *
 * <p>Attributes:
 * - value: Specifies the checker class that implements the validation logic.
 *   This class must extend the CheckFunctionInterface.
 *
 * <p>Example:
 * <pre>{@code
 * public class User {
 *
 *     @Checker(UserDataChecker.class)
 *     public String email;
 * }
 *
 * public class UserDataChecker implements CheckFunctionInterface {
 *     ...
 * }
 * }</pre>
 *
 * In this example, the email field in the User class is validated using the
 * UserDataChecker class whenever the User class data is validated.
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Checker {
	Class<? extends CheckFunctionInterface> value();
}

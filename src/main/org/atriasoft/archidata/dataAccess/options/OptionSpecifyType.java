package org.atriasoft.archidata.dataAccess.options;

/**
 * Query option to specify the concrete type of an element when the model declares it as {@link Object}.
 *
 * <p>This is useful for deserialization when the actual type cannot be inferred from the model definition.
 */
public class OptionSpecifyType extends QueryOption {
	/** The field name whose type is being specified. */
	public final String name;
	/** The concrete class to use for the field. */
	public final Class<?> clazz;
	/** Whether the field is a list of the specified type. */
	public final boolean isList;

	/**
	 * Constructs an option to specify the type of a single-valued field.
	 *
	 * @param name the field name
	 * @param clazz the concrete class for the field
	 */
	public OptionSpecifyType(final String name, final Class<?> clazz) {
		this.clazz = clazz;
		this.name = name;
		this.isList = false;
	}

	/**
	 * Constructs an option to specify the type of a field, optionally as a list.
	 *
	 * @param name the field name
	 * @param clazz the concrete class for the field
	 * @param isList {@code true} if the field is a list of the specified type
	 */
	public OptionSpecifyType(final String name, final Class<?> clazz, final boolean isList) {
		this.clazz = clazz;
		this.name = name;
		this.isList = isList;
	}
}

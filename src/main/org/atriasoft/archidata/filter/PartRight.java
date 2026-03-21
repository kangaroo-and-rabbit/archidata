package org.atriasoft.archidata.filter;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration representing access rights for a resource.
 *
 * <p>
 * Each value encodes a combination of read and write permissions as an integer bitmask.
 * </p>
 */
public enum PartRight {
	/** No access rights. */
	NONE(0), //
	/** Read-only access. */
	READ(1), //
	/** Write-only access. */
	WRITE(2), //
	/** Full read and write access. */
	READ_WRITE(3);

	private final int value;

	/**
	 * Constructs a PartRight with the given integer value.
	 *
	 * @param value the integer representation of this right
	 */
	PartRight(final int value) {
		this.value = value;
	}

	/**
	 * Returns the integer value of this right.
	 *
	 * @return the integer value
	 */
	@JsonValue
	public int getValue() {
		return this.value;
	}

	/**
	 * Returns the PartRight corresponding to the given integer value.
	 *
	 * @param value the integer value to look up
	 * @return the matching PartRight
	 * @throws IllegalArgumentException if no PartRight matches the value
	 */
	public static PartRight fromValue(final int value) {
		for (final PartRight element : values()) {
			if (element.getValue() == value) {
				return element;
			}
		}
		throw new IllegalArgumentException("PartRight: Unknown value: " + value);
	}

	/**
	 * Returns the PartRight corresponding to the given long value.
	 *
	 * @param value the long value to look up
	 * @return the matching PartRight
	 * @throws IllegalArgumentException if no PartRight matches the value
	 */
	public static PartRight fromValue(final long value) {
		for (final PartRight element : values()) {
			if (element.getValue() == value) {
				return element;
			}
		}
		throw new IllegalArgumentException("PartRight: Unknown value: " + value);
	}

	/**
	 * Returns the PartRight corresponding to the given string name.
	 *
	 * @param value the string name (case-insensitive) of the right
	 * @return the matching PartRight
	 * @throws IllegalArgumentException if the value is null or does not match any PartRight
	 */
	public static PartRight fromString(final String value) {
		if (value == null) {
			throw new IllegalArgumentException("La chaîne ne peut pas être nulle");
		}

		return switch (value.toUpperCase()) {
			case "NONE" -> NONE;
			case "READ" -> READ;
			case "WRITE" -> WRITE;
			case "READ_WRITE" -> READ_WRITE;
			default -> throw new IllegalArgumentException("Valeur inconnue pour PartRight : " + value);
		};
	}
}

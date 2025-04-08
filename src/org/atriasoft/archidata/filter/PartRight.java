package org.atriasoft.archidata.filter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PartRight {
	NONE(0), //
	READ(1), //
	WRITE(2), //
	READ_WRITE(3);

	private final int value;

	PartRight(final int value) {
		this.value = value;
	}

	@JsonValue
	public int getValue() {
		return this.value;
	}

	public static PartRight fromValue(final int value) {
		for (final PartRight element : values()) {
			if (element.getValue() == value) {
				return element;
			}
		}
		throw new IllegalArgumentException("PartRight: Unknown value: " + value);
	}

	public static PartRight fromValue(final long value) {
		for (final PartRight element : values()) {
			if (element.getValue() == value) {
				return element;
			}
		}
		throw new IllegalArgumentException("PartRight: Unknown value: " + value);
	}

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

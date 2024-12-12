package org.kar.archidata.filter;

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
}

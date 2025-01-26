package org.kar.archidata.filter;

import java.util.Map;

public class RightSafeCaster {

	@SuppressWarnings("unchecked")
	public static Map<String, Map<String, PartRight>> safeCastAndTransform(final Object obj) {
		if (!(obj instanceof Map)) {
			throw new IllegalArgumentException("L'objet n'est pas un Map");
		}

		final Map<?, ?> outerMap = (Map<?, ?>) obj;

		// Résultat final après vérification et transformation
		final Map<String, Map<String, PartRight>> resultMap = new java.util.HashMap<>();

		for (final Map.Entry<?, ?> outerEntry : outerMap.entrySet()) {
			if (!(outerEntry.getKey() instanceof String)) {
				throw new IllegalArgumentException("Une clé du Map externe n'est pas de type String");
			}

			if (!(outerEntry.getValue() instanceof Map)) {
				throw new IllegalArgumentException("Une valeur du Map externe n'est pas un Map");
			}

			final String outerKey = (String) outerEntry.getKey();
			final Map<?, ?> innerMap = (Map<?, ?>) outerEntry.getValue();

			final Map<String, PartRight> transformedInnerMap = new java.util.HashMap<>();

			for (final Map.Entry<?, ?> innerEntry : innerMap.entrySet()) {
				if (!(innerEntry.getKey() instanceof String)) {
					throw new IllegalArgumentException("Une clé du Map interne n'est pas de type String");
				}

				final String innerKey = (String) innerEntry.getKey();
				final Object value = innerEntry.getValue();

				PartRight partRight;
				if (value instanceof PartRight) {
					partRight = (PartRight) value;
				} else if (value instanceof final Integer valueCasted) {
					partRight = PartRight.fromValue(valueCasted);
				} else if (value instanceof final Long valueCasted) {
					partRight = PartRight.fromValue(valueCasted);
				} else if (value instanceof final String valueCasted) {
					partRight = PartRight.fromString(valueCasted);
				} else {
					throw new IllegalArgumentException("The Map Value is neither PartRight nor String nor Integer");
				}

				transformedInnerMap.put(innerKey, partRight);
			}

			resultMap.put(outerKey, transformedInnerMap);
		}

		return resultMap;
	}
}

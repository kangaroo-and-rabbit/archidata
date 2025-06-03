package org.atriasoft.archidata.tools;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class TypeUtils {

	public static ParameterizedType listOf(final Class<?> clazz) {
		return new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[] { clazz };
			}

			@Override
			public Type getRawType() {
				return List.class;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}

			@Override
			public String toString() {
				return "List<" + clazz.getTypeName() + ">";
			}
		};
	}
}

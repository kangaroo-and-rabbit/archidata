package test.atriasoft.archidata.tools;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.atriasoft.archidata.tools.TypeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestTypeUtils {

	@Test
	void testListOfRawType() {
		final ParameterizedType type = TypeUtils.listOf(String.class);
		Assertions.assertEquals(List.class, type.getRawType());
	}

	@Test
	void testListOfActualTypeArgument() {
		final ParameterizedType type = TypeUtils.listOf(Integer.class);
		Assertions.assertEquals(1, type.getActualTypeArguments().length);
		Assertions.assertEquals(Integer.class, type.getActualTypeArguments()[0]);
	}

	@Test
	void testListOfOwnerTypeIsNull() {
		final ParameterizedType type = TypeUtils.listOf(String.class);
		Assertions.assertNull(type.getOwnerType());
	}

	@Test
	void testListOfToString() {
		final ParameterizedType type = TypeUtils.listOf(Double.class);
		Assertions.assertEquals("List<java.lang.Double>", type.toString());
	}

	@Test
	void testIsSameClassSameClass() {
		Assertions.assertTrue(TypeUtils.isSameClass(String.class, String.class));
	}

	@Test
	void testIsSameClassDifferentClasses() {
		Assertions.assertFalse(TypeUtils.isSameClass(String.class, Integer.class));
	}

	@Test
	void testIsSameClassPrimitiveVsWrapper() {
		// int.class and Integer.class have different names
		Assertions.assertFalse(TypeUtils.isSameClass(int.class, Integer.class));
	}
}

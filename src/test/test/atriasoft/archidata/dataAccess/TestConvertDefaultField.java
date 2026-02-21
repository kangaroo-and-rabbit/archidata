package test.atriasoft.archidata.dataAccess;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

class TestConvertDefaultField {

	private static Method convertMethod;

	static {
		try {
			convertMethod = DBAccessMongo.class.getDeclaredMethod("convertDefaultField", String.class, Field.class);
			convertMethod.setAccessible(true);
		} catch (final NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	// ===== Model with all supported field types =====

	public static class DefaultFieldModel {
		public long longPrimitive;
		public Long longBoxed;
		public int intPrimitive;
		public Integer intBoxed;
		public float floatPrimitive;
		public Float floatBoxed;
		public double doublePrimitive;
		public Double doubleBoxed;
		public boolean boolPrimitive;
		public Boolean boolBoxed;
		public Date dateField;
		public Instant instantField;
		public LocalDate localDateField;
		public LocalTime localTimeField;
		public String stringField;
		public TestEnum enumField;
	}

	public enum TestEnum {
		VALUE_A, VALUE_B, VALUE_C
	}

	@BeforeAll
	static void configureWebRecorder() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void removeDataBase() throws Exception {
		ConfigureDb.clear();
	}

	private Object convert(final String data, final String fieldName) throws Exception {
		final Field field = DefaultFieldModel.class.getField(fieldName);
		try {
			return convertMethod.invoke(ConfigureDb.da, data, field);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof Exception ex) {
				throw ex;
			}
			throw e;
		}
	}

	// ===== Long =====

	@Test
	void testLongPrimitive() throws Exception {
		Assertions.assertEquals(42L, convert("42", "longPrimitive"));
	}

	@Test
	void testLongBoxed() throws Exception {
		Assertions.assertEquals(123L, convert("123", "longBoxed"));
	}

	@Test
	void testLongWithQuotes() throws Exception {
		Assertions.assertEquals(0L, convert("'0'", "longPrimitive"));
	}

	@Test
	void testLongNegative() throws Exception {
		Assertions.assertEquals(-99L, convert("-99", "longBoxed"));
	}

	// ===== Integer =====

	@Test
	void testIntPrimitive() throws Exception {
		Assertions.assertEquals(7, convert("7", "intPrimitive"));
	}

	@Test
	void testIntBoxed() throws Exception {
		Assertions.assertEquals(256, convert("256", "intBoxed"));
	}

	@Test
	void testIntWithQuotes() throws Exception {
		Assertions.assertEquals(3, convert("'3'", "intPrimitive"));
	}

	// ===== Float =====

	@Test
	void testFloatPrimitive() throws Exception {
		Assertions.assertEquals(3.14f, convert("3.14", "floatPrimitive"));
	}

	@Test
	void testFloatBoxed() throws Exception {
		Assertions.assertEquals(0.0f, convert("0.0", "floatBoxed"));
	}

	// ===== Double =====

	@Test
	void testDoublePrimitive() throws Exception {
		Assertions.assertEquals(2.718, convert("2.718", "doublePrimitive"));
	}

	@Test
	void testDoubleBoxed() throws Exception {
		Assertions.assertEquals(1.0, convert("'1.0'", "doubleBoxed"));
	}

	// ===== Boolean =====

	@Test
	void testBoolTrue() throws Exception {
		Assertions.assertEquals(true, convert("true", "boolPrimitive"));
	}

	@Test
	void testBoolFalse() throws Exception {
		Assertions.assertEquals(false, convert("false", "boolBoxed"));
	}

	@Test
	void testBoolWithQuotes() throws Exception {
		Assertions.assertEquals(false, convert("'0'", "boolPrimitive"));
	}

	// ===== Date =====

	@Test
	void testDate() throws Exception {
		final Object result = convert("1700000000000", "dateField");
		Assertions.assertInstanceOf(Date.class, result);
		Assertions.assertEquals(new Date(1700000000000L), result);
	}

	@Test
	void testDateWithQuotes() throws Exception {
		final Object result = convert("'0'", "dateField");
		Assertions.assertInstanceOf(Date.class, result);
		Assertions.assertEquals(new Date(0L), result);
	}

	// ===== Instant =====

	@Test
	void testInstant() throws Exception {
		final Object result = convert("2025-01-15T10:30:00Z", "instantField");
		Assertions.assertInstanceOf(Instant.class, result);
		Assertions.assertEquals(Instant.parse("2025-01-15T10:30:00Z"), result);
	}

	@Test
	void testInstantWithQuotes() throws Exception {
		final Object result = convert("'2024-06-01T00:00:00Z'", "instantField");
		Assertions.assertInstanceOf(Instant.class, result);
		Assertions.assertEquals(Instant.parse("2024-06-01T00:00:00Z"), result);
	}

	// ===== LocalDate =====

	@Test
	void testLocalDate() throws Exception {
		final Object result = convert("2025-03-20", "localDateField");
		Assertions.assertInstanceOf(LocalDate.class, result);
		Assertions.assertEquals(LocalDate.of(2025, 3, 20), result);
	}

	@Test
	void testLocalDateWithQuotes() throws Exception {
		final Object result = convert("'2000-01-01'", "localDateField");
		Assertions.assertInstanceOf(LocalDate.class, result);
		Assertions.assertEquals(LocalDate.of(2000, 1, 1), result);
	}

	// ===== LocalTime =====

	@Test
	void testLocalTime() throws Exception {
		final Object result = convert("14:30:00", "localTimeField");
		Assertions.assertInstanceOf(LocalTime.class, result);
		Assertions.assertEquals(LocalTime.of(14, 30, 0), result);
	}

	@Test
	void testLocalTimeWithQuotes() throws Exception {
		final Object result = convert("'08:00:00'", "localTimeField");
		Assertions.assertInstanceOf(LocalTime.class, result);
		Assertions.assertEquals(LocalTime.of(8, 0, 0), result);
	}

	@Test
	void testLocalTimeWithNanos() throws Exception {
		final Object result = convert("10:15:30.123456789", "localTimeField");
		Assertions.assertInstanceOf(LocalTime.class, result);
		Assertions.assertEquals(LocalTime.of(10, 15, 30, 123456789), result);
	}

	// ===== String =====

	@Test
	void testString() throws Exception {
		Assertions.assertEquals("hello", convert("hello", "stringField"));
	}

	@Test
	void testStringWithQuotes() throws Exception {
		Assertions.assertEquals("world", convert("'world'", "stringField"));
	}

	@Test
	void testStringEmpty() throws Exception {
		Assertions.assertEquals("", convert("''", "stringField"));
	}

	// ===== Enum =====

	@Test
	void testEnum() throws Exception {
		Assertions.assertEquals(TestEnum.VALUE_A, convert("VALUE_A", "enumField"));
	}

	@Test
	void testEnumWithQuotes() throws Exception {
		Assertions.assertEquals(TestEnum.VALUE_C, convert("'VALUE_C'", "enumField"));
	}

	// ===== Error cases =====

	@Test
	void testUnknownType() {
		Assertions.assertThrows(DataAccessException.class, () -> {
			final Field field = TestConvertDefaultField.class.getDeclaredField("UNKNOWN_FIELD_MARKER");
			try {
				convertMethod.invoke(ConfigureDb.da, "data", field);
			} catch (final InvocationTargetException e) {
				if (e.getCause() instanceof Exception ex) {
					throw ex;
				}
				throw e;
			}
		});
	}

	// Marker field for unknown type test
	@SuppressWarnings("unused")
	private Object UNKNOWN_FIELD_MARKER;
}

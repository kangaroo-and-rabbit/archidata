package org.atriasoft.archidata.externalRestApi.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Generates example values for API fields based on explicit annotations, constraints, field names,
 * and type defaults. Used to produce human-readable OpenAPI examples.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Explicit example from {@code @ApiDoc(example=...)} or {@code @Schema(example=...)} (handled before calling this class)</li>
 *   <li>Constraint-based generation ({@code @Size}, {@code @Min}, {@code @Max}, {@code @Email}, etc.)</li>
 *   <li>Field-name heuristics (e.g. "email" → "user@example.com")</li>
 *   <li>Type-based defaults (e.g. String → "string", Long → "0")</li>
 * </ol>
 */
public class ExampleGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExampleGenerator.class);

	private ExampleGenerator() {
		// Utility class
	}

	/**
	 * Generate an example value for a field.
	 *
	 * @param fieldName  the field name (e.g. "login", "createdAt")
	 * @param model      the ClassModel describing the field type
	 * @param stringSize @Size constraint (may be null)
	 * @param min        @Min constraint (may be null)
	 * @param max        @Max constraint (may be null)
	 * @param decimalMin @DecimalMin constraint (may be null)
	 * @param decimalMax @DecimalMax constraint (may be null)
	 * @param pattern    @Pattern constraint (may be null)
	 * @param email      @Email constraint (may be null)
	 * @return a generated example string, or null if no example can be produced
	 */
	public static String generate(
			final String fieldName,
			final ClassModel model,
			final Size stringSize,
			final Min min,
			final Max max,
			final DecimalMin decimalMin,
			final DecimalMax decimalMax,
			final Pattern pattern,
			final Email email) {
		final Class<?> type = model.getOriginClasses();
		if (type == null) {
			return null;
		}
		// 1. Constraint-based generation
		final String constraintExample = generateFromConstraints(fieldName, type, stringSize, min, max, decimalMin, decimalMax, email);
		if (constraintExample != null) {
			return constraintExample;
		}
		// 2. Field-name heuristics
		final String nameExample = generateFromFieldName(fieldName, type);
		if (nameExample != null) {
			return nameExample;
		}
		// 3. Type-based defaults
		return generateFromType(type, model);
	}

	private static String generateFromConstraints(
			final String fieldName,
			final Class<?> type,
			final Size stringSize,
			final Min min,
			final Max max,
			final DecimalMin decimalMin,
			final DecimalMax decimalMax,
			final Email email) {
		// @Email takes priority for strings
		if (email != null) {
			return "user@example.com";
		}
		// String with @Size constraint
		if (isStringType(type) && stringSize != null) {
			final int minLen = stringSize.min();
			final int maxLen = stringSize.max();
			final String baseName = generateFromFieldName(fieldName, type);
			if (baseName != null) {
				if (baseName.length() >= minLen && baseName.length() <= maxLen) {
					return baseName;
				}
				if (baseName.length() < minLen) {
					final StringBuilder sb = new StringBuilder(baseName);
					while (sb.length() < minLen) {
						sb.append("x");
					}
					return sb.toString();
				}
				return baseName.substring(0, maxLen);
			}
			// Generate a string of appropriate length
			final int targetLen = Math.max(minLen, Math.min(8, maxLen));
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < targetLen; i++) {
				sb.append((char) ('a' + (i % 26)));
			}
			return sb.toString();
		}
		// Numeric with @Min/@Max
		if (isIntegerType(type)) {
			if (min != null) {
				return String.valueOf(min.value());
			}
			if (max != null) {
				return String.valueOf(Math.min(0, max.value()));
			}
		}
		if (isFloatingType(type)) {
			if (decimalMin != null) {
				return decimalMin.value();
			}
			if (decimalMax != null) {
				return "0.0";
			}
			if (min != null) {
				return String.valueOf((double) min.value());
			}
			if (max != null) {
				return String.valueOf(Math.min(0.0, (double) max.value()));
			}
		}
		return null;
	}

	private static String generateFromFieldName(final String fieldName, final Class<?> type) {
		final String lower = fieldName.toLowerCase(Locale.ROOT);

		// Email patterns
		if (lower.contains("email") || lower.contains("mail")) {
			return "user@example.com";
		}
		// URL patterns
		if (lower.contains("url") || lower.contains("uri") || lower.contains("link") || lower.contains("href")) {
			return "https://example.com/resource";
		}
		// Token / password patterns
		if (lower.contains("token") || lower.contains("password") || lower.contains("secret")) {
			return "abc123xyz789";
		}
		// MIME type patterns
		if (lower.contains("mime") || lower.equals("type") || lower.equals("contenttype") || lower.equals("mediatype")) {
			return "application/json";
		}
		// Hash patterns
		if (lower.contains("sha") || lower.contains("hash") || lower.contains("md5")) {
			return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
		}
		// Language patterns
		if (lower.equals("language") || lower.equals("lang") || lower.equals("locale")) {
			return "en";
		}
		// ID fields
		if (lower.equals("id") || lower.endsWith("id") || lower.endsWith("Id")) {
			if (type == UUID.class || type == java.util.UUID.class) {
				return "e6b33c1c-d24d-11ee-b616-02420a030102";
			}
			if (type == ObjectId.class) {
				return "65161616841351";
			}
			if (isIntegerType(type)) {
				return "123456";
			}
			if (isStringType(type)) {
				return "123456";
			}
		}
		// Date/time fields
		if (lower.endsWith("at") || lower.endsWith("time") || lower.endsWith("date")
				|| lower.startsWith("created") || lower.startsWith("updated") || lower.startsWith("last")
				|| lower.startsWith("start") || lower.startsWith("end")) {
			if (type == Date.class) {
				return "2000-01-23T01:23:45.678Z";
			}
			if (type == LocalDate.class) {
				return "2000-01-23";
			}
			if (type == LocalTime.class) {
				return "01:23:45";
			}
			if (type == Instant.class) {
				return "2000-01-23T01:23:45.678Z";
			}
		}
		// Boolean patterns
		if (lower.contains("deleted") || lower.contains("blocked") || lower.contains("disabled")
				|| lower.contains("hidden") || lower.contains("archived")) {
			if (isBooleanType(type)) {
				return "false";
			}
		}
		if (lower.contains("enabled") || lower.contains("active") || lower.contains("visible")) {
			if (isBooleanType(type)) {
				return "true";
			}
		}
		// Name / login patterns
		if (lower.contains("name") || lower.contains("login") || lower.contains("username")) {
			if (isStringType(type)) {
				return "example-name";
			}
		}
		// Title / label patterns
		if (lower.contains("title") || lower.contains("label") || lower.contains("subject")) {
			if (isStringType(type)) {
				return "Example Title";
			}
		}
		// Description / comment patterns
		if (lower.contains("description") || lower.contains("comment") || lower.contains("note")
				|| lower.contains("message") || lower.contains("log") || lower.contains("reason")) {
			if (isStringType(type)) {
				return "Example description text";
			}
		}
		// Size / dimension patterns
		if (lower.equals("size") || lower.equals("length") || lower.equals("count")) {
			if (isIntegerType(type)) {
				return "1024";
			}
		}
		if (lower.equals("width")) {
			if (isIntegerType(type)) {
				return "1920";
			}
		}
		if (lower.equals("height")) {
			if (isIntegerType(type)) {
				return "1080";
			}
		}
		// Audio patterns
		if (lower.contains("samplerate")) {
			if (isIntegerType(type)) {
				return "48000";
			}
		}
		if (lower.equals("channels")) {
			if (isIntegerType(type)) {
				return "2";
			}
		}
		if (lower.contains("bitdepth")) {
			if (isIntegerType(type)) {
				return "16";
			}
		}
		// Duration / rate patterns
		if (lower.contains("duration")) {
			if (isFloatingType(type)) {
				return "120.5";
			}
		}
		if (lower.contains("framerate") || lower.contains("fps")) {
			if (isFloatingType(type)) {
				return "29.97";
			}
		}
		// Codec
		if (lower.equals("codec")) {
			if (isStringType(type)) {
				return "h264";
			}
		}
		// Path / file patterns
		if (lower.contains("path") || lower.contains("file") || lower.contains("filename")) {
			if (isStringType(type)) {
				return "/data/example-file.dat";
			}
		}
		return null;
	}

	private static String generateFromType(final Class<?> type, final ClassModel model) {
		// String
		if (isStringType(type)) {
			return "string";
		}
		// Boolean
		if (isBooleanType(type)) {
			return "false";
		}
		// Integer types
		if (type == Long.class || type == long.class) {
			return "0";
		}
		if (type == Integer.class || type == int.class) {
			return "0";
		}
		if (type == Short.class || type == short.class) {
			return "0";
		}
		// Floating types
		if (type == Double.class || type == double.class) {
			return "0.0";
		}
		if (type == Float.class || type == float.class) {
			return "0.0";
		}
		// UUID
		if (type == UUID.class) {
			return "e6b33c1c-d24d-11ee-b616-02420a030102";
		}
		// ObjectId
		if (type == ObjectId.class) {
			return "65161616841351";
		}
		// Date/time
		if (type == Date.class || type == Instant.class) {
			return "2000-01-23T01:23:45.678Z";
		}
		if (type == LocalDate.class) {
			return "2000-01-23";
		}
		if (type == LocalTime.class) {
			return "01:23:45";
		}
		// Enum: first value
		if (model instanceof ClassEnumModel) {
			final ClassEnumModel enumModel = (ClassEnumModel) model;
			if (!enumModel.getListOfValues().isEmpty()) {
				return enumModel.getListOfValues().keySet().iterator().next();
			}
		}
		// Collections, Maps, complex objects: no auto-generation
		return null;
	}

	private static boolean isStringType(final Class<?> type) {
		return type == String.class;
	}

	private static boolean isBooleanType(final Class<?> type) {
		return type == Boolean.class || type == boolean.class;
	}

	private static boolean isIntegerType(final Class<?> type) {
		return type == Long.class || type == long.class
				|| type == Integer.class || type == int.class
				|| type == Short.class || type == short.class;
	}

	private static boolean isFloatingType(final Class<?> type) {
		return type == Double.class || type == double.class
				|| type == Float.class || type == float.class;
	}
}

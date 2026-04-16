package org.atriasoft.archidata.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.CollectionItemNotNull;
import org.atriasoft.archidata.annotation.checker.CollectionItemUnique;
import org.atriasoft.archidata.annotation.checker.CollectionNotEmpty;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;

/**
 * Utility class providing helper methods for annotation introspection on classes,
 * fields, methods, and parameters. Supports lookup across class hierarchies and
 * implemented interfaces.
 */
public class AnnotationTools {
	/** Prevents instantiation of this utility class. */
	private AnnotationTools() {
		// Utility class
	}

	/** Logger for this class. */
	static final Logger LOGGER = LoggerFactory.getLogger(AnnotationTools.class);

	/**
	 * Checks whether a method has a given annotation, including on interface declarations.
	 *
	 * @param method the method to inspect
	 * @param annotationClass the annotation type to look for
	 * @return {@code true} if the annotation is present on the method or its interface declaration
	 */
	public static boolean methodHasAnnotation(final Method method, final Class<? extends Annotation> annotationClass) {
		if (method.isAnnotationPresent(annotationClass)) {
			return true;
		}
		final Class<?> declaringClass = method.getDeclaringClass();
		for (final Class<?> iface : declaringClass.getInterfaces()) {
			try {
				final Method interfaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
				if (interfaceMethod.isAnnotationPresent(annotationClass)) {
					return true;
				}
			} catch (final NoSuchMethodException ignored) {}
		}
		return false;
	}

	/**
	 * Checks whether a class has a given annotation, including on implemented interfaces.
	 *
	 * @param clazz the class to inspect
	 * @param annotationClass the annotation type to look for
	 * @return {@code true} if the annotation is present on the class or its interfaces
	 */
	public static boolean hasAnnotation(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
		if (clazz.isAnnotationPresent(annotationClass)) {
			return true;
		}
		for (final Class<?> iface : clazz.getInterfaces()) {
			if (iface.isAnnotationPresent(annotationClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Collects all instances of an annotation from the class and its interfaces.
	 *
	 * @param <TYPE> the annotation type
	 * @param clazz the class to inspect
	 * @param annotationClass the annotation type to look for
	 * @return a list of all matching annotations found
	 */
	public static <TYPE extends Annotation> List<TYPE> getAnnotationsIncludingInterfaces(
			final Class<?> clazz,
			final Class<TYPE> annotationClass) {
		final List<TYPE> result = new ArrayList<>();
		final TYPE[] declared = clazz.getDeclaredAnnotationsByType(annotationClass);
		Collections.addAll(result, declared);
		for (final Class<?> iface : clazz.getInterfaces()) {
			final TYPE[] ifaceAnnotations = iface.getDeclaredAnnotationsByType(annotationClass);
			Collections.addAll(result, ifaceAnnotations);
		}

		return result;
	}

	/**
	 * Retrieves a single annotation from a method, including interface declarations.
	 *
	 * @param <TYPE> the annotation type
	 * @param method the method to inspect
	 * @param annotationClass the annotation type to look for
	 * @return the annotation instance, or {@code null} if not found
	 */
	public static <TYPE extends Annotation> TYPE getAnnotationIncludingInterfaces(
			final Method method,
			final Class<TYPE> annotationClass) {
		TYPE annotation = method.getAnnotation(annotationClass);
		if (annotation != null) {
			return annotation;
		}
		final Class<?> declaringClass = method.getDeclaringClass();
		for (final Class<?> iface : declaringClass.getInterfaces()) {
			try {
				final Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
				annotation = ifaceMethod.getAnnotation(annotationClass);
				if (annotation != null) {
					return annotation;
				}
			} catch (final NoSuchMethodException e) {
				// Ignored
			}
		}
		return null;
	}

	/**
	 * Collects all instances of an annotation from a method and its interface declarations.
	 *
	 * @param <TYPE> the annotation type
	 * @param method the method to inspect
	 * @param annotationClass the annotation type to look for
	 * @return a list of all matching annotations found
	 */
	public static <TYPE extends Annotation> List<TYPE> getAnnotationsIncludingInterfaces(
			final Method method,
			final Class<TYPE> annotationClass) {
		final List<TYPE> result = new ArrayList<>();

		final TYPE[] declared = method.getDeclaredAnnotationsByType(annotationClass);
		Collections.addAll(result, declared);

		final Class<?> declaringClass = method.getDeclaringClass();
		for (final Class<?> iface : declaringClass.getInterfaces()) {
			try {
				final Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
				final TYPE[] ifaceAnnotations = ifaceMethod.getDeclaredAnnotationsByType(annotationClass);
				Collections.addAll(result, ifaceAnnotations);
			} catch (final NoSuchMethodException e) {
				// Ignored
			}
		}
		return result;
	}

	/**
	 * Collects all instances of an annotation from a method parameter and its interface declarations.
	 *
	 * @param <TYPE> the annotation type
	 * @param method the method to inspect
	 * @param parameterIndex the zero-based index of the parameter
	 * @param annotationClass the annotation type to look for
	 * @return a list of all matching annotations found
	 */
	public static <TYPE extends Annotation> List<TYPE> getAnnotationsIncludingInterfaces(
			final Method method,
			final int parameterIndex,
			final Class<TYPE> annotationClass) {

		final List<TYPE> result = new ArrayList<>();
		final Parameter parameter = method.getParameters()[parameterIndex];
		final TYPE[] declared = parameter.getDeclaredAnnotationsByType(annotationClass);
		Collections.addAll(result, declared);
		final Class<?> declaringClass = method.getDeclaringClass();
		for (final Class<?> iface : declaringClass.getInterfaces()) {
			try {
				final Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
				final Parameter ifaceParam = ifaceMethod.getParameters()[parameterIndex];

				final TYPE[] ifaceAnnotations = ifaceParam.getDeclaredAnnotationsByType(annotationClass);
				Collections.addAll(result, ifaceAnnotations);
			} catch (final NoSuchMethodException e) {
				// Ignored
			}
		}
		return result;
	}

	/**
	 * Retrieves a single declared annotation from a parameter.
	 *
	 * @param <TYPE> the annotation type
	 * @param param the parameter to inspect
	 * @param clazz the annotation type to look for
	 * @return the annotation instance, or {@code null} if not found
	 */
	public static <TYPE extends Annotation> TYPE get(final Parameter param, final Class<TYPE> clazz) {
		final TYPE[] annotations = param.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations[0];
	}

	/**
	 * Retrieves all declared annotations of a type from a parameter.
	 *
	 * @param <TYPE> the annotation type
	 * @param param the parameter to inspect
	 * @param clazz the annotation type to look for
	 * @return an array of matching annotations, or {@code null} if none found
	 */
	public static <TYPE extends Annotation> TYPE[] gets(final Parameter param, final Class<TYPE> clazz) {
		final TYPE[] annotations = param.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations;
	}

	/**
	 * Retrieves a single declared annotation from a field.
	 *
	 * @param <TYPE> the annotation type
	 * @param element the field to inspect
	 * @param clazz the annotation type to look for
	 * @return the annotation instance, or {@code null} if not found
	 */
	public static <TYPE extends Annotation> TYPE get(final Field element, final Class<TYPE> clazz) {
		final TYPE[] annotations = element.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations[0];
	}

	/**
	 * Retrieves all declared annotations of a type from a field.
	 *
	 * @param <TYPE> the annotation type
	 * @param element the field to inspect
	 * @param clazz the annotation type to look for
	 * @return an array of matching annotations, or {@code null} if none found
	 */
	public static <TYPE extends Annotation> TYPE[] gets(final Field element, final Class<TYPE> clazz) {
		final TYPE[] annotations = element.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations;
	}

	/**
	 * Retrieves a single declared annotation from a class.
	 *
	 * @param <TYPE> the annotation type
	 * @param classObject the class to inspect
	 * @param clazz the annotation type to look for
	 * @return the annotation instance, or {@code null} if not found
	 */
	public static <TYPE extends Annotation> TYPE get(final Class<?> classObject, final Class<TYPE> clazz) {
		final TYPE[] annotations = classObject.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations[0];
	}

	/**
	 * Retrieves all declared annotations of a type from a class.
	 *
	 * @param <TYPE> the annotation type
	 * @param classObject the class to inspect
	 * @param clazz the annotation type to look for
	 * @return an array of matching annotations, or {@code null} if none found
	 */
	public static <TYPE extends Annotation> TYPE[] gets(final Class<?> classObject, final Class<TYPE> clazz) {
		final TYPE[] annotations = classObject.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations;
	}

	/**
	 * Returns the collection (table) name for the given class, considering query option overrides.
	 *
	 * @param clazz the entity class
	 * @param options the query options that may override the table name, may be {@code null}
	 * @return the resolved table name
	 * @throws DataAccessException if the table name cannot be determined
	 */
	public static String getTableName(final Class<?> clazz, final QueryOptions options) throws DataAccessException {
		if (options != null) {
			final List<OverrideTableName> data = options.get(OverrideTableName.class);
			if (data.size() == 1) {
				return data.get(0).getName();
			}
		}
		return AnnotationTools.getTableName(clazz);
	}

	/**
	 * Returns the collection (table) name from the {@code @Table} annotation or the class simple name.
	 *
	 * @param element the entity class
	 * @return the table name
	 */
	public static String getTableName(final Class<?> element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Table.class);
		if (annotation.length == 0) {
			// when no annotation is detected, then the table name is the class name
			return element.getSimpleName();
		}
		final String tmp = ((Table) annotation[0]).name();
		if (tmp == null) {
			return element.getSimpleName();
		}
		return tmp;
	}

	/**
	 * Retrieves the {@link CollectionItemNotNull} annotation from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static CollectionItemNotNull getCollectionItemNotNull(final Field element) {
		return get(element, CollectionItemNotNull.class);
	}

	/**
	 * Retrieves the {@link CollectionItemUnique} annotation from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static CollectionItemUnique getCollectionItemUnique(final Field element) {
		return get(element, CollectionItemUnique.class);
	}

	/**
	 * Retrieves the {@link CollectionNotEmpty} annotation from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static CollectionNotEmpty getCollectionNotEmpty(final Field element) {
		return get(element, CollectionNotEmpty.class);
	}

	/**
	 * Retrieves the schema example string from {@code @ApiDoc} or {@code @Schema} on a class.
	 *
	 * @param element the class to inspect
	 * @return the example string, or {@code null} if not present
	 */
	public static String getSchemaExample(final Class<?> element) {
		final ApiDoc apiDoc = get(element, ApiDoc.class);
		if (apiDoc != null && !apiDoc.example().isEmpty()) {
			return apiDoc.example();
		}
		// Fallback to @Schema (deprecated)
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		final String example = ((Schema) annotation[0]).example();
		if (example != null && !example.isEmpty()) {
			LOGGER.warn("@Schema(example=...) on class '{}' is deprecated. Use @ApiDoc(example=...) instead.",
					element.getSimpleName());
		}
		return example;
	}

	/**
	 * Retrieves the schema example string from {@code @ApiDoc} or {@code @Schema} on a field.
	 *
	 * @param element the field to inspect
	 * @return the example string, or {@code null} if not present
	 */
	public static String getSchemaExample(final Field element) {
		final ApiDoc apiDoc = get(element, ApiDoc.class);
		if (apiDoc != null && !apiDoc.example().isEmpty()) {
			return apiDoc.example();
		}
		// Fallback to @Schema (deprecated)
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		final String example = ((Schema) annotation[0]).example();
		if (example != null && !example.isEmpty()) {
			LOGGER.warn("@Schema(example=...) on field '{}' is deprecated. Use @ApiDoc(example=...) instead.",
					element.getName());
		}
		return example;
	}

	/**
	 * Retrieves the schema description string from {@code @ApiDoc} or {@code @Schema} on a class.
	 *
	 * @param element the class to inspect
	 * @return the description string, or {@code null} if not present
	 */
	public static String getSchemaDescription(final Class<?> element) {
		final ApiDoc apiDoc = get(element, ApiDoc.class);
		if (apiDoc != null && !apiDoc.description().isEmpty()) {
			return apiDoc.description();
		}
		// Fallback to @Schema (deprecated)
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		final String desc = ((Schema) annotation[0]).description();
		if (desc != null && !desc.isEmpty()) {
			LOGGER.warn("@Schema(description=...) on class '{}' is deprecated. Use @ApiDoc(description=...) instead.",
					element.getSimpleName());
		}
		return desc;
	}

	/**
	 * Retrieves the schema description string from {@code @ApiDoc} or {@code @Schema} on a field.
	 *
	 * @param element the field to inspect
	 * @return the description string, or {@code null} if not present
	 */
	public static String getSchemaDescription(final Field element) {
		final ApiDoc apiDoc = get(element, ApiDoc.class);
		if (apiDoc != null && !apiDoc.description().isEmpty()) {
			return apiDoc.description();
		}
		// Fallback to @Schema (deprecated)
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		final String desc = ((Schema) annotation[0]).description();
		if (desc != null && !desc.isEmpty()) {
			LOGGER.warn("@Schema(description=...) on field '{}' is deprecated. Use @ApiDoc(description=...) instead.",
					element.getName());
		}
		return desc;
	}

	/**
	 * Retrieves the default value string from the {@code @DefaultValue} annotation on a field.
	 *
	 * @param element the field to inspect
	 * @return the default value string, or {@code null} if not annotated
	 */
	public static String getDefault(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(DefaultValue.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((DefaultValue) annotation[0]).value();
	}

	/**
	 * Retrieves the {@code @ManyToOne} annotation from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static ManyToOne getManyToOne(final Field element) {
		return get(element, ManyToOne.class);
	}

	/**
	 * Retrieves the {@code @ManyToMany} annotation from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static ManyToMany getManyToMany(final Field element) {
		return get(element, ManyToMany.class);
	}

	/**
	 * Retrieves the {@code @OneToMany} annotation from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static OneToMany getOneToMany(final Field element) {
		return get(element, OneToMany.class);
	}

	/**
	 * Retrieves the {@code @DecimalMin} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static DecimalMin getConstraintsDecimalMin(final Field element) {
		return get(element, DecimalMin.class);
	}

	/**
	 * Retrieves the {@code @DecimalMax} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static DecimalMax getConstraintsDecimalMax(final Field element) {
		return get(element, DecimalMax.class);
	}

	/**
	 * Retrieves the {@code @Max} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static Max getConstraintsMax(final Field element) {
		return get(element, Max.class);
	}

	/**
	 * Retrieves the {@code @Min} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static Min getConstraintsMin(final Field element) {
		return get(element, Min.class);
	}

	/**
	 * Returns the column length from {@code @Column(length)}, defaulting to 255.
	 *
	 * @param element the field to inspect
	 * @return the column length, or 255 if not specified
	 */
	public static int getLimitSize(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return 255;
		}
		final int length = ((Column) annotation[0]).length();
		return length <= 0 ? 0 : length;
	}

	/**
	 * Retrieves the {@code @Size} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static Size getConstraintsSize(final Field element) {
		return get(element, Size.class);
	}

	/**
	 * Retrieves the {@code @Pattern} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static Pattern getConstraintsPattern(final Field element) {
		return get(element, Pattern.class);
	}

	/**
	 * Retrieves the {@code @Email} validation constraint from a field.
	 *
	 * @param element the field to inspect
	 * @return the annotation instance, or {@code null} if not present
	 */
	public static Email getConstraintsEmail(final Field element) {
		return get(element, Email.class);
	}

	/**
	 * Checks whether a field has an annotation of the given type, either directly
	 * or as a meta-annotation on one of its annotations.
	 *
	 * @param field the field to inspect
	 * @param annotationType the annotation type to search for
	 * @return {@code true} if the annotation or meta-annotation is present
	 */
	public static boolean isAnnotationGroup(final Field field, final Class<?> annotationType) {
		try {
			final Annotation[] anns = field.getAnnotations();
			for (final Annotation ann : anns) {
				if (ann.annotationType() == annotationType) {
					return true;
				}
			}
			for (final Annotation ann : anns) {
				final Annotation[] anns2 = ann.annotationType().getDeclaredAnnotations();
				for (final Annotation ann2 : anns2) {
					if (ann2.annotationType() == annotationType) {
						return true;
					}
				}
			}
		} catch (final Exception ex) {
			LOGGER.error("Catch exception when try to get annotation...{}", ex.getLocalizedMessage());
			return false;
		}
		return false;
	}

	/**
	 * Returns the raw field name from {@code @Column(name)} or the Java field name.
	 *
	 * @param element the field to inspect
	 * @return the resolved field name
	 */
	public static String getFieldNameRaw(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return element.getName();
		}
		final String name = ((Column) annotation[0]).name();
		if (name.isBlank()) {
			return element.getName();
		}
		return name;
	}

	/**
	 * Represents a field name pair: the structural name (in code) and the table name (in DB, possibly renamed).
	 *
	 * @param inStruct the field name as used in the Java structure
	 * @param inTable the field name as used in the database table
	 */
	public record FieldName(
			String inStruct,
			String inTable) {};

	/**
	 * Returns the resolved field name pair considering {@link OptionRenameColumn} overrides.
	 *
	 * @param element the field to inspect
	 * @param options the query options containing potential column renames, may be {@code null}
	 * @return the field name pair (inStruct and inTable)
	 */
	public static FieldName getFieldName(final Field element, final QueryOptions options) {
		final String inStructName = getFieldNameRaw(element);
		String inTableName = inStructName;
		if (options != null) {
			final List<OptionRenameColumn> renamesColumn = options.get(OptionRenameColumn.class);
			for (final OptionRenameColumn rename : renamesColumn) {
				if (rename.columnName.equals(inStructName)) {
					inTableName = rename.ColumnNewName;
					LOGGER.trace("Detect overwrite of column name '{}' => '{}'", inStructName, inTableName);
					break;
				}
			}
		}
		return new FieldName(inStructName, inTableName);
	}

	/**
	 * Checks whether the {@code @Column} annotation specifies {@code nullable=false}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the column is marked as not-null
	 */
	public static boolean getColumnNotNull(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		return !((Column) annotation[0]).nullable();
	}

	/**
	 * Checks whether the field is annotated with {@code @Nullable}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is annotated as nullable
	 */
	public static boolean getNullable(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Nullable.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the field is annotated with {@code @NotNull}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field has the {@code @NotNull} constraint
	 */
	public static boolean getConstraintsNotNull(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(NotNull.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Collect all non-static instance fields from clazz and its superclasses,
	 * walking up the hierarchy. Superclass fields come first.
	 *
	 * @param clazz the class whose fields to collect
	 * @return a list of all instance fields in hierarchy order
	 */
	public static List<Field> getAllInstanceFields(final Class<?> clazz) {
		final List<Field> result = new ArrayList<>();
		final List<Class<?>> hierarchy = new ArrayList<>();
		Class<?> current = clazz;
		while (current != null && current != Object.class) {
			// Skip JDK internal classes (e.g. Enum, Boolean) whose fields
			// cannot be made accessible due to module encapsulation.
			if (current.getPackageName().startsWith("java.")) {
				break;
			}
			hierarchy.add(current);
			current = current.getSuperclass();
		}
		Collections.reverse(hierarchy);
		final Set<String> seen = new LinkedHashSet<>();
		for (final Class<?> cls : hierarchy) {
			for (final Field field : cls.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (!seen.contains(field.getName())) {
					seen.add(field.getName());
					field.setAccessible(true);
					result.add(field);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the field annotated with {@code @Id} in the given class, or {@code null} if none.
	 *
	 * @param clazz the class to inspect
	 * @return the primary key field, or {@code null}
	 */
	public static Field getPrimaryKeyField(final Class<?> clazz) {
		for (final Field field : getAllInstanceFields(clazz)) {
			if (AnnotationTools.isPrimaryKey(field)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Checks whether a field is annotated with {@code @Id}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is a primary key
	 */
	public static boolean isPrimaryKey(final Field element) {
		final Annotation[] idAnnotations = element.getDeclaredAnnotationsByType(Id.class);
		if (idAnnotations.length > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether a field has a unique constraint via {@code @Column(unique=true)}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is marked as unique
	 */
	public static boolean isUnique(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		return ((Column) annotation[0]).unique();
	}

	/**
	 * Returns the {@link GenerationType} strategy from {@code @GeneratedValue}, or {@code null}.
	 *
	 * @param element the field to inspect
	 * @return the generation strategy, or {@code null} if not annotated
	 */
	public static GenerationType getStrategy(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(GeneratedValue.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((GeneratedValue) annotation[0]).strategy();
	}

	/**
	 * Checks whether a field is annotated with {@code @DataDeleted}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is a soft-delete marker
	 */
	public static boolean isDeletedField(final Field element) {
		return element.getDeclaredAnnotationsByType(DataDeleted.class).length != 0;
	}

	/**
	 * Checks whether a field is annotated with {@code @CreationTimestamp}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is a creation timestamp
	 */
	public static boolean isCreatedAtField(final Field element) {
		return element.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
	}

	/**
	 * Checks whether a field is annotated with {@code @UpdateTimestamp}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is an update timestamp
	 */
	public static boolean isUpdateAtField(final Field element) {
		return element.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
	}

	/**
	 * Checks whether a field is annotated with {@code @DataNotRead}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is excluded from default reads
	 */
	public static boolean isDefaultNotRead(final Field element) {
		return element.getDeclaredAnnotationsByType(DataNotRead.class).length != 0;
	}

	/**
	 * Checks whether a field is annotated with {@code @ApiReadOnly}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is API read-only
	 */
	public static boolean isApiReadOnly(final Field element) {
		return element.getDeclaredAnnotationsByType(ApiReadOnly.class).length != 0;
	}

	/**
	 * Checks whether a field is annotated with {@code @Id}.
	 *
	 * @param element the field to inspect
	 * @return {@code true} if the field is an ID field
	 */
	public static boolean isIdField(final Field element) {
		return element.getDeclaredAnnotationsByType(Id.class).length != 0;
	}

	/**
	 * Returns the DB column name of the soft-delete marker field, or {@code null} if none exists.
	 * Note: the delete field cannot be renamed with {@link OptionRenameColumn}.
	 *
	 * @param clazz the entity class to inspect
	 * @return the deleted field column name, or {@code null}
	 */
	public static String getDeletedFieldName(final Class<?> clazz) {
		try {
			for (final Field elem : getAllInstanceFields(clazz)) {
				if (AnnotationTools.isDeletedField(elem)) {
					return AnnotationTools.getFieldNameRaw(elem);
				}
			}
		} catch (final Exception ex) {
			LOGGER.error("Failed to get deleted field name: {}", ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Returns the field name pair for the update timestamp field, or {@code null} if none exists.
	 * Note: the update field cannot be renamed with {@link OptionRenameColumn}.
	 *
	 * @param clazz the entity class to inspect
	 * @return the update timestamp field name pair, or {@code null}
	 */
	public static FieldName getUpdatedFieldName(final Class<?> clazz) {
		try {
			for (final Field elem : getAllInstanceFields(clazz)) {
				if (AnnotationTools.isUpdateAtField(elem)) {
					return AnnotationTools.getFieldName(elem, null);
				}
			}
		} catch (final Exception ex) {
			LOGGER.error("Failed to get updated field name: {}", ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Returns the {@code @Id}-annotated field from the class, or {@code null} if none.
	 *
	 * @param clazz the class to inspect
	 * @return the ID field, or {@code null}
	 */
	public static Field getIdField(final Class<?> clazz) {
		try {
			for (final Field field : getAllInstanceFields(clazz)) {
				if (AnnotationTools.isIdField(field)) {
					return field;
				}
			}
		} catch (final Exception ex) {
			LOGGER.error("Failed to get id field: {}", ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Checks whether the class has an instance field with the given Java name.
	 *
	 * @param clazz the class to inspect
	 * @param name the field name to look for
	 * @return {@code true} if a field with that name exists
	 */
	public static boolean hasFieldsName(final Class<?> clazz, final String name) {
		for (final Field field : getAllInstanceFields(clazz)) {
			if (field.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the DB field names of all editable (non-generic) fields for the class.
	 *
	 * @param clazz the class to inspect
	 * @return a list of editable field names
	 */
	public static List<String> getEditableFieldsNames(final Class<?> clazz) {
		return getFieldsNamesFilter(clazz, false);
	}

	/**
	 * Returns the DB field names of all instance fields for the class.
	 *
	 * @param clazz the class to inspect
	 * @return a list of all field names
	 */
	public static List<String> getAllFieldsNames(final Class<?> clazz) {
		return getFieldsNamesFilter(clazz, true);
	}

	/**
	 * Returns DB field names for the class, optionally filtering out generic (system-managed) fields.
	 *
	 * @param clazz the class to inspect
	 * @param full if {@code true}, include all fields; if {@code false}, exclude generic fields
	 * @return a list of field names
	 */
	public static List<String> getFieldsNamesFilter(final Class<?> clazz, final boolean full) {
		final List<String> out = new ArrayList<>();
		for (final Field field : getAllInstanceFields(clazz)) {
			if (!full && AnnotationTools.isGenericField(field)) {
				continue;
			}
			out.add(AnnotationTools.getFieldNameRaw(field));
		}
		return out;
	}

	/**
	 * Checks whether a field is a system-managed "generic" field (primary key, timestamps, deleted).
	 *
	 * @param elem the field to inspect
	 * @return {@code true} if the field is a generic system-managed field
	 */
	public static boolean isGenericField(final Field elem) {
		return AnnotationTools.isPrimaryKey(elem) //
				|| AnnotationTools.isCreatedAtField(elem) //
				|| AnnotationTools.isUpdateAtField(elem) //
				|| AnnotationTools.isDeletedField(elem);
	}

	/**
	 * Returns the {@code @Id}-annotated field from the class, or {@code null} if none.
	 *
	 * @param clazz the class to inspect
	 * @return the ID field, or {@code null}
	 */
	public static Field getFieldOfId(final Class<?> clazz) {
		for (final Field field : getAllInstanceFields(clazz)) {
			if (AnnotationTools.isIdField(field)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Returns the field whose DB column name matches the given name, or {@code null} if none.
	 *
	 * @param clazz the class to inspect
	 * @param name the DB column name to search for
	 * @return the matching field, or {@code null}
	 */
	public static Field getFieldNamed(final Class<?> clazz, final String name) {
		for (final Field field : getAllInstanceFields(clazz)) {
			if (AnnotationTools.getFieldNameRaw(field).equals(name)) {
				return field;
			}
		}
		return null;
	}

}

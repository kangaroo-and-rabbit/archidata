package org.kar.archidata.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.options.OverrideTableName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;

public class AnnotationTools {
	static final Logger LOGGER = LoggerFactory.getLogger(AnnotationTools.class);

	public static String getTableName(final Class<?> clazz, final QueryOptions options) throws Exception {
		if (options != null) {
			final List<OverrideTableName> data = options.get(OverrideTableName.class);
			if (data.size() == 1) {
				return data.get(0).getName();
			}
		}
		return AnnotationTools.getTableName(clazz);
	}

	public static String getTableName(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Table.class);
		if (annotation.length == 0) {
			// when no annotation is detected, then the table name is the class name
			return element.getSimpleName();
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Table on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((Table) annotation[0]).name();
		if (tmp == null) {
			return element.getSimpleName();
		}
		return tmp;
	}

	public static boolean getSchemaReadOnly(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return false;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Schema on " + element.getClass().getCanonicalName());
		}
		return ((Schema) annotation[0]).readOnly();
	}

	public static String getSchemaExample(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Schema on " + element.getClass().getCanonicalName());
		}
		return ((Schema) annotation[0]).example();
	}

	public static String getSchemaDescription(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Schema on " + element.getClass().getCanonicalName());
		}
		return ((Schema) annotation[0]).description();
	}

	public static String getSchemaDescription(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Schema on " + element.getClass().getCanonicalName());
		}
		return ((Schema) annotation[0]).description();
	}

	public static String getComment(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(DataComment.class);
		if (annotation.length == 0) {
			return getSchemaDescription(element);
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @DataComment on " + element.getClass().getCanonicalName());
		}
		return ((DataComment) annotation[0]).value();
	}

	public static String getDefault(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(DefaultValue.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @DataDefault on " + element.getClass().getCanonicalName());
		}
		return ((DefaultValue) annotation[0]).value();
	}

	public static ManyToOne getManyToOne(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(ManyToOne.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @ManyToOne on " + element.getClass().getCanonicalName());
		}
		return (ManyToOne) annotation[0];
	}

	public static DataJson getDataJson(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(DataJson.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @ManyToOne on " + element.getClass().getCanonicalName());
		}
		return (DataJson) annotation[0];
	}

	public static Long getConstraintsMax(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Max.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Size on " + element.getClass().getCanonicalName());
		}
		return ((Max) annotation[0]).value();
	}

	public static Long getConstraintsMin(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Min.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Size on " + element.getClass().getCanonicalName());
		}
		return ((Min) annotation[0]).value();
	}

	public static int getLimitSize(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return 255;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		final int length = ((Column) annotation[0]).length();
		return length <= 0 ? 0 : length;
	}

	public static Size getConstraintsSize(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Size.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Size on " + element.getClass().getCanonicalName());
		}
		return (Size) annotation[0];
	}

	public static String getConstraintsPattern(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Pattern.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Pattern on " + element.getClass().getCanonicalName());
		}
		return ((Pattern) annotation[0]).regexp();
	}

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

	public static String getFieldName(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return element.getName();
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		final String name = ((Column) annotation[0]).name();
		if (name.isBlank()) {
			return element.getName();
		}
		return name;
	}

	public static boolean getColumnNotNull(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		return !((Column) annotation[0]).nullable();
	}

	public static boolean getConstraintsNotNull(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(NotNull.class);
		if (annotation.length == 0) {
			return false;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @NotNull on " + element.getClass().getCanonicalName());
		}
		return true;
	}

	public static Field getPrimaryKeyField(final Class<?> clazz) throws Exception {
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (AnnotationTools.isPrimaryKey(field)) {
				return field;
			}
		}
		return null;
	}

	public static boolean isPrimaryKey(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Id.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	public static boolean isUnique(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		return ((Column) annotation[0]).unique();
	}

	public static GenerationType getStrategy(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(GeneratedValue.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception(
					"Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		return ((GeneratedValue) annotation[0]).strategy();
	}

	public static boolean isDeletedField(final Field element) throws Exception {
		return element.getDeclaredAnnotationsByType(DataDeleted.class).length != 0;
	}

	public static boolean isCreatedAtField(final Field element) throws Exception {
		return element.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
	}

	public static boolean isUpdateAtField(final Field element) throws Exception {
		return element.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
	}

	public static boolean isdefaultNotRead(final Field element) throws Exception {
		return element.getDeclaredAnnotationsByType(DataNotRead.class).length != 0;
	}

	public static boolean isIdField(final Field element) throws Exception {
		return element.getDeclaredAnnotationsByType(Id.class).length != 0;
	}

	public static String getDeletedFieldName(final Class<?> clazz) throws Exception {
		try {
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isDeletedField(elem)) {
					return AnnotationTools.getFieldName(elem);
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static String getUpdatedFieldName(final Class<?> clazz) throws Exception {
		try {
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isUpdateAtField(elem)) {
					return AnnotationTools.getFieldName(elem);
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static Field getIdField(final Class<?> clazz) {
		try {
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isIdField(field)) {
					return field;
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static List<String> getFieldsNames(final Class<?> clazz) throws Exception {
		return getFieldsNamesFilter(clazz, false);
	}

	public static List<String> getAllFieldsNames(final Class<?> clazz) throws Exception {
		return getFieldsNamesFilter(clazz, true);
	}

	private static List<String> getFieldsNamesFilter(final Class<?> clazz, final boolean full) throws Exception {
		final List<String> out = new ArrayList<>();
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (!full && AnnotationTools.isGenericField(field)) {
				continue;
			}
			out.add(AnnotationTools.getFieldName(field));
		}
		return out;
	}

	public static boolean isGenericField(final Field elem) throws Exception {
		return AnnotationTools.isPrimaryKey(elem) || AnnotationTools.isCreatedAtField(elem)
				|| AnnotationTools.isUpdateAtField(elem) || AnnotationTools.isDeletedField(elem);
	}

	public static Field getFieldOfId(final Class<?> clazz) throws Exception {
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (AnnotationTools.isIdField(field)) {
				return field;
			}
		}
		return null;
	}

	public static Field getFieldNamed(final Class<?> clazz, final String name) throws Exception {
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (AnnotationTools.getFieldName(field).equals(name)) {
				return field;
			}
		}
		return null;
	}

}

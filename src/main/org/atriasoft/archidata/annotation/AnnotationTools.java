package org.atriasoft.archidata.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.checker.Checker;
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

public class AnnotationTools {
	static final Logger LOGGER = LoggerFactory.getLogger(AnnotationTools.class);

	public static <TYPE extends Annotation> TYPE get(final Parameter param, final Class<TYPE> clazz) {
		final TYPE[] annotations = param.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations[0];
	}

	public static <TYPE extends Annotation> TYPE[] gets(final Parameter param, final Class<TYPE> clazz) {
		final TYPE[] annotations = param.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations;
	}

	public static <TYPE extends Annotation> TYPE get(final Field element, final Class<TYPE> clazz) {
		final TYPE[] annotations = element.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations[0];
	}

	public static <TYPE extends Annotation> TYPE[] gets(final Field element, final Class<TYPE> clazz) {
		final TYPE[] annotations = element.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations;
	}

	public static <TYPE extends Annotation> TYPE get(final Class<?> classObject, final Class<TYPE> clazz) {
		final TYPE[] annotations = classObject.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations[0];
	}

	public static <TYPE extends Annotation> TYPE[] gets(final Class<?> classObject, final Class<TYPE> clazz) {
		final TYPE[] annotations = classObject.getDeclaredAnnotationsByType(clazz);

		if (annotations.length == 0) {
			return null;
		}
		return annotations;
	}

	// For SQL declaration table Name
	public static String getTableName(final Class<?> clazz, final QueryOptions options) throws DataAccessException {
		if (options != null) {
			final List<OverrideTableName> data = options.get(OverrideTableName.class);
			if (data.size() == 1) {
				return data.get(0).getName();
			}
		}
		return AnnotationTools.getTableName(clazz);
	}

	// For SQL declaration table Name
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

	public static CollectionItemNotNull getCollectionItemNotNull(final Field element) {
		return get(element, CollectionItemNotNull.class);
	}

	public static CollectionItemUnique getCollectionItemUnique(final Field element) {
		return get(element, CollectionItemUnique.class);
	}

	public static CollectionNotEmpty getCollectionNotEmpty(final Field element) {
		return get(element, CollectionNotEmpty.class);
	}

	public static String getSchemaExample(final Class<?> element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Schema) annotation[0]).example();
	}

	public static String getSchemaDescription(final Class<?> element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Schema) annotation[0]).description();
	}

	public static String getSchemaDescription(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Schema.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Schema) annotation[0]).description();
	}

	public static String getDefault(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(DefaultValue.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((DefaultValue) annotation[0]).value();
	}

	public static ManyToOne getManyToOne(final Field element) {
		return get(element, ManyToOne.class);
	}

	public static ManyToMany getManyToMany(final Field element) {
		return get(element, ManyToMany.class);
	}

	public static OneToMany getOneToMany(final Field element) {
		return get(element, OneToMany.class);
	}

	public static DataJson getDataJson(final Field element) {
		return get(element, DataJson.class);
	}

	public static Checker[] getConstraintsCheckers(final Field element) {
		return gets(element, Checker.class);
	}

	public static DecimalMin getConstraintsDecimalMin(final Field element) {
		return get(element, DecimalMin.class);
	}

	public static DecimalMax getConstraintsDecimalMax(final Field element) {
		return get(element, DecimalMax.class);
	}

	public static Max getConstraintsMax(final Field element) {
		return get(element, Max.class);
	}

	public static Min getConstraintsMin(final Field element) {
		return get(element, Min.class);
	}

	public static int getLimitSize(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return 255;
		}
		final int length = ((Column) annotation[0]).length();
		return length <= 0 ? 0 : length;
	}

	public static Size getConstraintsSize(final Field element) {
		return get(element, Size.class);
	}

	public static Pattern getConstraintsPattern(final Field element) {
		return get(element, Pattern.class);
	}

	public static Email getConstraintsEmail(final Field element) {
		return get(element, Email.class);
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

	public record FieldName(
			String inStruct,
			String inTable) {};

	public static FieldName getFieldName(final Field element, final QueryOptions options) {
		final String inStructName = getFieldNameRaw(element);
		String inTableName = inStructName;
		if (options != null) {
			final List<OptionRenameColumn> renamesColumn = options.get(OptionRenameColumn.class);
			for (final OptionRenameColumn rename : renamesColumn) {
				if (rename.columnName.equals(inStructName)) {
					inTableName = rename.colomnNewName;
					LOGGER.trace("Detect overwrite of column name '{}' => '{}'", inStructName, inTableName);
					break;
				}
			}
		}
		return new FieldName(inStructName, inTableName);
	}

	public static boolean getColumnNotNull(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		return !((Column) annotation[0]).nullable();
	}

	public static boolean getNullable(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Nullable.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	public static boolean getConstraintsNotNull(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(NotNull.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	public static Field getPrimaryKeyField(final Class<?> clazz) {
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

	public static boolean isPrimaryKey(final Field element) {
		final Annotation[] annotationSQL = element.getDeclaredAnnotationsByType(Id.class);
		if (annotationSQL.length > 0) {
			return true;
		}
		return false;
	}

	public static boolean isUnique(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		return ((Column) annotation[0]).unique();
	}

	public static GenerationType getStrategy(final Field element) {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(GeneratedValue.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((GeneratedValue) annotation[0]).strategy();
	}

	public static boolean isDeletedField(final Field element) {
		return element.getDeclaredAnnotationsByType(DataDeleted.class).length != 0;
	}

	public static boolean isCreatedAtField(final Field element) {
		return element.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
	}

	public static boolean isUpdateAtField(final Field element) {
		return element.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
	}

	public static boolean isDefaultNotRead(final Field element) {
		return element.getDeclaredAnnotationsByType(DataNotRead.class).length != 0;
	}

	public static boolean isIdField(final Field element) {
		return element.getDeclaredAnnotationsByType(Id.class).length != 0;
	}

	// Note: delete field can not be renamed with OptionRenameColumn
	public static String getDeletedFieldName(final Class<?> clazz) {
		try {
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isDeletedField(elem)) {
					return AnnotationTools.getFieldNameRaw(elem);
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	// Note: update field can not be renamed with OptionRenameColumn
	public static FieldName getUpdatedFieldName(final Class<?> clazz) {
		try {
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isUpdateAtField(elem)) {
					return AnnotationTools.getFieldName(elem, null);
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

	public static boolean hasFieldsName(final Class<?> clazz, final String name) {
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (field.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public static List<String> getEditableFieldsNames(final Class<?> clazz) {
		return getFieldsNamesFilter(clazz, false);
	}

	public static List<String> getAllFieldsNames(final Class<?> clazz) {
		return getFieldsNamesFilter(clazz, true);
	}

	public static List<String> getFieldsNamesFilter(final Class<?> clazz, final boolean full) {
		final List<String> out = new ArrayList<>();
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (!full && AnnotationTools.isGenericField(field)) {
				continue;
			}
			out.add(AnnotationTools.getFieldNameRaw(field));
		}
		return out;
	}

	public static boolean isGenericField(final Field elem) {
		return AnnotationTools.isPrimaryKey(elem) || AnnotationTools.isCreatedAtField(elem)
				|| AnnotationTools.isUpdateAtField(elem) || AnnotationTools.isDeletedField(elem);
	}

	public static Field getFieldOfId(final Class<?> clazz) {
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

	public static Field getFieldNamed(final Class<?> clazz, final String name) {
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (AnnotationTools.getFieldNameRaw(field).equals(name)) {
				return field;
			}
		}
		return null;
	}

}

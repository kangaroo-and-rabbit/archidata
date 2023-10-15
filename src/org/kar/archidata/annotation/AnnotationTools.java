package org.kar.archidata.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

public class AnnotationTools {
	static final Logger LOGGER = LoggerFactory.getLogger(AnnotationTools.class);
	
	public static String getTableName(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Table.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @SQLTableName on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((Table) annotation[0]).name();
		if (tmp == null) {
			return null;
		}
		return tmp;
	}
	
	public static String getComment(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(SQLComment.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @SQLComment on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((SQLComment) annotation[0]).value();
		if (tmp == null) {
			return null;
		}
		return tmp;
	}
	
	public static String getDefault(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(SQLDefault.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @SQLDefault on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((SQLDefault) annotation[0]).value();
		if (tmp == null) {
			return null;
		}
		return tmp;
	}
	
	public static Integer getLimitSize(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @SQLLimitSize on " + element.getClass().getCanonicalName());
		}
		return ((Column) annotation[0]).length();
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
			throw new Exception("Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		String name = ((Column) annotation[0]).name();
		if (name.isBlank()) {
			return element.getName();
		}
		return name;
	}
	
	public static boolean getNotNull(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		return !((Column) annotation[0]).nullable();
	}
	
	public static boolean isPrimaryKey(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Column.class);
		if (annotation.length == 0) {
			return false;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		return ((Column) annotation[0]).unique();
	}
	
	public static GenerationType getStrategy(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(GeneratedValue.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @Column on " + element.getClass().getCanonicalName());
		}
		return ((GeneratedValue) annotation[0]).strategy();
	}
	
}

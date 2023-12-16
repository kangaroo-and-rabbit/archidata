package org.kar.archidata.dataAccess.options;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.Size;

public class CheckJPA<T> implements CheckFunctionInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckJPA.class);
	private final Class<?> clazz;

	/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
	public interface CheckInterface<K> {
		/** This function implementation is design to check if the updated class is valid of not for insertion
		 * @param data The object that might be injected.
		 * @param filterValue List of fields that might be check. If null, then all column must be checked.
		 * @throws Exception Exception is generate if the data are incorrect. */
		void check(K data) throws Exception;
	}

	private Map<String, List<CheckInterface<T>>> checking = null;

	protected void add(final String field, final CheckInterface<T> checkFunction) {
		List<CheckInterface<T>> actions = this.checking.get(field);
		if (actions == null) {
			actions = new ArrayList<>();
			this.checking.put(field, actions);
		}
		actions.add(checkFunction);
	}

	public CheckJPA(final Class<T> clazz) {
		this.clazz = clazz;
	}

	public void initialize() throws Exception {
		if (this.checking != null) {
			return;
		}
		try {
			this.checking = new HashMap<>();
			// create Table:
			final List<String> primaryKeys = new ArrayList<>();
			for (final Field field : this.clazz.getFields()) {
				final String fieldName = AnnotationTools.getFieldName(field);
				if (AnnotationTools.isPrimaryKey(field)) {
					add(fieldName, (final T data) -> {
						throw new InputException(fieldName, "This is a '@Id' (primaryKey) ==> can not be change");
					});
				}
				if (AnnotationTools.getConstraintsNotNull(field)) {
					add(fieldName, (final T data) -> {
						if (field.get(data) == null) {
							throw new InputException(fieldName, "Can not be null");
						}
					});
				}
				if (AnnotationTools.isCreatedAtField(field) || AnnotationTools.isUpdateAtField(field)) {
					add(fieldName, (final T data) -> {
						throw new InputException(fieldName, "It is forbidden to change this field");
					});
				}

				final Class<?> type = field.getType();
				if (type == Long.class || type == long.class) {

				} else if (type == Integer.class || type == int.class) {

				} else if (type == Boolean.class || type == boolean.class) {

				} else if (type == Float.class || type == float.class) {

				} else if (type == Double.class || type == double.class) {

				} else if (type == Date.class || type == Timestamp.class) {

				} else if (type == LocalDate.class) {

				} else if (type == LocalTime.class) {

				} else if (type == String.class) {
					final int maxSizeString = AnnotationTools.getLimitSize(field);
					if (maxSizeString > 0) {
						add(fieldName, (final T data) -> {
							final Object elem = field.get(data);
							if (elem == null) {
								return;
							}
							final String elemTyped = (String) elem;
							if (elemTyped.length() > maxSizeString) {
								throw new InputException(fieldName, "Too long size must be <= " + maxSizeString);
							}
						});
					}
					final Size limitSize = AnnotationTools.getConstraintsSize(field);
					if (limitSize != null) {
						add(fieldName, (final T data) -> {
							final Object elem = field.get(data);
							if (elem == null) {
								return;
							}
							final String elemTyped = (String) elem;
							if (elemTyped.length() > limitSize.max()) {
								throw new InputException(fieldName, "Too long size (constraints) must be <= " + limitSize.max());
							}
							if (elemTyped.length() < limitSize.min()) {
								throw new InputException(fieldName, "Too small size (constraints) must be >= " + limitSize.max());
							}
						});
					}
					final String patternString = AnnotationTools.getConstraintsPattern(field);
					if (patternString != null) {
						final Pattern pattern = Pattern.compile(patternString);
						add(fieldName, (final T data) -> {
							final Object elem = field.get(data);
							if (elem == null) {
								return;
							}
							final String elemTyped = (String) elem;
							if (!pattern.matcher(elemTyped).find()) {
								throw new InputException(fieldName, "does not match the required pattern (constraints) must be '" + patternString + "'");
							}
						});
					}
				} else if (type == JsonValue.class) {

				} else if (type.isEnum()) {
					// nothing to do.
				}
				// keep this is last ==> take more time...
				if (AnnotationTools.isUnique(field)) {
					// Create the request ...
					add(fieldName, (final T data) -> {
						final Object other = DataAccess.getWhere(this.clazz, new QueryCondition(fieldName, "==", field.get(data)));
						if (other != null) {
							throw new InputException(fieldName, "Name already exist in the DB");
						}
					});
				}

			}
		} catch (final Exception ex) {
			this.checking = null;
			throw ex;
		}
	}

	@Override
	public void check(final Object data, final List<String> filterValue) throws Exception {
		initialize();
		if (!(this.clazz.isAssignableFrom(data.getClass()))) {
			throw new DataAccessException("Incompatatyble type of Object" + data.getClass().getCanonicalName());
		}
		@SuppressWarnings("unchecked")
		final T dataCasted = (T) data;
		for (final String filter : filterValue) {
			final List<CheckInterface<T>> actions = this.checking.get(filter);
			if (actions == null) {
				continue;
			}
			for (final CheckInterface<T> action : actions) {
				action.check(dataCasted);
			}
		}
		checkTyped(dataCasted, filterValue);
	}

	public void checkTyped(final T data, final List<String> filterValue) throws Exception {
		// nothing to do ...
	}
}

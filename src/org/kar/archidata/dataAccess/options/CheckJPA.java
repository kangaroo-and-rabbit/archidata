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

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;

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
	
	protected void add(String field, CheckInterface<T> checkFunction) {
		List<CheckInterface<T>> actions = this.checking.get(field);
		if (actions == null) {
			actions = new ArrayList<>();
			this.checking.put(field, actions);
		}
		actions.add(checkFunction);
	}
	
	public CheckJPA(Class<T> clazz) {
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
				String fieldName = AnnotationTools.getFieldName(field);
				if (AnnotationTools.isPrimaryKey(field)) {
					add(fieldName, (T data) -> {
						throw new InputException(fieldName, "This is a '@Id' (primaryKey) ==> can not be change");
					});
				}
				if (AnnotationTools.getNotNull(field)) {
					add(fieldName, (T data) -> {
						if (field.get(data) == null) {
							throw new InputException(fieldName, "Can not be null");
						}
					});
				}
				if (AnnotationTools.isCreatedAtField(field) || AnnotationTools.isUpdateAtField(field)) {
					add(fieldName, (T data) -> {
						throw new InputException(fieldName, "It is forbidden to change this field");
					});
				}
				
				Class<?> type = field.getType();
				if (type == Long.class || type == long.class) {
					
				} else if (type == Integer.class || type == int.class) {
					
				} else if (type == Boolean.class || type == boolean.class) {
					
				} else if (type == Float.class || type == float.class) {
					
				} else if (type == Double.class || type == double.class) {
					
				} else if (type == Date.class || type == Timestamp.class) {
					
				} else if (type == LocalDate.class) {
					
				} else if (type == LocalTime.class) {
					
				} else if (type == String.class) {
					int maxSizeString = AnnotationTools.getLimitSize(field);
					
					if (maxSizeString > 0) {
						add(fieldName, (T data) -> {
							Object elem = field.get(data);
							if (elem == null) {
								return;
							}
							String elemTyped = (String) elem;
							if (elemTyped.length() > maxSizeString) {
								throw new InputException("name", "Too long size must be <= " + maxSizeString);
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
					add(fieldName, (T data) -> {
						final Object other = DataAccess.getWhere(this.clazz, new QueryCondition(fieldName, "==", field.get(data)));
						if (other != null) {
							throw new InputException(fieldName, "Name already exist in the DB");
						}
					});
				}
				
			}
		} catch (Exception ex) {
			this.checking = null;
			throw ex;
		}
	}
	
	@Override
	public void check(Object data, List<String> filterValue) throws Exception {
		initialize();
		if (!(this.clazz.isAssignableFrom(data.getClass()))) {
			throw new DataAccessException("Incompatatyble type of Object" + data.getClass().getCanonicalName());
		}
		@SuppressWarnings("unchecked")
		T dataCasted = (T) data;
		for (String filter : filterValue) {
			List<CheckInterface<T>> actions = this.checking.get(filter);
			if (actions == null) {
				continue;
			}
			for (CheckInterface<T> action : actions) {
				action.check(dataCasted);
			}
		}
		checkTyped(dataCasted, filterValue);
	}
	
	public void checkTyped(T data, List<String> filterValue) throws Exception {
		// nothing to do ...
	}
}

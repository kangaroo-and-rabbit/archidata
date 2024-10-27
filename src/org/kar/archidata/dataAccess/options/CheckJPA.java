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
import java.util.UUID;
import java.util.regex.Pattern;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Size;

public class CheckJPA<T> implements CheckFunctionInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckJPA.class);
	private final Class<?> clazz;

	/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
	public interface CheckInterface<K> {
		/** This function implementation is design to check if the updated class is valid of not for insertion
		 * @param baseName Base of the name input that is displayed in exception generated.
		 * @param data The object that might be injected.
		 * @param modifiedValue List of fields that modification is requested.
		 * @param options Some query option that the checker can need to generate basic check.
		 * @throws Exception Exception is generate if the data are incorrect. */
		void check(final String baseName, final K data, List<String> modifiedValue, final QueryOptions options)
				throws Exception;
	}

	protected Map<String, List<CheckInterface<T>>> checking = null;

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
				final String fieldName = field.getName(); // AnnotationTools.getFieldName(field);
				if (AnnotationTools.isPrimaryKey(field)) {
					add(fieldName,
							(
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								throw new InputException(baseName + fieldName,
										"This is a '@Id' (primaryKey) ==> can not be change");
							});
				}
				if (AnnotationTools.getConstraintsNotNull(field)) {
					add(fieldName,
							(
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								if (field.get(data) == null) {
									throw new InputException(baseName + fieldName, "Can not be null");
								}
							});
				}
				if (AnnotationTools.isCreatedAtField(field) || AnnotationTools.isUpdateAtField(field)) {
					add(fieldName,
							(
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								throw new InputException(baseName + fieldName, "It is forbidden to change this field");
							});
				}

				final Class<?> type = field.getType();
				if (type == Long.class || type == long.class) {
					final Long maxValue = AnnotationTools.getConstraintsMax(field);
					if (maxValue != null) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Long elemTyped = (Long) elem;
									if (elemTyped > maxValue) {
										throw new InputException(baseName + fieldName,
												"Value too height max: " + maxValue);
									}
								});
					}
					final Long minValue = AnnotationTools.getConstraintsMin(field);
					if (minValue != null) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Long elemTyped = (Long) elem;
									if (elemTyped < minValue) {
										throw new InputException(baseName + fieldName,
												"Value too Low min: " + minValue);
									}
								});
					}
					final ManyToOne annotationManyToOne = AnnotationTools.getManyToOne(field);
					if (annotationManyToOne != null && annotationManyToOne.targetEntity() != null) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final List<ConditionChecker> condCheckers = options.get(ConditionChecker.class);
									final Condition conditionCheck = condCheckers.isEmpty() ? null
											: condCheckers.get(0).toCondition();
									final long count = DataAccess.count(annotationManyToOne.targetEntity(), elem,
											conditionCheck);
									if (count == 0) {
										throw new InputException(baseName + fieldName,
												"Foreign element does not exist in the DB:" + elem);
									}
								});
					}

				} else if (type == Integer.class || type == int.class) {
					final Long maxValueRoot = AnnotationTools.getConstraintsMax(field);
					if (maxValueRoot != null) {
						final int maxValue = maxValueRoot.intValue();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Integer elemTyped = (Integer) elem;
									if (elemTyped > maxValue) {
										throw new InputException(baseName + fieldName,
												"Value too height max: " + maxValue);
									}
								});
					}
					final Long minValueRoot = AnnotationTools.getConstraintsMin(field);
					if (minValueRoot != null) {
						final int minValue = minValueRoot.intValue();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Integer elemTyped = (Integer) elem;
									if (elemTyped < minValue) {
										throw new InputException(baseName + fieldName,
												"Value too Low min: " + minValue);
									}
								});
					}
					final ManyToOne annotationManyToOne = AnnotationTools.getManyToOne(field);
					if (annotationManyToOne != null && annotationManyToOne.targetEntity() != null) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final long count = DataAccess.count(annotationManyToOne.targetEntity(), elem);
									if (count == 0) {
										throw new InputException(baseName + fieldName,
												"Foreign element does not exist in the DB:" + elem);
									}
								});
					}
				} else if (type == UUID.class) {
					final ManyToOne annotationManyToOne = AnnotationTools.getManyToOne(field);
					if (annotationManyToOne != null && annotationManyToOne.targetEntity() != null) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final long count = DataAccess.count(annotationManyToOne.targetEntity(), elem);
									if (count == 0) {
										throw new InputException(baseName + fieldName,
												"Foreign element does not exist in the DB:" + elem);
									}
								});
					}
				} else if (type == Boolean.class || type == boolean.class) {

				} else if (type == Float.class || type == float.class) {
					final Long maxValueRoot = AnnotationTools.getConstraintsMax(field);
					if (maxValueRoot != null) {
						final float maxValue = maxValueRoot.floatValue();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Float elemTyped = (Float) elem;
									if (elemTyped > maxValue) {
										throw new InputException(baseName + fieldName,
												"Value too height max: " + maxValue);
									}
								});
					}
					final Long minValueRoot = AnnotationTools.getConstraintsMin(field);
					if (minValueRoot != null) {
						final float minValue = minValueRoot.floatValue();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Float elemTyped = (Float) elem;
									if (elemTyped < minValue) {
										throw new InputException(baseName + fieldName,
												"Value too Low min: " + minValue);
									}
								});
					}
				} else if (type == Double.class || type == double.class) {
					final Long maxValueRoot = AnnotationTools.getConstraintsMax(field);
					if (maxValueRoot != null) {
						final double maxValue = maxValueRoot.doubleValue();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Double elemTyped = (Double) elem;
									if (elemTyped > maxValue) {
										throw new InputException(baseName + fieldName,
												"Value too height max: " + maxValue);
									}
								});
					}
					final Long minValueRoot = AnnotationTools.getConstraintsMin(field);
					if (minValueRoot != null) {
						final double minValue = minValueRoot.doubleValue();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Double elemTyped = (Double) elem;
									if (elemTyped < minValue) {
										throw new InputException(baseName + fieldName,
												"Value too Low min: " + minValue);
									}
								});
					}
				} else if (type == Date.class || type == Timestamp.class) {

				} else if (type == LocalDate.class) {

				} else if (type == LocalTime.class) {

				} else if (type == String.class) {
					final int maxSizeString = AnnotationTools.getLimitSize(field);
					if (maxSizeString > 0) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final String elemTyped = (String) elem;
									if (elemTyped.length() > maxSizeString) {
										throw new InputException(baseName + fieldName,
												"Too long size must be <= " + maxSizeString);
									}
								});
					}
					final Size limitSize = AnnotationTools.getConstraintsSize(field);
					if (limitSize != null) {
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final String elemTyped = (String) elem;
									if (elemTyped.length() > limitSize.max()) {
										throw new InputException(baseName + fieldName,
												"Too long size (constraints) must be <= " + limitSize.max());
									}
									if (elemTyped.length() < limitSize.min()) {
										throw new InputException(baseName + fieldName,
												"Too small size (constraints) must be >= " + limitSize.min());
									}
								});
					}
					final String patternString = AnnotationTools.getConstraintsPattern(field);
					if (patternString != null) {
						final Pattern pattern = Pattern.compile(patternString);
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final String elemTyped = (String) elem;
									if (!pattern.matcher(elemTyped).find()) {
										throw new InputException(baseName + fieldName,
												"does not match the required pattern (constraints) must be '"
														+ patternString + "'");
									}
								});
					}
				} else if (type == JsonValue.class) {
					final DataJson jsonAnnotation = AnnotationTools.getDataJson(field);
					if (jsonAnnotation != null && jsonAnnotation.checker() != CheckFunctionVoid.class) {
						// Here if we have an error it crash at start and no new instance after creation...
						final CheckFunctionInterface instance = jsonAnnotation.checker().getDeclaredConstructor()
								.newInstance();
						add(fieldName,
								(
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									instance.checkAll(baseName + fieldName + ".", field.get(data), options);
								});
					}
				} else if (type.isEnum()) {
					// nothing to do.
				}
				// keep this is last ==> take more time...
				if (AnnotationTools.isUnique(field)) {
					// Create the request ...
					add(fieldName,
							(
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								final List<ConditionChecker> condCheckers = options.get(ConditionChecker.class);
								Object other = null;
								if (condCheckers.isEmpty()) {
									other = DataAccess.getWhere(this.clazz,
											new Condition(new QueryCondition(fieldName, "==", field.get(data))));
								} else {
									other = DataAccess.getWhere(this.clazz,
											new Condition(new QueryCondition(fieldName, "==", field.get(data))),
											condCheckers.get(0).toCondition());
								}
								if (other != null) {
									throw new InputException(baseName + fieldName, "Name already exist in the DB");
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
	public void check(
			final String baseName,
			final Object data,
			final List<String> modifiedValue,
			final QueryOptions options) throws Exception {
		if (this.checking == null) {
			initialize();
		}
		if (!(this.clazz.isAssignableFrom(data.getClass()))) {
			throw new DataAccessException("Incompatatyble type of Object" + data.getClass().getCanonicalName());
		}
		@SuppressWarnings("unchecked")
		final T dataCasted = (T) data;
		for (final String filter : modifiedValue) {
			final List<CheckInterface<T>> actions = this.checking.get(filter);
			if (actions == null) {
				continue;
			}
			for (final CheckInterface<T> action : actions) {
				action.check(baseName, dataCasted, modifiedValue, options);
			}
		}
		checkTyped(dataCasted, modifiedValue, options);
	}

	public void checkTyped(final T data, final List<String> modifiedValue, final QueryOptions options)
			throws Exception {
		// nothing to do ...
	}
}

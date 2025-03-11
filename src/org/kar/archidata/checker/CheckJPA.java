package org.kar.archidata.checker;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.checker.CheckForeignKey;
import org.kar.archidata.annotation.checker.Checker;
import org.kar.archidata.annotation.checker.CollectionItemNotNull;
import org.kar.archidata.annotation.checker.CollectionItemUnique;
import org.kar.archidata.annotation.checker.CollectionNotEmpty;
import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.options.CheckFunctionInterface;
import org.kar.archidata.dataAccess.options.CheckFunctionVoid;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.dataAccess.options.ConditionChecker;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.ManyToOne;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class CheckJPA<T> implements CheckFunctionInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckJPA.class);
	private final Class<?> clazz;

	/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
	public interface CheckInterface<K> {
		/** This function implementation is design to check if the updated class is valid of not for insertion
		 * @param ioDb Access on the Data-Base
		 * @param baseName Base of the name input that is displayed in exception generated.
		 * @param data The object that might be injected.
		 * @param modifiedValue List of fields that modification is requested.
		 * @param options Some query option that the checker can need to generate basic check.
		 * @throws Exception Exception is generate if the data are incorrect. */
		void check(
				final DBAccess ioDb,
				final String baseName,
				final K data,
				List<String> modifiedValue,
				final QueryOptions options) throws Exception;
	}

	public interface CheckInterfaceExtended<K> {
		/** This function implementation is design to check if the updated class is valid of not for insertion
		 * @param ioDb Access on the Data-Base
		 * @param baseName Base of the name input that is displayed in exception generated.
		 * @param data The object that might be injected.
		 * @param modifiedValue List of fields that modification is requested.
		 * @param options Some query option that the checker can need to generate basic check.
		 * @param checkMode Check mode of the data normal, create or update.
		 * @throws Exception Exception is generate if the data are incorrect. */
		void check(
				final DBAccess ioDb,
				final String baseName,
				final K data,
				List<String> modifiedValue,
				final QueryOptions options,
				TypeOfCheck checkMode) throws Exception;
	}

	protected Map<String, List<CheckInterface<T>>> checking = null;
	protected Map<String, List<CheckInterfaceExtended<T>>> checkingExtended = null;

	protected void checkFieldExist(final String field) throws DataAccessException {
		if (!AnnotationTools.hasFieldsName(this.clazz, field)) {
			LOGGER.error("Try to add a JPA Filter on an inexistant Field: '{}' not in {}", field,
					AnnotationTools.getAllFieldsNames(this.clazz));
			throw new DataAccessException("Try to add a JPA Filter on an inexistant Field: '" + field + "' not in "
					+ AnnotationTools.getAllFieldsNames(this.clazz));
		}
	}

	protected void add(final String field, final CheckInterface<T> checkFunction) throws DataAccessException {
		checkFieldExist(field);
		List<CheckInterface<T>> actions = this.checking.get(field);
		if (actions == null) {
			actions = new ArrayList<>();
			this.checking.put(field, actions);
		}
		actions.add(checkFunction);
	}

	protected void add(final String field, final CheckInterfaceExtended<T> checkFunction) throws DataAccessException {
		checkFieldExist(field);
		List<CheckInterfaceExtended<T>> actions = this.checkingExtended.get(field);
		if (actions == null) {
			actions = new ArrayList<>();
			this.checkingExtended.put(field, actions);
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
				// move throught all annotation and find the generic annotation:
				for (final Annotation annotation : field.getAnnotations()) {
					final Constraint parentAnnotation = annotation.getClass().getAnnotation(Constraint.class);
					if (parentAnnotation == null) {
						continue;
					}
					Objects.requireNonNull(parentAnnotation.validatedBy());
					add(fieldName,
							(
									final DBAccess ioDb,
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								final Object value = field.get(data);
								Constructor<?> constructor;
								ConstraintValidator<?, ?> validator = null;
								try {
									constructor = parentAnnotation.validatedBy()[0].getConstructor();
									validator = (ConstraintValidator<?, ?>) constructor.newInstance(annotation,
											field.getType());
									//validator.initialize(annotation);
									LOGGER.warn("Not implemented ...");
								} catch (NoSuchMethodException | SecurityException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (final InstantiationException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (final IllegalAccessException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (final IllegalArgumentException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (final InvocationTargetException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							});
				}
				if (AnnotationTools.getConstraintsNotNull(field)) {
					add(fieldName,
							(
									final DBAccess ioDb,
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
									final DBAccess ioDb,
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								throw new InputException(baseName + fieldName, "It is forbidden to change this field");
							});
				}

				final Class<?> type = field.getType();
				if (type == Long.class || type == long.class) {
					final DecimalMax maxValueDecimal = AnnotationTools.getConstraintsDecimalMax(field);
					if (maxValueDecimal != null) {
						final long maxValue = Long.parseLong(maxValueDecimal.value());
						final boolean inclusive = maxValueDecimal.inclusive();
						final String exceptionComment = "Value too height max=" + maxValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Long elemTyped = (Long) elem;
									if (inclusive) {
										if (elemTyped > maxValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped >= maxValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final DecimalMin minValueDecimal = AnnotationTools.getConstraintsDecimalMin(field);
					if (minValueDecimal != null) {
						final long minValue = Long.parseLong(minValueDecimal.value());
						final boolean inclusive = minValueDecimal.inclusive();
						final String exceptionComment = "Value too low min=" + minValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Long elemTyped = (Long) elem;
									if (inclusive) {
										if (elemTyped < minValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped <= minValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Max maxValue = AnnotationTools.getConstraintsMax(field);
					if (maxValue != null) {
						final Long maxValueTmp = maxValue.value();
						final String exceptionComment = "Value too height max=" + maxValueTmp + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Long elemTyped = (Long) elem;
									if (elemTyped > maxValueTmp) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Min minValue = AnnotationTools.getConstraintsMin(field);
					if (minValue != null) {
						final Long minValueTmp = minValue.value();
						final String exceptionComment = "Value too low min=" + minValueTmp + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Long elemTyped = (Long) elem;
									if (elemTyped < minValueTmp) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
				} else if (type == Integer.class || type == int.class) {
					final DecimalMax maxValueDecimal = AnnotationTools.getConstraintsDecimalMax(field);
					if (maxValueDecimal != null) {
						final int maxValue = Integer.parseInt(maxValueDecimal.value());
						final boolean inclusive = maxValueDecimal.inclusive();
						final String exceptionComment = "Value too height max=" + maxValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Integer elemTyped = (Integer) elem;
									if (inclusive) {
										if (elemTyped > maxValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped >= maxValue) {

										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final DecimalMin minValueDecimal = AnnotationTools.getConstraintsDecimalMin(field);
					if (minValueDecimal != null) {
						final int minValue = Integer.parseInt(minValueDecimal.value());
						final boolean inclusive = minValueDecimal.inclusive();
						final String exceptionComment = "Value too low min=" + minValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Integer elemTyped = (Integer) elem;
									if (inclusive) {
										if (elemTyped < minValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped <= minValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Max maxValueRoot = AnnotationTools.getConstraintsMax(field);
					if (maxValueRoot != null) {
						final int maxValue = (int) maxValueRoot.value();
						final String exceptionComment = "Value too height max=" + maxValue + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
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
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Min minValueRoot = AnnotationTools.getConstraintsMin(field);
					if (minValueRoot != null) {
						final int minValue = (int) minValueRoot.value();
						final String exceptionComment = "Value too low min=" + minValue + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
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
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
				} else if (type == Boolean.class || type == boolean.class) {

				} else if (type == Float.class || type == float.class) {
					final DecimalMax maxValueDecimal = AnnotationTools.getConstraintsDecimalMax(field);
					if (maxValueDecimal != null) {
						final float maxValue = Float.parseFloat(maxValueDecimal.value());
						final boolean inclusive = maxValueDecimal.inclusive();
						final String exceptionComment = "Value too height max=" + maxValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Float elemTyped = (Float) elem;
									if (inclusive) {
										if (elemTyped > maxValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped >= maxValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final DecimalMin minValueDecimal = AnnotationTools.getConstraintsDecimalMin(field);
					if (minValueDecimal != null) {
						final float minValue = Float.parseFloat(minValueDecimal.value());
						final boolean inclusive = minValueDecimal.inclusive();
						final String exceptionComment = "Value too low min=" + minValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Float elemTyped = (Float) elem;
									if (inclusive) {
										if (elemTyped < minValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped <= minValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Max maxValueRoot = AnnotationTools.getConstraintsMax(field);
					if (maxValueRoot != null) {
						final float maxValue = maxValueRoot.value();
						final String exceptionComment = "Value too height max=" + maxValue + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
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
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Min minValueRoot = AnnotationTools.getConstraintsMin(field);
					if (minValueRoot != null) {
						final float minValue = minValueRoot.value();
						final String exceptionComment = "Value too low min=" + minValue + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
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
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
				} else if (type == Double.class || type == double.class) {
					final DecimalMax maxValueDecimal = AnnotationTools.getConstraintsDecimalMax(field);
					if (maxValueDecimal != null) {
						final double maxValue = Float.parseFloat(maxValueDecimal.value());
						final boolean inclusive = maxValueDecimal.inclusive();
						final String exceptionComment = "Value too height max=" + maxValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Double elemTyped = (Double) elem;
									if (inclusive) {
										if (elemTyped > maxValue) {
											throw new InputException(baseName + fieldName, exceptionComment);
										}
									} else if (elemTyped >= maxValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final DecimalMin minValueDecimal = AnnotationTools.getConstraintsDecimalMin(field);
					if (minValueDecimal != null) {
						final double minValue = Float.parseFloat(minValueDecimal.value());
						final boolean inclusive = minValueDecimal.inclusive();
						final String exceptionComment = "Value too low min=" + minValue
								+ (inclusive ? " (inclusive)" : " (exclusive)");
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object elem = field.get(data);
									if (elem == null) {
										return;
									}
									final Double elemTyped = (Double) elem;
									if (inclusive) {
										if (elemTyped < minValue) {
											throw new InputException(baseName + fieldName,
													"Value too Low min: " + minValue);
										}
									} else if (elemTyped <= minValue) {
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Max maxValueRoot = AnnotationTools.getConstraintsMax(field);
					if (maxValueRoot != null) {
						final double maxValue = maxValueRoot.value();
						final String exceptionComment = "Value too height max=" + maxValue + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
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
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
					final Min minValueRoot = AnnotationTools.getConstraintsMin(field);
					if (minValueRoot != null) {
						final double minValue = minValueRoot.value();
						final String exceptionComment = "Value too low min=" + minValue + " (inclusive)";
						add(fieldName,
								(
										final DBAccess ioDb,
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
										throw new InputException(baseName + fieldName, exceptionComment);
									}
								});
					}
				} else if (type == Date.class || type == Timestamp.class) {

				} else if (type == LocalDate.class) {

				} else if (type == LocalTime.class) {

				} else if (type == String.class) {
					final Size limitSize = AnnotationTools.getConstraintsSize(field);
					if (limitSize != null) {
						add(fieldName,
								(
										final DBAccess ioDb,
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
					final jakarta.validation.constraints.Pattern patternString = AnnotationTools
							.getConstraintsPattern(field);
					if (patternString != null && patternString.regexp() != null) {
						final Pattern pattern = Pattern.compile(patternString.regexp());
						add(fieldName,
								(
										final DBAccess ioDb,
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
												"does not match the required pattern (constraints) must be '" + pattern
														+ "'");
									}
								});
					}
					if (AnnotationTools.getConstraintsEmail(field) != null) {
						final String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
						final Pattern pattern = Pattern.compile(emailPattern);
						add(fieldName,
								(
										final DBAccess ioDb,
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
												"does not match the required pattern[email] (constraints) must be '"
														+ emailPattern + "'");
									}
								});
					}
				} else if (type.isEnum()) {
					// nothing to do.
				}
				final Checker[] checkers = AnnotationTools.getConstraintsCheckers(field);
				if (checkers != null) {
					for (final Checker checker : checkers) {
						if (checker == null || checker.value() == CheckFunctionVoid.class) {
							continue;
						}
						final CheckFunctionInterface checkerInstance = checker.value().getDeclaredConstructor()
								.newInstance();
						if (Collection.class.isAssignableFrom(field.getType())) {
							add(fieldName,
									(
											final DBAccess ioDb,
											final String baseName,
											final T data,
											final List<String> modifiedValue,
											final QueryOptions options) -> {
										// get the field of the specific element
										final Object tmpData = field.get(data);
										// It is not the objective of this element to check if it is authorize to set NULL
										if (tmpData == null) {
											return;
										}
										final Collection<?> tmpCollection = (Collection<?>) tmpData;
										final Object[] elements = tmpCollection.toArray();
										for (int iii = 0; iii < elements.length; iii++) {
											if (elements[iii] != null) {
												checkerInstance.check(ioDb, baseName + fieldName + '[' + iii + "].",
														elements[iii], null, options);
											}
										}
									});
						} else {
							add(fieldName,
									(
											final DBAccess ioDb,
											final String baseName,
											final T data,
											final List<String> modifiedValue,
											final QueryOptions options) -> {
										// get the field of the specific element
										final Object tmpData = field.get(data);
										// It is not the objective of this element to check if it is authorize to set NULL
										if (tmpData == null) {
											return;
										}
										checkerInstance.check(ioDb, baseName + fieldName + '.', tmpData, null, options);
									});
						}
					}
				}
				final CheckForeignKey foreighKey = AnnotationTools.get(field, CheckForeignKey.class);
				if (foreighKey != null) {
					if (Collection.class.isAssignableFrom(field.getType())) {
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									// get the field of the specific element
									final Object tmpData = field.get(data);
									// It is not the objective of this element to check if it is authorize to set NULL
									if (tmpData == null) {
										return;
									}
									final List<ConditionChecker> condCheckers = options.get(ConditionChecker.class);
									final Condition conditionCheck = condCheckers.isEmpty() ? null
											: condCheckers.get(0).toCondition();
									final Collection<?> tmpCollection = (Collection<?>) tmpData;
									final Object[] elements = tmpCollection.toArray();
									for (int iii = 0; iii < elements.length; iii++) {
										if (elements[iii] == null) {
											continue;
										}
										final Long count = ioDb.count(foreighKey.target(), elements[iii],
												conditionCheck);
										if (count != 1) {
											throw new InputException(baseName + fieldName + '[' + iii + ']',
													"Foreign-key does not exist in the DB:" + elements[iii]);

										}
									}
								});
					} else {
						add(fieldName,
								(
										final DBAccess ioDb,
										final String baseName,
										final T data,
										final List<String> modifiedValue,
										final QueryOptions options) -> {
									final Object tmpData = field.get(data);
									if (tmpData == null) {
										return;
									}
									final List<ConditionChecker> condCheckers = options.get(ConditionChecker.class);
									final Condition conditionCheck = condCheckers.isEmpty() ? null
											: condCheckers.get(0).toCondition();
									final Long count = ioDb.count(foreighKey.target(), tmpData, conditionCheck);
									if (count != 1) {
										throw new InputException(baseName + fieldName,
												"Foreign-key does not exist in the DB:" + tmpData);
									}
								});
					}
				}
				// check if we really want to keep it ...
				final ManyToOne annotationManyToOne = AnnotationTools.getManyToOne(field);
				if (annotationManyToOne != null && annotationManyToOne.targetEntity() != null) {
					add(fieldName,
							(
									final DBAccess ioDb,
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
								final long count = ioDb.count(annotationManyToOne.targetEntity(), elem, conditionCheck);
								if (count == 0) {
									throw new InputException(baseName + fieldName,
											"Foreign element does not exist in the DB:" + elem);
								}
							});
				}
				final CollectionItemUnique collectionUnique = AnnotationTools.getCollectionItemUnique(field);
				if (collectionUnique != null) {
					if (!Collection.class.isAssignableFrom(field.getType())) {
						throw new DataAccessException(
								"Request @CollectionItemUnique on a non collection field: '" + fieldName + "'");
					}
					add(fieldName,
							(
									final DBAccess ioDb,
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								final Object tmpData = field.get(data);
								if (tmpData == null) {
									return;
								}
								final Collection<?> tmpCollection = (Collection<?>) tmpData;
								final Set<Object> uniqueValues = new HashSet<>(tmpCollection);
								if (uniqueValues.size() != tmpCollection.size()) {
									throw new InputException(baseName + fieldName,
											"Cannot insert multiple times the same elements");
								}
							});
				}
				final CollectionItemNotNull collectionNotNull = AnnotationTools.getCollectionItemNotNull(field);
				if (collectionNotNull != null) {
					if (!Collection.class.isAssignableFrom(field.getType())) {
						throw new DataAccessException(
								"Request @CollectionItemNotNull on a non collection field: '" + fieldName + "'");
					}
					add(fieldName,
							(
									final DBAccess ioDb,
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								final Object tmpData = field.get(data);
								if (tmpData == null) {
									return;
								}

								final Collection<?> tmpCollection = (Collection<?>) tmpData;
								final Object[] elements = tmpCollection.toArray();
								for (int iii = 0; iii < elements.length; iii++) {
									if (elements[iii] == null) {
										throw new InputException(baseName + fieldName + '[' + iii + ']',
												"Collection can not contain NULL item");
									}
								}
							});
				}
				final CollectionNotEmpty collectionNotEmpty = AnnotationTools.getCollectionNotEmpty(field);
				if (collectionNotEmpty != null) {
					if (!Collection.class.isAssignableFrom(field.getType())) {
						throw new DataAccessException(
								"Request @collectionNotEmpty on a non collection field: '" + fieldName + "'");
					}
					add(fieldName,
							(
									final DBAccess ioDb,
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								final Object tmpData = field.get(data);
								if (tmpData == null) {
									return;
								}
								final Collection<?> tmpCollection = (Collection<?>) tmpData;
								if (tmpCollection.isEmpty()) {
									throw new InputException(baseName + fieldName, "Collection can not be empty");
								}
							});
				}
				// keep this is last ==> take more time...
				if (AnnotationTools.isUnique(field)) {
					// Create the request ...
					add(fieldName,
							(
									final DBAccess ioDb,
									final String baseName,
									final T data,
									final List<String> modifiedValue,
									final QueryOptions options) -> {
								final List<ConditionChecker> condCheckers = options.get(ConditionChecker.class);
								Object other = null;
								if (condCheckers.isEmpty()) {
									other = ioDb.getWhere(this.clazz,
											new Condition(new QueryCondition(fieldName, "==", field.get(data))));
								} else {
									other = ioDb.getWhere(this.clazz,
											new Condition(new QueryCondition(fieldName, "==", field.get(data))),
											condCheckers.get(0).toCondition());
								}
								if (other != null) {
									throw new InputException(baseName + fieldName,
											"The field is already exist in the DB");
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
			final DBAccess ioDb,
			final String baseName,
			final Object data,
			List<String> modifiedValue,
			final QueryOptions options,
			final TypeOfCheck checkMode) throws Exception {
		if (this.checking == null) {
			initialize();
		}
		if (modifiedValue == null) {
			modifiedValue = AnnotationTools.getAllFieldsNames(this.clazz);
		}
		if (!(this.clazz.isAssignableFrom(data.getClass()))) {
			throw new DataAccessException("Incompatatyble type of Object" + data.getClass().getCanonicalName());
		}
		@SuppressWarnings("unchecked")
		final T dataCasted = (T) data;
		for (final String filter : modifiedValue) {
			final List<CheckInterface<T>> actions = this.checking.get(filter);
			if (actions != null) {
				for (final CheckInterface<T> action : actions) {
					action.check(ioDb, baseName, dataCasted, modifiedValue, options);
				}
			}
			final List<CheckInterfaceExtended<T>> actionsExtended = this.checkingExtended.get(filter);
			if (actionsExtended != null) {
				for (final CheckInterfaceExtended<T> action : actionsExtended) {
					action.check(ioDb, baseName, dataCasted, modifiedValue, options, checkMode);
				}
			}
		}
		checkTyped(dataCasted, modifiedValue, options);
	}

	public void checkTyped(final T data, final List<String> modifiedValue, final QueryOptions options)
			throws Exception {
		// nothing to do ...
	}

}

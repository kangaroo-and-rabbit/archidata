package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.apiGenerator.ApiAccessLimitation;
import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.annotation.checker.CheckForeignKey;
import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.annotation.apiGenerator.ApiNotNull;
import org.atriasoft.archidata.annotation.apiGenerator.ApiOptionalIsNullable;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.AnnotationCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Represents an object (class/record) model extracted from a Java class for API generation.
 *
 * <p>Handles field introspection, inheritance resolution, validation constraints,
 * and access limitation annotations for each property of the modeled class.
 */
public class ClassObjectModel extends ClassModel {
	/** Logger instance. */
	static final Logger LOGGER = LoggerFactory.getLogger(ClassObjectModel.class);

	/**
	 * Constructs a new object model for the given class.
	 * @param clazz the Java class to model
	 */
	public ClassObjectModel(final Class<?> clazz) {
		this.originClasses = clazz;
		final ApiGenerationMode tmp = AnnotationTools.get(clazz, ApiGenerationMode.class);
		if (tmp != null) {
			this.apiGenerationMode = tmp;
		}
	}

	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append("ClassObjectModel [");
		out.append(this.originClasses.getCanonicalName());
		out.append("]");
		return out.toString();
	}

	private static boolean hasJsonIncludeNonNull(final Class<?> clazz) {
		final JsonInclude annotation = clazz.getDeclaredAnnotation(JsonInclude.class);
		return annotation != null && annotation.value() == JsonInclude.Include.NON_NULL;
	}

	private static boolean isPropertyFromSuperClass(final Class<?> model, final String propertyName) {
		final Class<?> superClass = model.getSuperclass();
		if (superClass == null) {
			return false;
		}
		try {
			final org.atriasoft.archidata.bean.ClassModel superBeanModel = org.atriasoft.archidata.bean.ClassModel
					.of(superClass);
			return superBeanModel.getProperty(propertyName) != null;
		} catch (final IntrospectionException e) {
			LOGGER.trace("Catch error introspecting parent class: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Describes a single field/property of a modeled class, including its type,
	 * validation constraints, and access limitations.
	 *
	 * @param name the field name
	 * @param model the class model representing the field type
	 * @param linkClass the linked entity class model for remote ID references (e.g., {@code List<UUID>})
	 * @param checkForeignKey the foreign key annotation specifying the target entity
	 * @param comment the description from {@code @ApiDoc} or {@code @Schema}
	 * @param example the example value from {@code @ApiDoc} or {@code @Schema}
	 * @param stringSize the {@code @Size} constraint for strings
	 * @param min the {@code @Min} constraint for numeric values
	 * @param max the {@code @Max} constraint for numeric values
	 * @param decimalMin the {@code @DecimalMin} constraint
	 * @param decimalMax the {@code @DecimalMax} constraint
	 * @param pattern the {@code @Pattern} constraint
	 * @param email the {@code @Email} constraint
	 * @param accessLimitation the API access limitation annotation
	 * @param apiReadOnly the API read-only annotation
	 * @param apiNotNull the API not-null annotation
	 * @param annotationNotNull the Jakarta {@code @NotNull} annotation
	 * @param annotationNull the Jakarta {@code @Null} annotation
	 * @param nullable whether the field is annotated as nullable
	 */
	public record FieldProperty(
			String name,
			ClassModel model,
			ClassModel linkClass,
			CheckForeignKey checkForeignKey,
			String comment,
			String example,
			Size stringSize,
			Min min,
			Max max,
			DecimalMin decimalMin,
			DecimalMax decimalMax,
			Pattern pattern,
			Email email,
			ApiAccessLimitation accessLimitation,
			ApiReadOnly apiReadOnly,
			ApiNotNull apiNotNull,
			NotNull annotationNotNull,
			Null annotationNull,
			Boolean nullable) {

		/**
		 * Constructs a FieldProperty with all parameters, applying defaults for access limitation and example.
		 * @param name the field name
		 * @param model the class model representing the field type
		 * @param linkClass the linked entity class model
		 * @param checkForeignKey the foreign key annotation
		 * @param comment the field description
		 * @param example the example value (auto-generated if {@code null})
		 * @param stringSize the size constraint
		 * @param min the minimum value constraint
		 * @param max the maximum value constraint
		 * @param decimalMin the decimal minimum constraint
		 * @param decimalMax the decimal maximum constraint
		 * @param pattern the pattern constraint
		 * @param email the email constraint
		 * @param accessLimitation the access limitation (defaults to all-access if {@code null})
		 * @param apiReadOnly the read-only annotation
		 * @param apiNotNull the API not-null annotation
		 * @param annotationNotNull the Jakarta not-null annotation
		 * @param annotationNull the Jakarta null annotation
		 * @param nullable whether the field is nullable
		 */
		public FieldProperty(//
				final String name, //
				final ClassModel model, //
				final ClassModel linkClass, //
				final CheckForeignKey checkForeignKey, //
				final String comment, //
				final String example, //
				final Size stringSize, //
				final Min min, //
				final Max max, //
				final DecimalMin decimalMin, //
				final DecimalMax decimalMax, //
				final Pattern pattern, //
				final Email email, //
				final ApiAccessLimitation accessLimitation, //
				final ApiReadOnly apiReadOnly, //
				final ApiNotNull apiNotNull, //
				final NotNull annotationNotNull, //
				final Null annotationNull, //
				final Boolean nullable) {
			this.name = name;
			this.model = model;
			this.linkClass = linkClass;
			this.checkForeignKey = checkForeignKey;
			this.comment = comment;
			if (example != null) {
				this.example = example;
			} else {
				this.example = ExampleGenerator.generate(name, model, stringSize, min, max, decimalMin, decimalMax,
						pattern, email);
			}
			this.stringSize = stringSize;
			this.decimalMin = decimalMin;
			this.decimalMax = decimalMax;
			this.pattern = pattern;
			this.email = email;
			this.min = min;
			this.max = max;
			if (accessLimitation == null) {
				this.accessLimitation = AnnotationCreator.createAnnotation(ApiAccessLimitation.class);
			} else {
				this.accessLimitation = accessLimitation;
			}
			this.apiReadOnly = apiReadOnly;
			this.annotationNotNull = annotationNotNull;
			this.apiNotNull = apiNotNull;
			this.annotationNull = annotationNull;
			this.nullable = nullable;

		}

		// -- PropertyDescriptor-based helpers (bean introspection) --

		private static String getSchemaDescription(final PropertyDescriptor property) {
			final ApiDoc apiDoc = property.getAnnotation(ApiDoc.class);
			if (apiDoc != null && !apiDoc.description().isEmpty()) {
				return apiDoc.description();
			}
			// Fallback to @Schema (deprecated)
			final Schema schema = property.getAnnotation(Schema.class);
			if (schema == null) {
				return null;
			}
			final String desc = schema.description();
			if (desc == null || desc.isEmpty()) {
				return null;
			}
			LOGGER.warn(
					"@Schema(description=...) on property '{}' is deprecated. Use @ApiDoc(description=...) instead.",
					property.getName());
			return desc;
		}

		private static String getSchemaExample(final PropertyDescriptor property) {
			final ApiDoc apiDoc = property.getAnnotation(ApiDoc.class);
			if (apiDoc != null && !apiDoc.example().isEmpty()) {
				return apiDoc.example();
			}
			// Fallback to @Schema (deprecated)
			final Schema schema = property.getAnnotation(Schema.class);
			if (schema == null) {
				return null;
			}
			final String example = schema.example();
			if (example == null || example.isEmpty()) {
				return null;
			}
			LOGGER.warn("@Schema(example=...) on property '{}' is deprecated. Use @ApiDoc(example=...) instead.",
					property.getName());
			return example;
		}

		private static Class<?> getSubModelIfExist2(final PropertyDescriptor property) {
			final ManyToOne manyToOne = property.getAnnotation(ManyToOne.class);
			if (manyToOne != null) {
				if (manyToOne.targetEntity() != null && manyToOne.targetEntity() != void.class) {
					return manyToOne.targetEntity();
				}
				return null;
			}
			final ManyToMany manyToMany = property.getAnnotation(ManyToMany.class);
			if (manyToMany != null) {
				if (manyToMany.targetEntity() != null && manyToMany.targetEntity() != void.class) {
					return manyToMany.targetEntity();
				}
				return null;
			}
			final OneToMany oneToMany = property.getAnnotation(OneToMany.class);
			if (oneToMany != null) {
				if (oneToMany.targetEntity() != null && oneToMany.targetEntity() != void.class) {
					return oneToMany.targetEntity();
				}
				return null;
			}
			return null;
		}

		private static ClassModel getSubModelIfExist(final PropertyDescriptor property, final ModelGroup previous)
				throws IOException {
			final Class<?> tmp = getSubModelIfExist2(property);
			if (tmp == null) {
				return null;
			}
			return ClassModel.getModel(tmp, previous);
		}

		/**
		 * Constructs a FieldProperty from a bean PropertyDescriptor (supports POJO, Record, Bean).
		 * @param property the bean property descriptor
		 * @param previous the model group for resolving referenced types
		 * @throws DataAccessException if data access fails during introspection
		 * @throws IOException if type resolution fails
		 */
		public FieldProperty(final PropertyDescriptor property, final ModelGroup previous)
				throws DataAccessException, IOException {
			this(property.getName(), //
					ClassModel.getModel(property.getTypeInfo().genericType(), previous), //
					getSubModelIfExist(property, previous), //
					property.getAnnotation(CheckForeignKey.class), //
					getSchemaDescription(property), //
					getSchemaExample(property), //
					property.getAnnotation(Size.class), //
					property.getAnnotation(Min.class), //
					property.getAnnotation(Max.class), //
					property.getAnnotation(DecimalMin.class), //
					property.getAnnotation(DecimalMax.class), //
					property.getAnnotation(Pattern.class), //
					property.getAnnotation(Email.class), //
					property.getAnnotation(ApiAccessLimitation.class), //
					property.getAnnotation(ApiReadOnly.class), //
					property.getAnnotation(ApiNotNull.class), //
					property.getAnnotation(NotNull.class), //
					property.getAnnotation(Null.class), //
					property.hasAnnotation(Nullable.class));
		}

		// -- Legacy Field-based helpers (kept for backward compatibility) --

		@Deprecated
		private static Class<?> getSubModelIfExist2(final Field field) {
			final ManyToOne manyToOne = AnnotationTools.getManyToOne(field);
			if (manyToOne != null) {
				if (manyToOne.targetEntity() != null && manyToOne.targetEntity() != void.class) {
					return manyToOne.targetEntity();
				}
				return null;
			}
			final ManyToMany manyToMany = AnnotationTools.getManyToMany(field);
			if (manyToMany != null) {
				if (manyToMany.targetEntity() != null && manyToMany.targetEntity() != void.class) {
					return manyToMany.targetEntity();
				}
				return null;
			}
			final OneToMany oneToMany = AnnotationTools.getOneToMany(field);
			if (oneToMany != null) {
				if (oneToMany.targetEntity() != null && oneToMany.targetEntity() != void.class) {
					return oneToMany.targetEntity();
				}
				return null;
			}
			return null;
		}

		@Deprecated
		private static ClassModel getSubModelIfExist(final Field field, final ModelGroup previous) throws IOException {
			final Class<?> tmp = getSubModelIfExist2(field);
			if (tmp == null) {
				return null;
			}
			return ClassModel.getModel(tmp, previous);
		}

		/**
		 * Constructs a FieldProperty from a legacy Field (kept for backward compatibility).
		 * @param field the Java field to extract metadata from
		 * @param previous the model group for resolving referenced types
		 * @throws DataAccessException if data access fails during introspection
		 * @throws IOException if type resolution fails
		 * @deprecated use {@link #FieldProperty(PropertyDescriptor, ModelGroup)} instead
		 */
		@Deprecated
		public FieldProperty(final Field field, final ModelGroup previous) throws DataAccessException, IOException {
			this(field.getName(), //
					ClassModel.getModel(field.getGenericType(), previous), //
					getSubModelIfExist(field, previous), //
					AnnotationTools.get(field, CheckForeignKey.class), //
					AnnotationTools.getSchemaDescription(field), //
					AnnotationTools.getSchemaExample(field), //
					AnnotationTools.getConstraintsSize(field), //
					AnnotationTools.getConstraintsMin(field), //
					AnnotationTools.getConstraintsMax(field), //
					AnnotationTools.getConstraintsDecimalMin(field), //
					AnnotationTools.getConstraintsDecimalMax(field), //
					AnnotationTools.getConstraintsPattern(field), //
					AnnotationTools.getConstraintsEmail(field), //
					AnnotationTools.get(field, ApiAccessLimitation.class), //
					AnnotationTools.get(field, ApiReadOnly.class), //
					AnnotationTools.get(field, ApiNotNull.class), //
					AnnotationTools.get(field, NotNull.class), //
					AnnotationTools.get(field, Null.class), //
					AnnotationTools.getNullable(field));
		}

	}

	String name = "";
	boolean isPrimitive = false;
	boolean jsonIncludeNonNull = false;
	Class<?>[] optionalIsNullableGroups = null;
	String description = null;
	String example = null;
	ClassModel extendsClass = null;
	List<FieldProperty> fields = new ArrayList<>();

	/**
	 * Returns the fully qualified name of this model.
	 * @return the model name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns whether this model represents a Java primitive type.
	 * @return {@code true} if the modeled class is a primitive
	 */
	public boolean isPrimitive() {
		return this.isPrimitive;
	}

	/**
	 * Returns whether the modeled class is annotated with {@code @JsonInclude(NON_NULL)}.
	 * @return {@code true} if non-null JSON inclusion is enabled
	 */
	public boolean isJsonIncludeNonNull() {
		return this.jsonIncludeNonNull;
	}

	/**
	 * Returns the groups for which optional fields should be treated as nullable.
	 * @return the array of group classes, or {@code null} if not specified
	 */
	public Class<?>[] getOptionalIsNullableGroups() {
		return this.optionalIsNullableGroups;
	}

	/**
	 * Returns the description of this model from schema annotations.
	 * @return the description, or {@code null} if not specified
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Returns the example value of this model from schema annotations.
	 * @return the example, or {@code null} if not specified
	 */
	public String getExample() {
		return this.example;
	}

	/**
	 * Returns the model representing the parent class, if any.
	 * @return the parent class model, or {@code null} if no inheritance
	 */
	public ClassModel getExtendsClass() {
		return this.extendsClass;
	}

	/**
	 * Returns the list of field properties for this model.
	 * @return the field properties
	 */
	public List<FieldProperty> getFields() {
		return this.fields;
	}

	@Override
	public void analyze(final ModelGroup previous) throws Exception {
		if (this.analyzeDone) {
			return;
		}
		this.analyzeDone = true;
		final Class<?> clazz = this.originClasses;
		this.isPrimitive = clazz.isPrimitive();
		if (this.isPrimitive) {
			return;
		}
		final List<Class<?>> basicClass = List.of(Void.class, void.class, Character.class, char.class, Short.class,
				short.class, Integer.class, int.class, Long.class, long.class, Float.class, float.class, Double.class,
				double.class, Boolean.class, boolean.class, String.class, Object.class, Date.class, LocalDate.class,
				LocalTime.class, byte[].class);
		if (basicClass.contains(clazz)) {
			return;
		}

		// Detect @JsonInclude(NON_NULL) on this class or any parent class
		this.jsonIncludeNonNull = hasJsonIncludeNonNull(clazz);

		// Detect @ApiOptionalIsNullable on this class
		final ApiOptionalIsNullable optionalIsNullable = clazz.getAnnotation(ApiOptionalIsNullable.class);
		if (optionalIsNullable != null) {
			this.optionalIsNullableGroups = optionalIsNullable.groups();
		}

		// Use bean introspection (supports POJO, Record, Bean)
		LOGGER.trace("parse class: '{}'", clazz.getCanonicalName());

		final org.atriasoft.archidata.bean.ClassModel beanModel = org.atriasoft.archidata.bean.ClassModel.of(clazz);

		final List<String> alreadyAdded = new ArrayList<>();
		for (final PropertyDescriptor prop : beanModel.getProperties()) {
			// Only consider properties backed by a field (skip getter-only computed
			// properties from external classes like ObjectId.getDate()).
			if (prop.getField() == null) {
				LOGGER.trace("        SKIP (no field): '{}'", prop.getName());
				continue;
			}
			final String dataName = prop.getName();
			if (isPropertyFromSuperClass(clazz, dataName)) {
				LOGGER.trace("        SKIP:  '{}'", dataName);
				continue;
			}
			if (alreadyAdded.contains(dataName)) {
				LOGGER.trace("        SKIP2: '{}'", dataName);
				continue;
			}
			alreadyAdded.add(dataName);
			LOGGER.trace("        + '{}'", dataName);
			LOGGER.trace("Create type for: {} ==> {}", dataName, prop.getType());
			final FieldProperty fieldProperty = new FieldProperty(prop, previous);
			for (final ClassModel depModel : fieldProperty.model().getAlls()) {
				if (!this.dependencyModels.contains(depModel)) {
					this.dependencyModels.add(depModel);
				}
			}
			this.fields.add(fieldProperty);
		}
		this.name = clazz.getName();

		final String[] elems = this.name.split("\\$");
		if (elems.length == 2) {
			LOGGER.warn("Can have conflict in generation: {} (Remove class path) ==> {}", this.name, elems[1]);
			this.name = elems[1];
		}
		this.description = AnnotationTools.getSchemaDescription(clazz);
		this.example = AnnotationTools.getSchemaExample(clazz);
		final Class<?> parentClass = clazz.getSuperclass();
		// manage heritage
		if (parentClass != null && parentClass != Object.class && parentClass != Record.class) {
			this.extendsClass = previous.add(parentClass);
			this.dependencyModels.add(this.extendsClass);
		}
	}

	@Override
	public Set<ClassModel> getDependencyGroupModels() {
		return Set.of(this);
	}

	@Override
	public Set<ClassModel> getAlls() {
		return Set.of(this);
	}

	@Override
	public List<String> getReadOnlyFields() {
		final List<String> out = new ArrayList<>();
		for (final FieldProperty field : this.fields) {
			if (!field.accessLimitation().creatable() && !field.accessLimitation().updatable()) {
				out.add(field.name);
			}
		}
		if (this.extendsClass != null) {
			out.addAll(this.extendsClass.getReadOnlyFields());
		}
		return out;
	}

	@Override
	public List<String> getCreateFields() {
		final List<String> out = new ArrayList<>();
		for (final FieldProperty field : this.fields) {
			if (field.accessLimitation().creatable()) {
				out.add(field.name);
			}
		}
		if (this.extendsClass != null) {
			out.addAll(this.extendsClass.getCreateFields());
		}
		return out;
	}

	@Override
	public List<String> getUpdateFields() {
		final List<String> out = new ArrayList<>();
		for (final FieldProperty field : this.fields) {
			if (field.accessLimitation().updatable()) {
				out.add(field.name);
			}
		}
		if (this.extendsClass != null) {
			out.addAll(this.extendsClass.getUpdateFields());
		}
		return out;
	}

}

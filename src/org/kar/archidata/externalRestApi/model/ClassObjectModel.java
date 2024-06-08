package org.kar.archidata.externalRestApi.model;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.kar.archidata.annotation.AnnotationTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassObjectModel extends ClassModel {
	static final Logger LOGGER = LoggerFactory.getLogger(ClassObjectModel.class);

	public ClassObjectModel(final Class<?> clazz) {
		this.originClasses = clazz;
	}

	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append("ClassObjectModel [");
		out.append(this.originClasses.getCanonicalName());
		out.append("]");
		return out.toString();
	}

	private static boolean isFieldFromSuperClass(final Class<?> model, final String filedName) {
		final Class<?> superClass = model.getSuperclass();
		if (superClass == null) {
			return false;
		}
		for (final Field field : superClass.getFields()) {
			String name;
			try {
				name = AnnotationTools.getFieldName(field);
				if (filedName.equals(name)) {
					return true;
				}
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				LOGGER.trace("Catch error field name in parent create data table: {}", e.getMessage());
			}
		}
		return false;
	}

	public record FieldProperty(
			String name,
			ClassModel model,
			String comment,
			int limitSize,
			Boolean readOnly,
			Boolean notNull,
			Boolean columnNotNull,
			Boolean nullable) {

		public FieldProperty(final String name, final ClassModel model, final String comment, final int limitSize,
				final Boolean readOnly, final Boolean notNull, final Boolean columnNotNull, final Boolean nullable) {
			this.name = name;
			this.model = model;
			this.comment = comment;
			this.limitSize = limitSize;
			this.readOnly = readOnly;
			this.notNull = notNull;
			this.columnNotNull = columnNotNull;
			this.nullable = nullable;

		}

		public FieldProperty(final Field field, final ModelGroup previous) throws Exception {
			this(field.getName(), //
					ClassModel.getModel(field.getGenericType(), previous), //
					AnnotationTools.getComment(field), //
					AnnotationTools.getLimitSize(field), //
					AnnotationTools.getSchemaReadOnly(field), //
					AnnotationTools.getConstraintsNotNull(field), //
					AnnotationTools.getColumnNotNull(field), //
					AnnotationTools.getNullable(field));
		}

	}

	String name = "";
	boolean isPrimitive = false;
	String description = null;
	String example = null;
	ClassModel extendsClass = null;
	List<FieldProperty> fields = new ArrayList<>();

	public String getName() {
		return this.name;
	}

	public boolean isPrimitive() {
		return this.isPrimitive;
	}

	public String getDescription() {
		return this.description;
	}

	public String getExample() {
		return this.example;
	}

	public ClassModel getExtendsClass() {
		return this.extendsClass;
	}

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
		this.noWriteSpecificMode = AnnotationTools.getNoWriteSpecificMode(clazz);
		this.isPrimitive = clazz.isPrimitive();
		if (this.isPrimitive) {
			return;
		}
		final List<Class<?>> basicClass = List.of(Void.class, void.class, Character.class, char.class, Short.class,
				short.class, Integer.class, int.class, Long.class, long.class, Float.class, float.class, Double.class,
				double.class, Date.class, Timestamp.class, LocalDate.class, LocalTime.class);
		if (basicClass.contains(clazz)) {
			return;
		}

		// Local generation of class:
		LOGGER.trace("parse class: '{}'", clazz.getCanonicalName());
		final List<String> alreadyAdded = new ArrayList<>();
		for (final Field elem : clazz.getFields()) {
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			final String dataName = elem.getName();
			if (isFieldFromSuperClass(clazz, dataName)) {
				LOGGER.trace("        SKIP:  '{}'", elem.getName());
				continue;
			}
			if (alreadyAdded.contains(dataName)) {
				LOGGER.trace("        SKIP2: '{}'", elem.getName());
				continue;
			}
			alreadyAdded.add(dataName);
			LOGGER.trace("        + '{}'", elem.getName());
			LOGGER.trace("Create type for: {} ==> {}", AnnotationTools.getFieldName(elem), elem.getType());
			final FieldProperty porperty = new FieldProperty(elem, previous);
			for (final ClassModel depModel : porperty.model().getAlls()) {
				if (!this.dependencyModels.contains(depModel)) {
					this.dependencyModels.add(depModel);
				}
			}
			this.fields.add(new FieldProperty(elem, previous));
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
	public List<String> getReadOnlyField() {
		final List<String> out = new ArrayList<>();
		for (final FieldProperty field : this.fields) {
			if (field.readOnly()) {
				out.add(field.name);
			}
		}
		if (this.extendsClass != null) {
			out.addAll(this.extendsClass.getReadOnlyField());
		}
		return out;
	}

}

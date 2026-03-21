package org.atriasoft.archidata.externalRestApi.python;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Python class/type element for code generation.
 * Similar to TsClassElement but generates Pydantic models.
 */
public class PyClassElement {
	/** Logger for this class. */
	static final Logger LOGGER = LoggerFactory.getLogger(PyClassElement.class);

	/** Defines where a type is positioned in the generation hierarchy. */
	public enum DefinedPosition {
		/** Native Python type (str, int, etc.). */
		NATIVE,
		/** Basic wrapper types (UUID, ObjectId, etc.). */
		BASIC,
		/** Complex objects to generate. */
		NORMAL
	}

	/** The list of class models associated with this element. */
	public List<ClassModel> models;
	/** The Python type name for this element. */
	public String pyTypeName;
	/** The validator name for Pydantic validation. */
	public String validatorName;
	/** The file name (snake_case) for the generated Python file. */
	public String fileName = null;
	/** The position category of this type (native, basic, or normal). */
	public DefinedPosition nativeType = DefinedPosition.NORMAL;
	/** The set of requested parameter class models for this element. */
	public Set<ParameterClassModel> requestedModels = new HashSet<>();

	// Field constraints from Pydantic
	private final String fieldConstraints;

	/**
	 * Converts CamelCase to snake_case for Python file names.
	 * @param className the CamelCase class name to convert
	 * @return the snake_case version of the class name
	 */
	public static String toSnakeCase(final String className) {
		return className.replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2").toLowerCase();
	}

	/**
	 * Converts camelCase to snake_case for Python field names.
	 * @param fieldName the camelCase field name to convert
	 * @return the snake_case version of the field name
	 */
	public static String fieldToSnakeCase(final String fieldName) {
		return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}

	/**
	 * Constructor for native/basic types.
	 * @param model the list of class models associated with this element
	 * @param pyTypeName the Python type name
	 * @param validatorName the Pydantic validator name
	 * @param fieldConstraints the field constraint string for Pydantic
	 * @param nativeType the position category of this type
	 */
	public PyClassElement(final List<ClassModel> model, final String pyTypeName, final String validatorName,
			final String fieldConstraints, final DefinedPosition nativeType) {
		this.models = model;
		this.pyTypeName = pyTypeName;
		this.validatorName = validatorName;
		this.fieldConstraints = fieldConstraints;
		this.nativeType = nativeType;
		this.fileName = toSnakeCase(pyTypeName);
	}

	/**
	 * Constructor for complex object models.
	 * @param model the class model to wrap
	 */
	public PyClassElement(final ClassModel model) {
		this.models = List.of(model);
		this.pyTypeName = model.getOriginClasses().getSimpleName();
		this.validatorName = this.pyTypeName;
		this.fieldConstraints = null;
		this.fileName = toSnakeCase(this.pyTypeName);
	}

	/**
	 * Gets the Python type name for this element.
	 * @return the Python type name, or the simple class name if not set
	 */
	public String getTypeName() {
		if (this.pyTypeName != null) {
			return this.pyTypeName;
		}
		return this.models.get(0).getOriginClasses().getSimpleName();
	}

	/**
	 * Gets the Python type name and registers the parameter model as requested.
	 * @param model the parameter class model to register
	 * @return the Python type name
	 */
	public String getTypeName(final ParameterClassModel model) {
		this.requestedModels.add(model);
		if (this.pyTypeName != null) {
			return this.pyTypeName;
		}
		return model.getType();
	}

	/**
	 * Checks if the given class model is compatible with this element.
	 * @param model the class model to check
	 * @return true if the model is contained in this element's models
	 */
	public boolean isCompatible(final ClassModel model) {
		return this.models.contains(model);
	}

	/**
	 * Generates the file header with standard Python imports.
	 * @return the header string including future annotations and Pydantic imports
	 */
	public String getBaseHeader() {
		return """
				\"\"\"Model definitions (auto-generated code).\"\"\"

				from __future__ import annotations

				from typing import Annotated, Any

				from pydantic import BaseModel, Field

				""";
	}

	/**
	 * Generates Python code for an enum class definition.
	 * @param model the enum model to generate code for
	 * @param pyGroup the group registry for resolving type references
	 * @return the generated Python enum source code
	 */
	public String generateEnum(final ClassEnumModel model, final PyClassElementGroup pyGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"Enum definitions (auto-generated code).\"\"\"\n\n");
		out.append("from enum import Enum\n\n\n");

		out.append("class ");
		out.append(this.pyTypeName);
		out.append("(str, Enum):\n");
		out.append("    \"\"\"");
		out.append(this.pyTypeName);
		out.append(" enumeration.\"\"\"\n\n");

		for (final Entry<String, Object> elem : model.getListOfValues().entrySet()) {
			out.append("    ");
			out.append(elem.getKey().toUpperCase());
			out.append(" = ");
			if (elem.getValue() instanceof final Integer value) {
				out.append(value);
			} else {
				out.append("\"");
				out.append(elem.getValue());
				out.append("\"");
			}
			out.append("\n");
		}

		return out.toString();
	}

	/**
	 * Generates the imports section based on model dependencies.
	 * @param imports the import model tracking required imports
	 * @param pyGroup the group registry for resolving type references
	 * @return the generated Python import statements
	 */
	public String generateImports(final PyImportModel imports, final PyClassElementGroup pyGroup) {
		final StringBuilder out = new StringBuilder();
		final Map<String, Set<String>> fileToTypes = new TreeMap<>();

		for (final Entry<ClassModel, Set<PyImportModel.ImportType>> entry : imports.data.entrySet()) {
			final PyClassElement pyModel = pyGroup.find(entry.getKey());
			if (pyModel == null || pyModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}

			final String fileName = pyModel.fileName;
			fileToTypes.computeIfAbsent(fileName, k -> new HashSet<>());
			for (final PyImportModel.ImportType importType : entry.getValue()) {
				fileToTypes.get(fileName).add(pyModel.getTypeName());
			}
		}

		for (final Entry<String, Set<String>> entry : fileToTypes.entrySet()) {
			final List<String> types = new ArrayList<>(entry.getValue());
			Collections.sort(types);
			out.append("from .");
			out.append(entry.getKey());
			out.append(" import ");
			out.append(String.join(", ", types));
			out.append("\n");
		}

		return out.toString();
	}

	/**
	 * Checks if a field is optional based on validation constraints.
	 * @param field the field property to check
	 * @param isValid whether validation is active
	 * @param groups the validation groups to consider
	 * @return true if the field should be optional in generated code
	 */
	public boolean isOptionalField(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		if (field.apiNotNull() != null) {
			return !field.apiNotNull().value();
		}
		if (field.nullable()) {
			return true;
		}
		if (field.annotationNotNull() != null) {
			if ((field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0)
					&& isValid) {
				return false;
			}
		}
		if (field.model() instanceof ClassMapModel || field.model() instanceof ClassListModel) {
			return true;
		}
		if (field.model().getOriginClasses() == null || field.model().getOriginClasses().isPrimitive()) {
			return false;
		}
		return true;
	}

	/**
	 * Generates Pydantic Field constraints for a given field.
	 * @param field the field property containing constraint annotations
	 * @param originalName the original Java field name (for alias generation)
	 * @return the Pydantic Field() constraint string, or empty string if no constraints
	 */
	public String generateFieldConstraints(final FieldProperty field, final String originalName) {
		final StringBuilder constraints = new StringBuilder();
		final List<String> args = new ArrayList<>();
		final Class<?> clazz = field.model().getOriginClasses();

		// Add alias if name changed
		final String snakeName = fieldToSnakeCase(originalName);
		if (!snakeName.equals(originalName)) {
			args.add("alias=\"" + originalName + "\"");
		}

		// String constraints
		if (clazz == String.class) {
			if (field.stringSize() != null) {
				if (field.stringSize().min() > 0) {
					args.add("min_length=" + field.stringSize().min());
				}
				if (field.stringSize().max() != Integer.MAX_VALUE) {
					args.add("max_length=" + field.stringSize().max());
				}
			}
			if (field.pattern() != null) {
				args.add("pattern=r\"" + field.pattern().regexp() + "\"");
			}
		}

		// Number constraints
		if (clazz == short.class || clazz == Short.class || clazz == int.class || clazz == Integer.class
				|| clazz == long.class || clazz == Long.class || clazz == float.class || clazz == Float.class
				|| clazz == double.class || clazz == Double.class) {
			if (field.min() != null) {
				args.add("ge=" + field.min().value());
			}
			if (field.max() != null) {
				args.add("le=" + field.max().value());
			}
			if (field.decimalMin() != null) {
				if (field.decimalMin().inclusive()) {
					args.add("ge=" + field.decimalMin().value());
				} else {
					args.add("gt=" + field.decimalMin().value());
				}
			}
			if (field.decimalMax() != null) {
				if (field.decimalMax().inclusive()) {
					args.add("le=" + field.decimalMax().value());
				} else {
					args.add("lt=" + field.decimalMax().value());
				}
			}
		}

		if (args.isEmpty()) {
			return "";
		}
		constraints.append("Field(");
		constraints.append(String.join(", ", args));
		constraints.append(")");
		return constraints.toString();
	}

	/**
	 * Generates Python code for an object model with all variants (Read, Create, Update).
	 * @param model the object model to generate code for
	 * @param pyGroup the group registry for resolving type references
	 * @return the complete generated Python source code including imports and all variants
	 */
	public String generateObject(final ClassObjectModel model, final PyClassElementGroup pyGroup) {
		final PyImportModel imports = new PyImportModel();
		final StringBuilder outData = new StringBuilder();

		// Generate base/read model
		outData.append(generateObjectVariant(model, pyGroup, "", imports, true, false, false));

		// Generate Create variant (only creatable fields, required)
		outData.append("\n\n");
		outData.append(generateObjectVariant(model, pyGroup, "Create", imports, false, true, false));

		// Generate Update variant (only updatable fields, all optional)
		outData.append("\n\n");
		outData.append(generateObjectVariant(model, pyGroup, "Update", imports, false, false, true));

		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		final String importSection = generateImports(imports, pyGroup);
		if (!importSection.isEmpty()) {
			out.append(importSection);
			out.append("\n");
		}
		out.append(outData.toString());

		return out.toString();
	}

	/**
	 * Generate a specific variant of the object model.
	 */
	private String generateObjectVariant(
			final ClassObjectModel model,
			final PyClassElementGroup pyGroup,
			final String suffix,
			final PyImportModel imports,
			final boolean includeReadOnly,
			final boolean onlyCreatable,
			final boolean onlyUpdatable) {

		final StringBuilder out = new StringBuilder();
		final String className = this.pyTypeName + suffix;

		// Class declaration
		out.append("class ");
		out.append(className);
		out.append("(BaseModel):\n");

		// Docstring
		out.append("    \"\"\"");
		out.append(this.pyTypeName);
		if (!suffix.isEmpty()) {
			out.append(" - ");
			out.append(suffix);
			out.append(" variant");
		}
		out.append(".\"\"\"\n\n");

		boolean hasFields = false;

		// Handle inheritance
		if (model.getExtendsClass() != null && includeReadOnly) {
			// For read model with inheritance, we need to extend the parent
			// This is handled differently in Python - we include parent fields directly
		}

		// Generate fields
		for (final FieldProperty field : model.getFields()) {
			// Filter based on variant
			if (onlyCreatable && !field.accessLimitation().creatable()) {
				continue;
			}
			if (onlyUpdatable && !field.accessLimitation().updatable()) {
				continue;
			}
			if (!includeReadOnly && !onlyCreatable && !onlyUpdatable) {
				// Base variant without read-only - skip read-only fields
				if (!field.accessLimitation().creatable() && !field.accessLimitation().updatable()) {
					continue;
				}
			}

			hasFields = true;
			final String originalName = field.name();
			final String snakeName = fieldToSnakeCase(originalName);

			// Generate field comment if present
			if (field.comment() != null) {
				out.append("    # ");
				out.append(field.comment());
				out.append("\n");
			}

			out.append("    ");
			out.append(snakeName);
			out.append(": ");

			// Generate type annotation
			final String typeAnnotation = generateTypeAnnotation(field, pyGroup, imports);
			final String constraints = generateFieldConstraints(field, originalName);
			final boolean isOptional = onlyUpdatable || isOptionalField(field, true, null);

			if (!constraints.isEmpty()) {
				out.append("Annotated[");
				if (isOptional) {
					out.append(typeAnnotation);
					out.append(" | None, ");
				} else {
					out.append(typeAnnotation);
					out.append(", ");
				}
				out.append(constraints);
				out.append("]");
			} else if (isOptional) {
				out.append(typeAnnotation);
				out.append(" | None");
			} else {
				out.append(typeAnnotation);
			}

			// Default value for optional fields
			if (isOptional) {
				out.append(" = None");
			}

			out.append("\n");
		}

		if (!hasFields) {
			out.append("    pass\n");
		}

		// Add model_config for alias support
		if (includeReadOnly) {
			out.append("\n    model_config = {\"populate_by_name\": True}\n");
		}

		return out.toString();
	}

	/**
	 * Generate Python type annotation for a field.
	 */
	private String generateTypeAnnotation(
			final FieldProperty field,
			final PyClassElementGroup pyGroup,
			final PyImportModel imports) {

		final ClassModel fieldModel = field.model();
		return generateTypeForModel(fieldModel, pyGroup, imports);
	}

	/**
	 * Generates the Python type string for a ClassModel.
	 * @param model the class model to generate a type for
	 * @param pyGroup the group registry for resolving type references
	 * @param imports the import model to track dependencies
	 * @return the Python type annotation string
	 */
	public String generateTypeForModel(
			final ClassModel model,
			final PyClassElementGroup pyGroup,
			final PyImportModel imports) {

		if (model instanceof final ClassEnumModel enumModel) {
			final PyClassElement pyModel = pyGroup.find(enumModel);
			if (pyModel != null) {
				imports.addType(enumModel);
				return pyModel.getTypeName();
			}
			return "str";
		}

		if (model instanceof final ClassObjectModel objectModel) {
			final PyClassElement pyModel = pyGroup.find(objectModel);
			if (pyModel != null) {
				imports.addType(objectModel);
				return pyModel.getTypeName();
			}
			return "Any";
		}

		if (model instanceof final ClassListModel listModel) {
			final String valueType = generateTypeForModel(listModel.valueModel, pyGroup, imports);
			return "list[" + valueType + "]";
		}

		if (model instanceof final ClassMapModel mapModel) {
			final String keyType = generateTypeForModel(mapModel.keyModel, pyGroup, imports);
			final String valueType = generateTypeForModel(mapModel.valueModel, pyGroup, imports);
			return "dict[" + keyType + ", " + valueType + "]";
		}

		// Native type - use PyClassElement's type name
		final PyClassElement pyModel = pyGroup.find(model);
		if (pyModel != null) {
			return pyModel.getTypeName();
		}

		return "Any";
	}

	/**
	 * Generates the Python file for this element and adds it to the generation map.
	 * @param pyGroup the group registry for resolving type references
	 * @param generation the map of file paths to generated content
	 */
	public void generateFile(final PyClassElementGroup pyGroup, final Map<Path, String> generation) {
		if (this.nativeType == DefinedPosition.NATIVE) {
			return;
		}

		final ClassModel model = this.models.get(0);
		String data = "";

		if (model instanceof final ClassEnumModel modelEnum) {
			data = generateEnum(modelEnum, pyGroup);
		} else if (model instanceof final ClassObjectModel modelObject) {
			data = generateObject(modelObject, pyGroup);
		}

		generation.put(Paths.get("model").resolve(this.fileName + ".py"), data);
	}
}

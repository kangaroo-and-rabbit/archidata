package org.atriasoft.archidata.externalRestApi.python;

import java.nio.file.Path;
import java.util.Comparator;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import org.atriasoft.archidata.externalRestApi.model.ClassPaginationModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.atriasoft.archidata.tools.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Python type for code generation: a native type ({@code str}, {@code int}),
 * a type alias ({@code ObjectId}), an enum or a Pydantic model with its variants.
 *
 * <p>Mirrors {@link org.atriasoft.archidata.externalRestApi.typescript.TsClassElement}: the
 * variants generated for an object are the ones requested by the API ({@code Zone},
 * {@code ZoneCreate}, {@code ZoneUpdate}...), named after
 * {@link ParameterClassModel#getType()}.
 */
public class PyClassElement {
	/** Logger for this class. */
	static final Logger LOGGER = LoggerFactory.getLogger(PyClassElement.class);

	/**
	 * Order of the names inside an import statement: what isort (and ruff's `I` rules)
	 * expect, i.e. case-insensitive, the exact spelling only breaking ties.
	 */
	public static final Comparator<String> IMPORT_NAME_ORDER = Comparator
			.comparing((final String name) -> name.toLowerCase()).thenComparing(Comparator.naturalOrder());

	/** Maximum line length of the generated code (Black / ruff default). */
	public static final int LINE_LENGTH = 88;
	/** One indentation level. */
	public static final String INDENT = "    ";

	private static final Set<String> PYTHON_KEYWORDS = Set.of("False", "None", "True", "and", "as", "assert", "async",
			"await", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for", "from",
			"global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try",
			"while", "with", "yield", "match", "case", "type");

	/**
	 * Builtins a generated parameter must not shadow. Model fields are left alone: they
	 * mirror the server's names and shadowing only matters for a local binding.
	 */
	private static final Set<String> PYTHON_BUILTINS = Set.of("abs", "aiter", "all", "anext", "any", "ascii", "bin",
			"bool", "breakpoint", "bytearray", "bytes", "callable", "chr", "classmethod", "compile", "complex",
			"delattr", "dict", "dir", "divmod", "enumerate", "eval", "exec", "filter", "float", "format", "frozenset",
			"getattr", "globals", "hasattr", "hash", "help", "hex", "id", "input", "int", "isinstance", "issubclass",
			"iter", "len", "list", "locals", "map", "max", "memoryview", "min", "next", "object", "oct", "open", "ord",
			"pow", "print", "property", "range", "repr", "reversed", "round", "set", "setattr", "slice", "sorted",
			"staticmethod", "str", "sum", "super", "tuple", "type", "vars", "zip");

	/** Defines where a type is positioned in the generation hierarchy. */
	public enum DefinedPosition {
		/** Native Python type (str, int, datetime...): no file generated. */
		NATIVE,
		/** Type alias generated in its own file (ObjectId, Document). */
		BASIC,
		/** Enum or Pydantic model to generate. */
		NORMAL
	}

	/**
	 * An import needed to use or declare a type.
	 * @param module one of {@code typing}, {@code datetime}, {@code pydantic}, {@code uuid}, {@code rest_tools}
	 * @param name the imported name ({@code null} for {@code import uuid})
	 */
	public record ImportSpec(
			String module,
			String name) {
		/**
		 * Applies this import to an import model.
		 * @param imports the import model to fill
		 */
		public void applyTo(final PyImportModel imports) {
			switch (this.module) {
				case "typing" -> imports.addTyping(this.name);
				case "datetime" -> imports.addDatetime(this.name);
				case "pydantic" -> imports.addPydantic(this.name);
				case "uuid" -> imports.requestUuid();
				case "rest_tools" -> imports.addRestTools(this.name);
				default -> imports.addRaw("import " + this.module);
			}
		}
	}

	/** The list of class models associated with this element. */
	public final List<ClassModel> models;
	/** The fixed Python type name (native and basic types), {@code null} for generated models. */
	private final String pyTypeName;
	/** The right-hand side of the type alias (basic types only). */
	private final String declaration;
	/** Imports needed wherever the type name is used. */
	private final List<ImportSpec> usageImports;
	/** Imports needed by the type alias declaration (basic types only). */
	private final List<ImportSpec> declarationImports;
	/** The file name (snake_case, without extension) of the generated Python module. */
	public final String fileName;
	/** The position category of this type (native, basic, or normal). */
	public final DefinedPosition nativeType;
	/** The variants requested by the API (Pydantic models only). */
	public final Set<ParameterClassModel> requestedModels = new LinkedHashSet<>();
	/** The type names written in the generated file, filled by {@link #generateFile}. */
	public final List<String> generatedNames = new ArrayList<>();

	/**
	 * Converts a CamelCase class name to a snake_case module name.
	 * @param className the CamelCase class name to convert
	 * @return the snake_case version of the class name
	 */
	public static String toSnakeCase(final String className) {
		return className.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z0-9])([A-Z])", "$1_$2")
				.replace('-', '_').toLowerCase();
	}

	/**
	 * Converts a Java identifier to a valid Python snake_case identifier.
	 * A Python keyword gets a trailing underscore (PEP 8 convention).
	 * @param name the Java identifier (camelCase)
	 * @return the Python identifier
	 */
	public static String toPythonIdentifier(final String name) {
		String out = toSnakeCase(name);
		if (out.isEmpty() || Character.isDigit(out.charAt(0))) {
			out = "_" + out;
		}
		if (PYTHON_KEYWORDS.contains(out)) {
			out = out + "_";
		}
		return out;
	}

	/**
	 * Converts a Java parameter name to a Python one, also avoiding the builtins: a
	 * parameter named {@code range} or {@code type} would shadow them in the method body.
	 * @param name the Java parameter name
	 * @return the Python parameter name
	 */
	public static String toPythonParameter(final String name) {
		final String out = toPythonIdentifier(name);
		if (PYTHON_BUILTINS.contains(out)) {
			return out + "_";
		}
		return out;
	}

	/**
	 * Renders a Python double-quoted string literal.
	 * @param value the text to quote
	 * @return the literal, including the quotes
	 */
	public static String pyString(final String value) {
		// Same rule as the Python formatters: double quotes, unless single quotes mean
		// strictly fewer escapes.
		int doubleQuotes = 0;
		int singleQuotes = 0;
		for (final char current : value.toCharArray()) {
			if (current == '"') {
				doubleQuotes++;
			} else if (current == '\'') {
				singleQuotes++;
			}
		}
		final char quote = doubleQuotes > singleQuotes ? '\'' : '"';
		final StringBuilder out = new StringBuilder().append(quote);
		for (final char current : value.toCharArray()) {
			if (current == quote || current == '\\') {
				out.append('\\').append(current);
			} else if (current == '\n') {
				out.append("\\n");
			} else if (current == '\r') {
				out.append("\\r");
			} else if (current == '\t') {
				out.append("\\t");
			} else {
				out.append(current);
			}
		}
		out.append(quote);
		return out.toString();
	}

	/**
	 * Renders an indented docstring.
	 * @param text the text (may contain new lines)
	 * @param indent the indentation of the docstring
	 * @return the docstring lines, each terminated by a new line
	 */
	public static String pyDocstring(final String text, final String indent) {
		final boolean trailingBlank = text.endsWith("\n\n");
		String safe = text.replace("\\", "\\\\").replace("\"\"\"", "\\\"\\\"\\\"").replace("\t", INDENT).strip();
		if (safe.endsWith("\"")) {
			safe = safe + " ";
		}
		if (!safe.isEmpty()) {
			// a docstring summary starts with a capital, whatever the @ApiDoc text does
			safe = Character.toUpperCase(safe.charAt(0)) + safe.substring(1);
		}
		final String[] lines = safe.split("\n");
		final StringBuilder out = new StringBuilder();
		if (lines.length == 1) {
			out.append(indent).append("\"\"\"").append(lines[0]).append("\"\"\"\n");
			return out.toString();
		}
		out.append(indent).append("\"\"\"").append(lines[0]).append("\n");
		if (!lines[1].isBlank()) {
			// a summary line and its description are separated by a blank line
			out.append("\n");
		}
		for (int i = 1; i < lines.length; i++) {
			if (lines[i].isBlank()) {
				out.append("\n");
			} else {
				out.append(indent).append(lines[i]).append("\n");
			}
		}
		if (trailingBlank) {
			out.append("\n");
		}
		out.append(indent).append("\"\"\"\n");
		return out.toString();
	}

	/**
	 * Constructor for native and basic types.
	 * @param models the class models mapped on this Python type
	 * @param pyTypeName the Python type name
	 * @param declaration the type alias right-hand side ({@code null} for native types)
	 * @param usageImports imports needed wherever the type name is used
	 * @param declarationImports imports needed by the alias declaration
	 * @param nativeType {@link DefinedPosition#NATIVE} or {@link DefinedPosition#BASIC}
	 */
	public PyClassElement(final List<ClassModel> models, final String pyTypeName, final String declaration,
			final List<ImportSpec> usageImports, final List<ImportSpec> declarationImports,
			final DefinedPosition nativeType) {
		this.models = models;
		this.pyTypeName = pyTypeName;
		this.declaration = declaration;
		this.usageImports = usageImports == null ? List.of() : usageImports;
		this.declarationImports = declarationImports == null ? List.of() : declarationImports;
		this.nativeType = nativeType;
		this.fileName = toSnakeCase(pyTypeName);
	}

	/**
	 * Constructor for the enums and objects to generate.
	 * @param model the class model to wrap
	 */
	public PyClassElement(final ClassModel model) {
		this.models = List.of(model);
		this.pyTypeName = null;
		this.declaration = null;
		this.usageImports = List.of();
		this.declarationImports = List.of();
		this.nativeType = DefinedPosition.NORMAL;
		this.fileName = toSnakeCase(model.getOriginClasses().getSimpleName());
	}

	/**
	 * Gets the Python type name of the default (read) variant, registering it when needed.
	 * @return the Python type name
	 */
	public String getTypeName() {
		if (this.pyTypeName != null) {
			return this.pyTypeName;
		}
		return getParameterClassModel(this.models.get(0)).getType();
	}

	/**
	 * Gets the Python type name of a variant, registering it when needed.
	 * @param valid whether validation is active
	 * @param groups the validation groups
	 * @return the Python type name
	 */
	public String getTypeName(final boolean valid, final Class<?>[] groups) {
		if (this.pyTypeName != null) {
			return this.pyTypeName;
		}
		return getParameterClassModel(valid, groups, this.models.get(0)).getType();
	}

	/**
	 * Gets the Python type name of a variant, registering it when needed.
	 * @param model the requested variant
	 * @return the Python type name
	 */
	public String getTypeName(final ParameterClassModel model) {
		if (this.pyTypeName != null) {
			return this.pyTypeName;
		}
		return getParameterClassModel(model.valid(), model.groups(), model.model()).getType();
	}

	/**
	 * Registers the imports needed to use the type name in a file.
	 * @param imports the import model of the file
	 */
	public void applyUsageImports(final PyImportModel imports) {
		for (final ImportSpec spec : this.usageImports) {
			spec.applyTo(imports);
		}
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
	 * Gets or creates the requested variant for the given validation context.
	 * @param valid whether validation is active
	 * @param validGroup the validation groups
	 * @param parameterModel the class model to wrap
	 * @return the existing or newly registered variant
	 */
	public ParameterClassModel getParameterClassModel(
			final boolean valid,
			final Class<?>[] validGroup,
			final ClassModel parameterModel) {
		final ParameterClassModel tmp = new ParameterClassModel(valid, validGroup, parameterModel);
		for (final ParameterClassModel elem : this.requestedModels) {
			if (elem.equals(tmp)) {
				return elem;
			}
		}
		this.requestedModels.add(tmp);
		return tmp;
	}

	/**
	 * Gets or creates the default (read) variant.
	 * @param parameterModel the class model to wrap
	 * @return the existing or newly registered variant
	 */
	public ParameterClassModel getParameterClassModel(final ClassModel parameterModel) {
		final ParameterClassModel tmp = new ParameterClassModel(parameterModel);
		for (final ParameterClassModel elem : this.requestedModels) {
			if (elem.equals(tmp)) {
				return elem;
			}
		}
		this.requestedModels.add(tmp);
		return tmp;
	}

	// ------------------------------------------------------------------
	// Type rendering
	// ------------------------------------------------------------------

	/**
	 * Renders the Python annotation of a class model, registering the imports it needs.
	 * Objects and enums are always referenced through their default (read) variant.
	 * @param model the class model to render
	 * @param group the group registry for resolving type references
	 * @param imports the import model of the file being generated
	 * @return the Python type expression
	 */
	public static String generateTypeForModel(
			final ClassModel model,
			final PyClassElementGroup group,
			final PyImportModel imports) {
		if (model instanceof final ClassListModel listModel) {
			String inner = generateTypeForModel(listModel.valueModel, group, imports);
			if (listModel.valueNullable) {
				inner = inner + " | None";
			}
			return "list[" + inner + "]";
		}
		if (model instanceof final ClassMapModel mapModel) {
			final String key = generateTypeForModel(mapModel.keyModel, group, imports);
			String value = generateTypeForModel(mapModel.valueModel, group, imports);
			if (mapModel.valueNullable) {
				value = value + " | None";
			}
			return "dict[" + key + ", " + value + "]";
		}
		if (model instanceof final ClassPaginationModel paginationModel) {
			imports.addRestTools("Pagination");
			return "Pagination[" + generateTypeForModel(paginationModel.valueModel, group, imports) + "]";
		}
		final PyClassElement element = group.find(model);
		if (element == null) {
			LOGGER.warn("No Python type for {} ==> Any", model);
			imports.addTyping("Any");
			return "Any";
		}
		final String name = element.getTypeName();
		if (element.nativeType == DefinedPosition.NATIVE) {
			element.applyUsageImports(imports);
		} else {
			imports.addModel(element, name);
		}
		return name;
	}

	// ------------------------------------------------------------------
	// Field rules (same as TsClassElement)
	// ------------------------------------------------------------------

	private static boolean isCompatibleGroup(final Class<?>[] groupsA, final Class<?>[] groupsB) {
		if (groupsA == null || groupsB == null) {
			return false;
		}
		for (final Class<?> elemA : groupsA) {
			for (final Class<?> elemB : groupsB) {
				if (TypeUtils.isSameClass(elemA, elemB)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if a field belongs to a variant.
	 * A non-validated variant ({@code valid == false}) keeps every field: unlike the TypeScript
	 * generator (which emits an empty Zod object), a Pydantic model without fields would drop
	 * the whole payload.
	 * @param field the field property to check
	 * @param isValid whether validation is active
	 * @param groups the validation groups to consider
	 * @return true if the field must be generated in the variant
	 */
	public static boolean isCompatibleField(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		if (!isValid) {
			return true;
		}
		if (field.annotationNotNull() != null) {
			if (isCompatibleGroup(field.annotationNotNull().groups(), groups)) {
				return true;
			}
			if (field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0) {
				return true;
			}
		}
		if (field.annotationNull() != null) {
			if (isCompatibleGroup(field.annotationNull().groups(), groups)) {
				return false;
			}
			if (field.annotationNull().groups() == null || field.annotationNull().groups().length == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines if a field is optional ({@code T | None = None}) in a variant.
	 * @param field the field property to check
	 * @param isValid whether validation is active
	 * @param groups the validation groups to consider
	 * @return true if the field is optional
	 */
	public static boolean isOptionalField(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		if (field.apiNotNull() != null) {
			return !field.apiNotNull().value();
		}
		if (!isValid) {
			return true;
		}
		if (field.nullable()) {
			return true;
		}
		if (field.annotationNotNull() != null) {
			if (isCompatibleGroup(field.annotationNotNull().groups(), groups)) {
				return false;
			}
			if (field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0) {
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
	 * Builds the {@code Field(...)} arguments of a field: default, alias, description,
	 * validation constraints and read-only marker.
	 * @param field the field property
	 * @param pyName the Python attribute name
	 * @param optional whether the field is optional
	 * @param imports the import model of the file (to request {@code Field})
	 * @return the argument list, possibly empty
	 */
	public static List<String> generateFieldArgs(
			final FieldProperty field,
			final String pyName,
			final boolean optional,
			final PyImportModel imports) {
		final List<String> args = new ArrayList<>();
		if (optional) {
			args.add("default=None");
		}
		if (!pyName.equals(field.name())) {
			args.add("alias=" + pyString(field.name()));
		}
		if (field.comment() != null && !field.comment().isBlank()) {
			args.add("description=" + pyString(field.comment().strip()));
		}
		final Class<?> clazz = field.model().getOriginClasses();
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
				args.add("pattern=" + pyString(field.pattern().regexp()));
			}
		}
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
				args.add((field.decimalMin().inclusive() ? "ge=" : "gt=") + field.decimalMin().value());
			}
			if (field.decimalMax() != null) {
				args.add((field.decimalMax().inclusive() ? "le=" : "lt=") + field.decimalMax().value());
			}
		}
		if (field.apiReadOnly() != null) {
			args.add("frozen=True");
		}
		if (args.size() > 1 || (args.size() == 1 && !optional)) {
			imports.addPydantic("Field");
		}
		return args;
	}

	/**
	 * Renders one attribute line (wrapped when longer than {@link #LINE_LENGTH}).
	 * @param pyName the attribute name
	 * @param annotation the type annotation
	 * @param args the {@code Field(...)} arguments
	 * @return the attribute declaration, terminated by a new line
	 */
	public static String renderField(final String pyName, final String annotation, final List<String> args) {
		final String declaration = INDENT + pyName + ": " + annotation;
		if (args.isEmpty()) {
			return declaration + "\n";
		}
		if (args.size() == 1 && "default=None".equals(args.get(0))) {
			return declaration + " = None\n";
		}
		final String oneLine = declaration + " = Field(" + String.join(", ", args) + ")";
		if (oneLine.length() <= LINE_LENGTH) {
			return oneLine + "\n";
		}
		final StringBuilder out = new StringBuilder();
		if ((declaration + " = Field(").length() <= LINE_LENGTH) {
			out.append(declaration).append(" = Field(\n");
			for (final String arg : args) {
				out.append(INDENT).append(INDENT).append(arg).append(",\n");
			}
			out.append(INDENT).append(")\n");
			return out.toString();
		}
		// even `name: type = Field(` does not fit: the whole value goes in parentheses
		out.append(declaration).append(" = (\n");
		out.append(INDENT).append(INDENT).append("Field(\n");
		for (final String arg : args) {
			out.append(INDENT).append(INDENT).append(INDENT).append(arg).append(",\n");
		}
		out.append(INDENT).append(INDENT).append(")\n");
		out.append(INDENT).append(")\n");
		return out.toString();
	}

	// ------------------------------------------------------------------
	// File generation
	// ------------------------------------------------------------------

	/**
	 * Generates the module of a type alias (basic type).
	 * @return the Python source
	 */
	public String generateBaseAlias() {
		final PyImportModel imports = new PyImportModel();
		for (final ImportSpec spec : this.declarationImports) {
			spec.applyTo(imports);
		}
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"").append(this.pyTypeName).append(" type alias (auto-generated code).\"\"\"\n\n");
		out.append(imports.renderCommonImports());
		// PEP 695, available from Python 3.12: the form `type X = ...` replaces the
		// `X: TypeAlias = ...` annotation.
		out.append("\n").append("type ").append(this.pyTypeName).append(" = ").append(this.declaration).append("\n");
		this.generatedNames.add(this.pyTypeName);
		return out.toString();
	}

	/**
	 * Generates the module of an enum ({@code StrEnum}, or {@code IntEnum} when every value is an integer).
	 * @param model the enum model
	 * @return the Python source
	 */
	public String generateEnum(final ClassEnumModel model) {
		final String typeName = getTypeName();
		final Map<String, Object> values = model.getListOfValues();
		boolean allInt = !values.isEmpty();
		for (final Object value : values.values()) {
			if (!(value instanceof Integer)) {
				allInt = false;
				break;
			}
		}
		final String base = allInt ? "IntEnum" : "StrEnum";
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"").append(typeName).append(" enumeration (auto-generated code).\"\"\"\n\n");
		out.append("from enum import ").append(base).append("\n\n\n");
		out.append("class ").append(typeName).append("(").append(base).append("):\n");
		out.append(INDENT).append("\"\"\"").append(typeName).append(" enumeration.\"\"\"\n");
		if (!values.isEmpty()) {
			out.append("\n");
		}
		for (final Entry<String, Object> elem : values.entrySet()) {
			String memberName = elem.getKey();
			if (PYTHON_KEYWORDS.contains(memberName) || Character.isDigit(memberName.charAt(0))) {
				memberName = memberName + "_";
			}
			final String value = allInt ? String.valueOf(elem.getValue()) : pyString(String.valueOf(elem.getValue()));
			final String line = INDENT + memberName + " = " + value;
			if (line.length() <= LINE_LENGTH) {
				out.append(line).append("\n");
			} else {
				// a formatter would wrap the value rather than leave the line over the limit
				out.append(INDENT).append(memberName).append(" = (\n");
				out.append(INDENT).append(INDENT).append(value).append("\n");
				out.append(INDENT).append(")\n");
			}
		}
		this.generatedNames.add(typeName);
		return out.toString();
	}

	private String variantDocstring(final ClassObjectModel model, final ParameterClassModel variant) {
		final StringBuilder text = new StringBuilder();
		if (model.getDescription() != null && !model.getDescription().isBlank()) {
			text.append(model.getDescription().strip());
		} else {
			text.append(model.getOriginClasses().getSimpleName()).append(" model");
		}
		final String defaultName = new ParameterClassModel(model).getType();
		final String variantName = variant.getType();
		if (!variantName.equals(defaultName)) {
			text.append(" (").append(variantName.substring(defaultName.length())).append(" variant)");
		}
		if (!text.toString().endsWith(".")) {
			text.append(".");
		}
		if (model.getExample() != null && !model.getExample().isBlank()) {
			// A fenced JSON block, not an indented literal one: a Python formatter reads the
			// latter as code and rewrites the example (`docstring-code-format`). The label
			// deliberately avoids "Example", which the docstring rules read as a Google
			// section header and then want padded the way the formatter unpads it.
			text.append("\n\nExample payload:\n```json\n");
			for (final String line : model.getExample().strip().split("\n")) {
				text.append(line).append("\n");
			}
			text.append("```\n");
		}
		return pyDocstring(text.toString(), INDENT);
	}

	/**
	 * Generates one Pydantic model variant.
	 * @param model the object model
	 * @param group the group registry for resolving type references
	 * @param variant the variant to generate
	 * @param imports the import model of the file
	 * @return the class source
	 */
	public String generateObjectVariant(
			final ClassObjectModel model,
			final PyClassElementGroup group,
			final ParameterClassModel variant,
			final PyImportModel imports) {
		final String className = getTypeName(variant);
		final StringBuilder out = new StringBuilder();
		String base = "BaseModel";
		if (model.getExtendsClass() != null) {
			final PyClassElement parent = group.find(model.getExtendsClass());
			if (parent != null && parent.nativeType != DefinedPosition.NATIVE) {
				base = parent.getTypeName(variant.valid(), variant.groups());
				imports.addModel(parent, base);
			}
		}
		if ("BaseModel".equals(base)) {
			imports.addPydantic("BaseModel");
			imports.addPydantic("ConfigDict");
		}
		out.append("class ").append(className).append("(").append(base).append("):\n");
		out.append(variantDocstring(model, variant));
		if ("BaseModel".equals(base)) {
			out.append("\n");
			out.append(INDENT).append("model_config = ConfigDict(populate_by_name=True, protected_namespaces=())\n");
		}
		final StringBuilder fields = new StringBuilder();
		for (final FieldProperty field : model.getFields()) {
			if (!isCompatibleField(field, variant.valid(), variant.groups())) {
				continue;
			}
			final String pyName = toPythonIdentifier(field.name());
			final boolean optional = isOptionalField(field, variant.valid(), variant.groups());
			String annotation = generateTypeForModel(field.model(), group, imports);
			if (optional) {
				annotation = annotation + " | None";
			}
			fields.append(renderField(pyName, annotation, generateFieldArgs(field, pyName, optional, imports)));
		}
		if (fields.length() != 0) {
			out.append("\n");
			out.append(fields);
		}
		this.generatedNames.add(className);
		return out.toString();
	}

	/**
	 * Generates the module of an object: every requested variant, with the imports they need.
	 * @param model the object model
	 * @param group the group registry for resolving type references
	 * @return the Python source
	 */
	public String generateObject(final ClassObjectModel model, final PyClassElementGroup group) {
		final PyImportModel imports = new PyImportModel();
		imports.requestFutureAnnotations();
		if (group.hasDeferredImports(this)) {
			imports.addTyping("TYPE_CHECKING");
		}
		final Map<String, ParameterClassModel> ordered = new TreeMap<>();
		if (this.requestedModels.isEmpty()) {
			getParameterClassModel(model);
		}
		for (final ParameterClassModel elem : new ArrayList<>(this.requestedModels)) {
			ordered.put(elem.getType(), elem);
		}
		final List<String> classes = new ArrayList<>();
		for (final ParameterClassModel variant : ordered.values()) {
			classes.add(generateObjectVariant(model, group, variant, imports));
		}
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"").append(model.getOriginClasses().getSimpleName())
				.append(" models (auto-generated code).\"\"\"\n\n");
		out.append(imports.renderCommonImports());
		out.append(imports.renderModelFileImports(this, group));
		out.append("\n");
		out.append(String.join("\n\n", classes));
		return out.toString();
	}

	/**
	 * Generates the Python module of this element and adds it to the generation map.
	 * @param group the group registry for resolving type references
	 * @param generation the map of file paths to generated content
	 */
	public void generateFile(final PyClassElementGroup group, final Map<Path, String> generation) {
		if (this.nativeType == DefinedPosition.NATIVE) {
			return;
		}
		final ClassModel model = this.models.get(0);
		final String data;
		if (this.nativeType == DefinedPosition.BASIC) {
			data = generateBaseAlias();
		} else if (model instanceof final ClassEnumModel modelEnum) {
			data = generateEnum(modelEnum);
		} else if (model instanceof final ClassObjectModel modelObject) {
			data = generateObject(modelObject, group);
		} else {
			LOGGER.warn("Unmanaged model type for Python generation: {}", model);
			return;
		}
		generation.put(Paths.get("model").resolve(this.fileName + ".py"), data);
	}
}

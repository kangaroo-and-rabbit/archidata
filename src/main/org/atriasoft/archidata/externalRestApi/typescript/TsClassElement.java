package org.atriasoft.archidata.externalRestApi.typescript;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

public class TsClassElement {
	static final Logger LOGGER = LoggerFactory.getLogger(TsClassElement.class);

	public enum DefinedPosition {
		NATIVE, // Native element of  TS language.
		BASIC, // basic wrapping for JAVA type.
		NORMAL // Normal Object to interpret.
	}

	public List<ClassModel> models;
	public String aaaaaaaaaaaaaa_zodName;
	private final String aaaaaaaaaaaaaa_tsTypeName;
	private final String aaaaaaaaaaaaaa_tsCheckType;
	private final String declaration;
	public String fileName = null;
	public String comment = null;
	public DefinedPosition nativeType = DefinedPosition.NORMAL;
	public Set<ParameterClassModel> requestedModels = new HashSet<>();

	public static String determineFileName(final String className) {
		return className.replaceAll("([a-z])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2").toLowerCase();
	}

	public TsClassElement(final List<ClassModel> model, final String zodName, final String tsTypeName,
			final String tsCheckType, final String declaration, final DefinedPosition nativeType) {
		if (model.get(0).getOriginClasses().getSimpleName().equals("TestObject")) {
			LOGGER.error("lkjlkj");
		}
		this.models = model;
		this.aaaaaaaaaaaaaa_zodName = zodName;
		this.aaaaaaaaaaaaaa_tsTypeName = tsTypeName;
		this.aaaaaaaaaaaaaa_tsCheckType = tsCheckType;
		this.declaration = declaration;
		this.nativeType = nativeType;
		this.fileName = determineFileName(tsTypeName);
	}

	public TsClassElement(final ClassModel model, final boolean forceMode) {
		this.models = List.of(model);
		if (forceMode) {
			if (model.getOriginClasses().getSimpleName().equals("TestObject")) {
				LOGGER.error("lkjlkj");
			}
			this.aaaaaaaaaaaaaa_zodName = "Zod" + model.getOriginClasses().getSimpleName();
			this.aaaaaaaaaaaaaa_tsTypeName = model.getOriginClasses().getSimpleName();
			this.aaaaaaaaaaaaaa_tsCheckType = "is" + model.getOriginClasses().getSimpleName();
		} else {
			this.aaaaaaaaaaaaaa_zodName = null;
			this.aaaaaaaaaaaaaa_tsTypeName = null;
			this.aaaaaaaaaaaaaa_tsCheckType = null;
		}
		this.declaration = null;
		this.fileName = determineFileName(model.getOriginClasses().getSimpleName());
	}

	public TsClassElement(final ClassModel model) {
		if (model.getOriginClasses().getSimpleName().equals("TestObject")) {
			LOGGER.error("lkjlkj");
		}
		this.models = List.of(model);
		this.aaaaaaaaaaaaaa_zodName = "Zod" + model.getOriginClasses().getSimpleName();
		this.aaaaaaaaaaaaaa_tsTypeName = model.getOriginClasses().getSimpleName();
		this.aaaaaaaaaaaaaa_tsCheckType = "is" + model.getOriginClasses().getSimpleName();
		this.declaration = null;
		this.fileName = determineFileName(this.aaaaaaaaaaaaaa_tsTypeName);
	}

	public String getZodName() {
		final ParameterClassModel model = getParameterClassModel(this.models.get(0));
		if (this.aaaaaaaaaaaaaa_zodName != null) {
			return this.aaaaaaaaaaaaaa_zodName;
		}
		return "Zod" + model.getType();
	}

	public String getZodName(final boolean isValid, final Class<?>[] groups) {
		final ParameterClassModel model = getParameterClassModel(isValid, groups, this.models.get(0));
		if (this.aaaaaaaaaaaaaa_zodName != null) {
			return this.aaaaaaaaaaaaaa_zodName;
		}
		return "Zod" + model.getType();
	}

	public String getZodName(final ParameterClassModel model) {
		getParameterClassModel(model.valid(), model.groups(), model.model());
		if (this.aaaaaaaaaaaaaa_zodName != null) {
			return this.aaaaaaaaaaaaaa_zodName;
		}
		return "Zod" + model.getType();
	}

	public String getTypeName() {
		final ParameterClassModel model = getParameterClassModel(this.models.get(0));
		if (this.aaaaaaaaaaaaaa_tsTypeName != null) {
			return this.aaaaaaaaaaaaaa_tsTypeName;
		}
		return model.getType();
	}

	public String getTypeName(final boolean isValid, final Class<?>[] groups) {
		final ParameterClassModel model = getParameterClassModel(isValid, groups, this.models.get(0));
		if (this.aaaaaaaaaaaaaa_tsTypeName != null) {
			return this.aaaaaaaaaaaaaa_tsTypeName;
		}
		return model.getType();
	}

	public String getTypeName(final ParameterClassModel model) {
		this.requestedModels.add(model);
		if (this.aaaaaaaaaaaaaa_tsTypeName != null) {
			return this.aaaaaaaaaaaaaa_tsTypeName;
		}
		return model.getType();
	}

	public String getCheckType(final boolean isValid, final Class<?>[] groups) {
		final ParameterClassModel model = getParameterClassModel(isValid, groups, this.models.get(0));
		if (this.aaaaaaaaaaaaaa_tsCheckType != null) {
			return this.aaaaaaaaaaaaaa_tsCheckType;
		}
		return "is" + model.getType();
	}

	public String getCheckType() {
		final ParameterClassModel model = getParameterClassModel(this.models.get(0));
		if (this.aaaaaaaaaaaaaa_tsCheckType != null) {
			return this.aaaaaaaaaaaaaa_tsCheckType;
		}
		return "is" + model.getType();
	}

	public String getCheckType(final ParameterClassModel model) {
		if (model != null) {
			this.requestedModels.add(model);
		}
		if (this.aaaaaaaaaaaaaa_tsCheckType != null) {
			return this.aaaaaaaaaaaaaa_tsCheckType;
		}
		return "is" + model.getType();
	}

	public boolean isCompatible(final ClassModel model) {
		return this.models.contains(model);
	}

	public String getBaseHeader() {
		return """
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";

				""";
	}

	public String generateEnum(final ClassEnumModel model, final TsClassElementGroup tsGroup) {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append("\n");
		//out.append(generateComment(model));

		if (System.getenv("ARCHIDATA_GENERATE_ZOD_ENUM") != null) {
			boolean first = true;
			out.append("export const ");
			out.append(this.aaaaaaaaaaaaaa_tsTypeName);
			out.append(" = ");
			out.append("zod.enum([");
			for (final Entry<String, Object> elem : model.getListOfValues().entrySet()) {
				if (!first) {
					out.append(",\n\t");
				} else {
					out.append("\n\t");
					first = false;
				}
				out.append("'");
				out.append(elem.getKey());
				out.append("'");
			}
			if (first) {
				out.append("]}");
			} else {
				out.append("\n\t])");
			}
			out.append(";\n");
			out.append(generateZodInfer(this.aaaaaaaaaaaaaa_tsTypeName, this.aaaaaaaaaaaaaa_zodName));
		} else {
			boolean first = true;
			out.append("export enum ");
			out.append(this.aaaaaaaaaaaaaa_tsTypeName);
			out.append("  {");
			for (final Entry<String, Object> elem : model.getListOfValues().entrySet()) {
				if (!first) {
					out.append(",\n\t");
				} else {
					out.append("\n\t");
					first = false;
				}
				out.append(elem.getKey());
				out.append(" = ");
				if (elem.getValue() instanceof final Integer value) {
					out.append(value);
				} else {
					out.append("'");
					out.append(elem.getValue());
					out.append("'");
				}
			}
			if (first) {
				out.append("}");
			} else {
				out.append(",\n\t}");
			}
			out.append(";\n");
			out.append("\nexport const ");
			out.append(this.aaaaaaaaaaaaaa_zodName);
			out.append(" = zod.nativeEnum(");
			out.append(this.aaaaaaaaaaaaaa_tsTypeName);
			out.append(");\n");
		}
		out.append(generateExportCheckFunctionAppended("", null));
		return out.toString();
	}

	private static String generateExportCheckFunction(
			final String tsCheckType,
			final String tsTypeName,
			final String zodName) {
		final StringBuilder out = new StringBuilder();
		out.append("\nexport function ");
		out.append(tsCheckType);
		out.append("(data: any): data is ");
		out.append(tsTypeName);
		out.append(" {\n\ttry {\n\t\t");
		out.append(zodName);
		out.append("""
				.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='""");
		out.append(tsTypeName);
		out.append("""
				' error=${e}`);
						return false;
					}
				}
				""");
		return out.toString();
	}

	private String generateExportCheckFunctionAppended(
			final String appendString,
			final ParameterClassModel parameterClassModel) {
		return generateExportCheckFunction(this.getCheckType(parameterClassModel) + appendString,
				this.getTypeName(parameterClassModel) + appendString,
				this.getZodName(parameterClassModel) + appendString);
	}

	public String generateImports(final Set<ClassModel> depModels, final TsClassElementGroup tsGroup) {
		final Set<TsClassElement> typeScriptModelAlreadyImported = new HashSet<>();
		final Map<String, String> mapOutput = new TreeMap<>();

		for (final ClassModel depModel : depModels) {
			final StringBuilder inputStream = new StringBuilder();
			final TsClassElement tsModel = tsGroup.find(depModel);
			if (typeScriptModelAlreadyImported.contains(tsModel)) {
				LOGGER.trace("Model alredy imported for typescript");
				continue;
			}
			typeScriptModelAlreadyImported.add(tsModel);
			if (tsModel.nativeType != DefinedPosition.NATIVE) {
				inputStream.append("import {");
				if (tsModel.nativeType != DefinedPosition.NORMAL
						|| tsModel.models.get(0).getApiGenerationMode().read()) {
					inputStream.append(tsModel.zodName);
				}
				if (tsModel.nativeType == DefinedPosition.NORMAL
						&& tsModel.models.get(0).getApiGenerationMode().update()) {
					inputStream.append(", ");
					inputStream.append(tsModel.zodName);
					inputStream.append(MODEL_TYPE_UPDATE);
					inputStream.append(" ");
				}
				if (tsModel.nativeType == DefinedPosition.NORMAL
						&& tsModel.models.get(0).getApiGenerationMode().create()) {
					inputStream.append(", ");
					inputStream.append(tsModel.zodName);
					inputStream.append(MODEL_TYPE_CREATE);
					inputStream.append(" ");
				}
				inputStream.append("} from \"./");
				inputStream.append(tsModel.fileName);
				inputStream.append("\";\n");
				mapOutput.put(tsModel.fileName.toString().toLowerCase(), inputStream.toString());
			}
		}
		final StringBuilder out = new StringBuilder();
		for (final Entry<String, String> elem : mapOutput.entrySet()) {
			out.append(elem.getValue());
		}
		return out.toString();

	}

	private Object generateComment(final ClassObjectModel model) {
		final StringBuilder out = new StringBuilder();
		if (model.getDescription() != null || model.getExample() != null) {
			out.append("/**\n");
			if (model.getDescription() != null) {
				for (final String elem : model.getDescription().split("\n")) {
					out.append(" * ");
					out.append(elem);
					out.append("\n");
				}
			}
			if (model.getExample() != null) {
				out.append(" * Example:\n");
				out.append(" * ```\n");
				for (final String elem : model.getExample().split("\n")) {
					out.append(" * ");
					out.append(elem);
					out.append("\n");
				}
				out.append(" * ```\n");
			}
			out.append(" */\n");
		}
		return out.toString();
	}

	private boolean containGroup(final Class<?>[] groupsA, final Class<?> groupB) {
		for (final Class<?> elemA : groupsA) {
			if (elemA == groupB) {
				return true;
			}
		}
		return false;
	}

	private boolean isCompatibleGroup(final Class<?>[] groupsA, final Class<?>[] groupsB) {
		for (final Class<?> elemA : groupsA) {
			for (final Class<?> elemB : groupsB) {
				if (elemA == elemB) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isCompatibleField(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		int findAnnotation = 0;
		if (field.annotationNotNull() != null) {
			findAnnotation++;
			// if it is not null iun the specific group, this meen the element MUST be transcript
			if (isCompatibleGroup(field.annotationNotNull().groups(), groups)) {
				// todo, maybe check if the element does not exist in the other item ...
				return true;
			}
			// check if no element
			if ((field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0)
					&& isValid) {
				return true;
			}
		}
		if (field.annotationNull() != null) {
			findAnnotation++;
			// if it is not null iun the specific group, this meen the element MUST NOT be transcript (is is NULL)
			if (isCompatibleGroup(field.annotationNull().groups(), groups)) {
				// todo, maybe check if the element does not exist in the other item ...
				return false;
			}
			if ((field.annotationNull().groups() == null || field.annotationNull().groups().length == 0) && isValid) {
				return false;
			}
		}
		if (findAnnotation != 0) {
			return false;
		}
		return isValid;
	}

	public boolean isOptionalTypeZod(final FieldProperty field) {
		if (field.apiNotNull() != null) {
			return !field.apiNotNull().value();
		}
		// Common checking element (apply to List, Map, ...)
		if (field.nullable()) {
			return true;
		}
		if (field.annotationNotNull() != null) {
			return false;
		}
		if (field.annotationNull() != null) {
			return false;
		}
		// model is a Map or a List:
		if (field.model() instanceof ClassMapModel || field.model() instanceof ClassListModel) {
			return true;
		}
		// Other object:
		if (field.model().getOriginClasses() == null || field.model().getOriginClasses().isPrimitive()) {
			return false;
		}
		return true;
	}

	public String optionalTypeZod(final FieldProperty field) {
		// Common checking element (apply to List, Map, ...)
		if (isOptionalTypeZod(field)) {
			return ".optional()";
		}
		return "";
	}

	public String optionalWriteTypeZod(final FieldProperty field) {
		// Common checking element (apply to List, Map, ...)
		if (isOptionalTypeZod(field)) {
			return ".nullable()";
		}
		return "";
	}

	public String maxSizeZod(final FieldProperty field) {
		final StringBuilder builder = new StringBuilder();
		final Class<?> clazz = field.model().getOriginClasses();
		if (clazz == String.class) {
			if (field.stringSize() != null) {
				if (field.stringSize().min() > 0) {
					// A string size can not be lower at 0
					builder.append(".min(");
					builder.append(field.stringSize().min());
					builder.append(")");
				}
				if (field.stringSize().max() != Integer.MAX_VALUE) {
					builder.append(".max(");
					builder.append(field.stringSize().max());
					builder.append(")");
				}
			}
			/* Must be tested before
			if (field.pattern() != null) {
				builder.append(".regex((");
				builder.append(field.pattern().regexp());
				builder.append(")");
			}*/
			/* Must be tested before
			if (field.email() != null) {
				builder.append(".regex((");
				builder.append(field.email().regexp());
				builder.append(")");
			}*/
		}
		if (clazz == short.class || clazz == Short.class || clazz == int.class || clazz == Integer.class
				|| clazz == long.class || clazz == Long.class || clazz == float.class || clazz == Float.class
				|| clazz == double.class || clazz == Double.class) {
			if (field.min() != null) {
				builder.append(".gte(");
				builder.append(field.min().value());
				builder.append(")");
			}
			if (field.max() != null) {
				builder.append(".lte(");
				builder.append(field.max().value());
				builder.append(")");
			}
			if (field.decimalMax() != null) {
				if (field.decimalMax().inclusive()) {
					builder.append(".lte(");
				} else {
					builder.append(".lt(");
				}
				builder.append(field.decimalMax().value());
				builder.append(")");
			}
			if (field.decimalMin() != null) {
				if (field.decimalMin().inclusive()) {
					builder.append(".gte(");
				} else {
					builder.append(".gt(");
				}
				builder.append(field.decimalMin().value());
				builder.append(")");
			}
		}
		return builder.toString();
	}

	public String readOnlyZod(final FieldProperty field) {
		if (!field.accessLimitation().creatable() && !field.accessLimitation().updatable()) {
			return ".readonly()";
		}
		return "";
	}

	public String generateBaseObject() {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append("\n");

		out.append("export const ");
		out.append(this.aaaaaaaaaaaaaa_zodName);
		out.append(" = ");
		out.append(this.declaration);
		out.append(";");
		out.append(generateZodInfer(this.aaaaaaaaaaaaaa_tsTypeName, this.aaaaaaaaaaaaaa_zodName));
		return out.toString();
	}

	public String generateObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) {
		final StringBuilder out = new StringBuilder();

		out.append(getBaseHeader());
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  Review this !!!!!!
		out.append(generateImports(model.getDependencyModels(), tsGroup));
		out.append("\n");
		for (final ParameterClassModel elem : this.requestedModels) {
			out.append(generateObjectLimited(model, tsGroup, elem));
		}

		return out.toString();
	}

	public String generateObjectLimited(
			final ClassObjectModel model,
			final TsClassElementGroup tsGroup,
			final ParameterClassModel parameterClassModel) {
		final StringBuilder out = new StringBuilder();
		out.append(generateComment(model));
		out.append("// ... " + parameterClassModel.valid() + " ... [");
		for (final Class<?> elem : parameterClassModel.groups()) {
			out.append(elem.getSimpleName() + ", ");
		}
		out.append("]\n");
		out.append("export const ");
		out.append(this.getZodName(parameterClassModel));
		out.append(" = ");
		// Check if the object is empty:
		final boolean isEmpty = model.getFields().size() == 0;

		if (model.getExtendsClass() != null) {
			final ClassModel parentClass = model.getExtendsClass();
			final TsClassElement tsParentModel = tsGroup.find(parentClass);
			out.append(tsParentModel.getZodName(parameterClassModel.valid(), parameterClassModel.groups()));
			if (!isEmpty) {
				out.append(".extend({\n");
			}
		} else {
			out.append("zod.object({\n");
		}
		for (final FieldProperty field : model.getFields()) {
			if (!isCompatibleField(field, parameterClassModel.valid(), parameterClassModel.groups())) {
				continue;
			}
			// remove all readOnly field
			//			if (!field.accessLimitation().readable()) {
			//				continue;
			//			}
			final ClassModel fieldModel = field.model();
			if (field.comment() != null) {
				out.append("\t/**\n");
				out.append("\t * ");
				out.append(field.comment());
				out.append("\n\t */\n");
			}
			out.append("\t");
			out.append(field.name());
			out.append(": ");
			if (fieldModel instanceof ClassEnumModel || fieldModel instanceof ClassObjectModel) {
				final TsClassElement tsFieldModel = tsGroup.find(fieldModel);
				out.append(tsFieldModel.aaaaaaaaaaaaaa_zodName);
			} else if (fieldModel instanceof final ClassListModel fieldListModel) {
				final String data = generateTsList(fieldListModel, tsGroup);
				out.append(data);
			} else if (fieldModel instanceof final ClassMapModel fieldMapModel) {
				final String data = generateTsMap(fieldMapModel, tsGroup);
				out.append(data);
			}
			out.append(maxSizeZod(field));
			out.append(readOnlyZod(field));
			out.append(optionalTypeZod(field));
			out.append(",\n");
		}
		if (model.getExtendsClass() != null && isEmpty) {
			out.append(";\n");
		} else {
			out.append("\n});\n");
		}
		out.append(generateZodInfer(this.getTypeName(parameterClassModel), this.getZodName(parameterClassModel)));
		out.append(generateExportCheckFunctionAppended("", parameterClassModel));
		return out.toString();
	}
	/*
		public String generateObjectUpdate(final ClassObjectModel model, final TsClassElementGroup tsGroup) {
			final StringBuilder out = new StringBuilder();
			final String modeleType = "";//MODEL_TYPE_UPDATE;
			out.append("export const ");
			out.append(this.zodName);
			out.append(modeleType);
			out.append(" = ");
			// Check if at minimum One fiend is updatable to generate the local object
			final boolean isEmpty = model.getFields().stream().filter(field -> field.accessLimitation().updatable())
					.count() == 0;
			if (model.getExtendsClass() != null) {
				final ClassModel parentClass = model.getExtendsClass();
				final TsClassElement tsParentModel = tsGroup.find(parentClass);
				out.append(tsParentModel.zodName);
				out.append(modeleType);
				if (!isEmpty) {
					out.append(".extend({\n");
				}
			} else {
				out.append("zod.object({\n");
			}
			for (final FieldProperty field : model.getFields()) {
				// remove all readOnly field
				if (!field.accessLimitation().updatable()) {
					continue;
				}
				final ClassModel fieldModel = field.model();
				if (field.comment() != null) {
					out.append("\t/**\n");
					out.append("\t * ");
					out.append(field.comment());
					out.append("\n\t * /\n");
				}
				out.append("\t");
				out.append(field.name());
				out.append(": ");
				if (fieldModel instanceof ClassEnumModel || fieldModel instanceof ClassObjectModel) {
					final TsClassElement tsFieldModel = tsGroup.find(fieldModel);
					out.append(tsFieldModel.zodName);
				} else if (fieldModel instanceof final ClassListModel fieldListModel) {
					final String data = generateTsList(fieldListModel, tsGroup);
					out.append(data);
				} else if (fieldModel instanceof final ClassMapModel fieldMapModel) {
					final String data = generateTsMap(fieldMapModel, tsGroup);
					out.append(data);
				}
				out.append(maxSizeZod(field));
				out.append(optionalWriteTypeZod(field));
				out.append(optionalTypeZod(field));
				out.append(",\n");
			}
			if (model.getExtendsClass() != null && isEmpty) {
				out.append(";\n");
			} else {
				out.append("\n});\n");
			}
			out.append(generateZodInfer(this.tsTypeName + modeleType, this.zodName + modeleType));
			// Check only the input value ==> no need of the output
			out.append(generateExportCheckFunctionAppended(modeleType));
			return out.toString();
		}

		public String generateObjectCreate(final ClassObjectModel model, final TsClassElementGroup tsGroup) {
			final StringBuilder out = new StringBuilder();
			final String modeleType = "";//MODEL_TYPE_CREATE;
			out.append("export const ");
			out.append(this.zodName);
			out.append(modeleType);
			out.append(" = ");
			final boolean isEmpty = model.getFields().stream().filter(field -> field.accessLimitation().creatable())
					.count() == 0;
			if (model.getExtendsClass() != null) {
				final ClassModel parentClass = model.getExtendsClass();
				final TsClassElement tsParentModel = tsGroup.find(parentClass);
				out.append(tsParentModel.zodName);
				out.append(modeleType);
				if (!isEmpty) {
					out.append(".extend({\n");
				}
			} else {
				out.append("zod.object({\n");
			}
			for (final FieldProperty field : model.getFields()) {
				// remove all readOnly field
				if (!field.accessLimitation().creatable()) {
					continue;
				}
				final ClassModel fieldModel = field.model();
				if (field.comment() != null) {
					out.append("\t/**\n");
					out.append("\t * ");
					out.append(field.comment());
					out.append("\n\t * /\n");
				}
				out.append("\t");
				out.append(field.name());
				out.append(": ");
				if (fieldModel instanceof ClassEnumModel || fieldModel instanceof ClassObjectModel) {
					final TsClassElement tsFieldModel = tsGroup.find(fieldModel);
					out.append(tsFieldModel.zodName);
				} else if (fieldModel instanceof final ClassListModel fieldListModel) {
					final String data = generateTsList(fieldListModel, tsGroup);
					out.append(data);
				} else if (fieldModel instanceof final ClassMapModel fieldMapModel) {
					final String data = generateTsMap(fieldMapModel, tsGroup);
					out.append(data);
				}
				out.append(maxSizeZod(field));
				out.append(optionalWriteTypeZod(field));
				out.append(optionalTypeZod(field));
				out.append(",\n");
			}
			if (model.getExtendsClass() != null && isEmpty) {
				out.append(";\n");
			} else {
				out.append("\n});\n");
			}
			out.append(generateZodInfer(this.tsTypeName + modeleType, this.zodName + modeleType));
			// Check only the input value ==> no need of the output
			out.append(generateExportCheckFunctionAppended(modeleType));
			return out.toString();
		}
	*/

	private static String generateZodInfer(final String tsName, final String zodName) {
		final StringBuilder out = new StringBuilder();
		out.append("\nexport type ");
		out.append(tsName);
		out.append(" = zod.infer<typeof ");
		out.append(zodName);
		out.append(">;\n");
		return out.toString();
	}

	private static String generateTsMap(final ClassMapModel model, final TsClassElementGroup tsGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("zod.record(");
		if (model.keyModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, tsGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, tsGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, tsGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, tsGroup);
			out.append(tmp);
		}
		out.append(", ");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, tsGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, tsGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, tsGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, tsGroup);
			out.append(tmp);
		}
		out.append(")");
		return out.toString();
	}

	private static String generateTsEnum(final ClassEnumModel model, final TsClassElementGroup tsGroup) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		return tsParentModel.aaaaaaaaaaaaaa_zodName;
	}

	private static String generateTsObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		return tsParentModel.aaaaaaaaaaaaaa_zodName;
	}

	private static String generateTsList(final ClassListModel model, final TsClassElementGroup tsGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("zod.array(");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, tsGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, tsGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, tsGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, tsGroup);
			out.append(tmp);
		}
		out.append(")");
		return out.toString();
	}

	public void generateFile(final TsClassElementGroup tsGroup, final Map<Path, String> generation) {
		if (this.nativeType == DefinedPosition.NATIVE) {
			return;
		}
		final ClassModel model = this.models.get(0);
		String data = "";
		if (this.nativeType == DefinedPosition.BASIC && model instanceof ClassObjectModel) {
			data = generateBaseObject();
		} else if (model instanceof final ClassEnumModel modelEnum) {
			data = generateEnum(modelEnum, tsGroup);
		} else if (model instanceof final ClassObjectModel modelObject) {
			data = generateObject(modelObject, tsGroup);
		}
		generation.put(Paths.get("model").resolve(this.fileName + ".ts"), data);
	}

	private static String generateLocalModelBase(final ClassModel model, final TsClassElementGroup tsGroup) {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateTsObject(objectModel, tsGroup);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateTsEnum(enumModel, tsGroup);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateTsList(listModel, tsGroup);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateTsMap(mapModel, tsGroup);
		}
		return "";
	}

	public static String generateLocalModel(
			final String modelName,
			final List<ClassModel> models,
			final TsClassElementGroup tsGroup) {
		if (models.size() == 1) {
			if (models.get(0) instanceof ClassObjectModel) {
				return null;
			}
			if (models.get(0) instanceof ClassEnumModel) {
				return null;
			}
		}
		final StringBuilder out = new StringBuilder();
		out.append("export const Zod");
		out.append(modelName);
		out.append(" = ");
		if (models.size() == 1) {
			out.append(generateLocalModelBase(models.get(0), tsGroup));
			out.append(";");
		} else {
			out.append("z.union([\n");
			for (final ClassModel model : models) {
				out.append("\t");
				out.append(generateLocalModelBase(model, tsGroup));
				out.append(",\n");
			}
			out.append("]);");
		}
		//model.getDependencyModels()
		out.append(generateZodInfer(modelName, "Zod" + modelName));
		out.append(generateExportCheckFunction("is" + modelName, modelName, "Zod" + modelName));
		return out.toString();
	}

	public ParameterClassModel getParameterClassModel(
			final boolean valid,
			final Class<?>[] validGroup,
			final ClassModel parameterModel) {
		// TODO: je viens de cree un cycle ...
		final ParameterClassModel tmp = new ParameterClassModel(valid, validGroup, parameterModel);
		for (final ParameterClassModel elem : this.requestedModels) {
			if (elem.equals(tmp)) {
				return elem;
			}
		}
		this.requestedModels.add(tmp);
		return tmp;
	}

	public ParameterClassModel getParameterClassModel(
			final Valid validParam,
			final ValidGroup validGroupParam,
			final ClassModel parameterModel) {
		// TODO: je viens de cree un cycle ...
		final ParameterClassModel tmp = new ParameterClassModel(validParam, validGroupParam, parameterModel);
		for (final ParameterClassModel elem : this.requestedModels) {
			if (elem.equals(tmp)) {
				return elem;
			}
		}
		this.requestedModels.add(tmp);
		return tmp;
	}

	public ParameterClassModel getParameterClassModel(final ClassModel parameterModel) {
		// TODO: je viens de cree un cycle ...
		final ParameterClassModel tmp = new ParameterClassModel(parameterModel);
		for (final ParameterClassModel elem : this.requestedModels) {
			if (elem.equals(tmp)) {
				return elem;
			}
		}
		this.requestedModels.add(tmp);
		return tmp;
	}

}
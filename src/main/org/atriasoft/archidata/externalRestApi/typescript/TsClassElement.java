package org.atriasoft.archidata.externalRestApi.typescript;

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

import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.atriasoft.archidata.externalRestApi.typescript.ImportModel.ModeImport;
import org.atriasoft.archidata.externalRestApi.typescript.ImportModel.PairElem;
import org.atriasoft.archidata.tools.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

public class TsClassElement {
	static final Logger LOGGER = LoggerFactory.getLogger(TsClassElement.class);

	public enum DefinedPosition {
		NATIVE, // Native element of TS language.
		BASIC, // basic wrapping for JAVA type.
		NORMAL // Normal Object to interpret.
	}

	public List<ClassModel> models;
	public String zodName;
	private final String tsTypeName;
	private final String tsCheckType;
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
		this.zodName = zodName;
		this.tsTypeName = tsTypeName;
		this.tsCheckType = tsCheckType;
		this.declaration = declaration;
		this.nativeType = nativeType;
		this.fileName = determineFileName(tsTypeName);
	}

	public TsClassElement(final ClassModel model, final boolean forceMode) {
		this.models = List.of(model);
		if (forceMode) {
			this.zodName = "Zod" + model.getOriginClasses().getSimpleName();
			this.tsTypeName = model.getOriginClasses().getSimpleName();
			this.tsCheckType = "is" + model.getOriginClasses().getSimpleName();
		} else {
			this.zodName = null;
			this.tsTypeName = null;
			this.tsCheckType = null;
		}
		this.declaration = null;
		this.fileName = determineFileName(model.getOriginClasses().getSimpleName());
	}

	public TsClassElement(final ClassModel model) {
		this.models = List.of(model);
		this.zodName = "Zod" + model.getOriginClasses().getSimpleName();
		this.tsTypeName = model.getOriginClasses().getSimpleName();
		this.tsCheckType = "is" + model.getOriginClasses().getSimpleName();
		this.declaration = null;
		this.fileName = determineFileName(this.tsTypeName);
	}

	public String getZodName() {
		final ParameterClassModel model = getParameterClassModel(this.models.get(0));
		if (this.zodName != null) {
			return this.zodName;
		}
		return "Zod" + model.getType();
	}

	public String getZodName(final boolean isValid, final Class<?>[] groups) {
		final ParameterClassModel model = getParameterClassModel(isValid, groups, this.models.get(0));
		if (this.zodName != null) {
			return this.zodName;
		}
		return "Zod" + model.getType();
	}

	public String getZodName(final ParameterClassModel model) {
		getParameterClassModel(model.valid(), model.groups(), model.model());
		if (this.zodName != null) {
			return this.zodName;
		}
		return "Zod" + model.getType();
	}

	public String getTypeName() {
		final ParameterClassModel model = getParameterClassModel(this.models.get(0));
		if (this.tsTypeName != null) {
			return this.tsTypeName;
		}
		return model.getType();
	}

	public String getTypeName(final boolean isValid, final Class<?>[] groups) {
		final ParameterClassModel model = getParameterClassModel(isValid, groups, this.models.get(0));
		if (this.tsTypeName != null) {
			return this.tsTypeName;
		}
		return model.getType();
	}

	public String getTypeName(final ParameterClassModel model) {
		this.requestedModels.add(model);
		if (this.tsTypeName != null) {
			return this.tsTypeName;
		}
		return model.getType();
	}

	public String getCheckType(final boolean isValid, final Class<?>[] groups) {
		final ParameterClassModel model = getParameterClassModel(isValid, groups, this.models.get(0));
		if (this.tsCheckType != null) {
			return this.tsCheckType;
		}
		return "is" + model.getType();
	}

	public String getCheckType() {
		final ParameterClassModel model = getParameterClassModel(this.models.get(0));
		if (this.tsCheckType != null) {
			return this.tsCheckType;
		}
		return "is" + model.getType();
	}

	public String getCheckType(final ParameterClassModel model) {
		if (model != null) {
			this.requestedModels.add(model);
		}
		if (this.tsCheckType != null) {
			return this.tsCheckType;
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
		// out.append(generateComment(model));
		if (System.getenv("ARCHIDATA_GENERATE_ZOD_ENUM") != null) {
			boolean first = true;
			out.append("export const ");
			out.append(this.tsTypeName);
			out.append(" = ");
			out.append("zod.enum([");
			for (final Entry<String, Object> elem : model.getListOfValues().entrySet()) {
				if (first) {
					out.append("\n");
					first = false;
				}
				out.append("\t'");
				out.append(elem.getKey());
				out.append("',\n");
			}
			if (first) {
				out.append(" ");
			}
			out.append("\n\t]);\n");
			out.append(generateZodInfer(this.getTypeName(), this.getZodName()));
		} else {
			boolean first = true;
			out.append("export enum ");
			out.append(this.getTypeName());
			out.append("  {");
			for (final Entry<String, Object> elem : model.getListOfValues().entrySet()) {
				if (first) {
					out.append("\n");
					first = false;
				}
				out.append("\t");
				out.append(elem.getKey());
				out.append(" = ");
				if (elem.getValue() instanceof final Integer value) {
					out.append(value);
				} else {
					out.append("'");
					out.append(elem.getValue());
					out.append("'");
				}
				out.append(",\n");
			}
			if (first) {
				out.append(" ");
			}
			out.append("};\n");
			out.append("\nexport const ");
			out.append(this.getZodName());
			out.append(" = zod.nativeEnum(");
			out.append(this.getTypeName());
			out.append(");\n");
		}
		out.append(generateExportCheckFunctionAppended("", new ParameterClassModel(model)));
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
						console.log(`Fail to parse data type='Zod""");
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

	public String generateImports(final ImportModel imports, final TsClassElementGroup tsGroup) {
		final Map<String, String> mapOutput = new TreeMap<>();
		// TODO: order alphabetical order...
		final Map<String, ClassModel> orderedMap = new TreeMap<>();

		for (final Entry<ClassModel, Set<PairElem>> depModel : imports.data.entrySet()) {
			final TsClassElement tsModel = tsGroup.find(depModel.getKey());
			orderedMap.put(tsModel.fileName, depModel.getKey());
		}
		for (final Entry<String, ClassModel> importOrdered : orderedMap.entrySet()) {
			final ClassModel keyModel = importOrdered.getValue();
			final Set<PairElem> elementImportForThisFile = imports.data.get(keyModel);
			final StringBuilder inputStream = new StringBuilder();
			final TsClassElement tsModel = tsGroup.find(keyModel);
			if (tsModel.nativeType != DefinedPosition.NATIVE) {
				inputStream.append("import {");
				final List<String> elements = new ArrayList<>();
				for (final PairElem pair : elementImportForThisFile) {
					final ParameterClassModel modelSpecialized = new ParameterClassModel(pair.valid(), pair.groups(),
							keyModel);
					switch (pair.mode()) {
						case ModeImport.IS:
							elements.add(tsModel.getCheckType(modelSpecialized));
							break;
						case ModeImport.TYPE:
							elements.add(tsModel.getTypeName(modelSpecialized));
							break;
						case ModeImport.ZOD:
							elements.add(tsModel.getZodName(modelSpecialized));
							break;
					}
				}
				if (elements.size() == 0) {
					throw new RuntimeException("Impossible case of no element in the imports...");
				}
				if (elements.size() == 1) {
					inputStream.append(" ");
					inputStream.append(elements.get(0));
					inputStream.append(" ");
				} else {
					inputStream.append("\n");
					Collections.sort(elements);
					for (final String elem : elements) {
						inputStream.append("\t");
						inputStream.append(elem);
						inputStream.append(",\n");
					}
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

	private boolean isCompatibleGroup(final Class<?>[] groupsA, final Class<?>[] groupsB) {
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

	public boolean isCompatibleField(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		if (field.annotationNotNull() != null) {
			// if it is not null in the specific group, this mean the element MUST be
			// transcript
			if (isCompatibleGroup(field.annotationNotNull().groups(), groups)) {
				// TODO: maybe check if the element does not exist in the other item ...
				return true;
			}
			// check if no element
			if ((field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0)
					&& isValid) {
				return true;
			}
		}
		if (field.annotationNull() != null) {
			// if it is not null in the specific group, this mean the element MUST NOT be
			// transcript (is is NULL)
			if (isCompatibleGroup(field.annotationNull().groups(), groups)) {
				// TODO: maybe check if the element does not exist in the other item ...
				return false;
			}
			if ((field.annotationNull().groups() == null || field.annotationNull().groups().length == 0) && isValid) {
				return false;
			}
		}
		return isValid;
	}

	public boolean isOptionalTypeZod(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		if (field.apiNotNull() != null) {
			return !field.apiNotNull().value();
		}
		// Common checking element (apply to List, Map, ...)
		if (field.nullable()) {
			return true;
		}
		if (field.annotationNotNull() != null) {
			if (isCompatibleGroup(field.annotationNotNull().groups(), groups)) {
				return false;
			}
			if ((field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0)
					&& isValid) {
				return false;
			}
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

	public String optionalTypeZod(final FieldProperty field, final boolean isValid, final Class<?>[] groups) {
		if (isOptionalTypeZod(field, isValid, groups)) {
			return ".optional()";
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
			/*
			 * Must be tested before if (field.pattern() != null) {
			 * builder.append(".regex(("); builder.append(field.pattern().regexp());
			 * builder.append(")"); }
			 */
			/*
			 * Must be tested before if (field.email() != null) {
			 * builder.append(".regex(("); builder.append(field.email().regexp());
			 * builder.append(")"); }
			 */
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
		if (field.apiReadOnly() != null) {
			return ".readonly()";
		}
		return "";
	}

	public String generateBaseObject() {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append("\n");

		out.append("export const ");
		out.append(this.getZodName());
		out.append(" = ");
		out.append(this.declaration);
		out.append(";");
		out.append(generateZodInfer(this.tsTypeName, this.zodName));
		return out.toString();
	}

	public String generateObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) {

		// the key is the fileName
		final ImportModel imports = new ImportModel();

		final StringBuilder outData = new StringBuilder();
		final Map<String, ParameterClassModel> orderedElement = new TreeMap<>();
		for (final ParameterClassModel elem : this.requestedModels) {
			orderedElement.put(this.getZodName(elem), elem);
		}
		boolean isFirst = true;
		if (orderedElement.isEmpty()) {
			outData.append(generateObjectLimited(model, tsGroup, new ParameterClassModel(model), imports, true));
		} else {
			for (final Entry<String, ParameterClassModel> elem : orderedElement.entrySet()) {
				outData.append("\n");
				outData.append(generateObjectLimited(model, tsGroup, elem.getValue(), imports, isFirst));
				isFirst = false;
			}
		}
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append(generateImports(imports, tsGroup));
		out.append(outData.toString());
		return out.toString();
	}

	public String generateObjectLimited(
			final ClassObjectModel model,
			final TsClassElementGroup tsGroup,
			final ParameterClassModel parameterClassModel,
			final ImportModel imports,
			final boolean displayHelp) {
		final StringBuilder out = new StringBuilder();
		if (displayHelp) {
			out.append(generateComment(model));
		}
		out.append("export const ");
		out.append(this.getZodName(parameterClassModel));
		out.append(" = ");
		final StringBuilder outContent = new StringBuilder();
		final StringBuilder outContentEmpty = new StringBuilder();
		if (model.getExtendsClass() != null) {
			final ClassModel parentClass = model.getExtendsClass();
			if (imports != null) {
				imports.addZod(parameterClassModel.valid(), parameterClassModel.groups(), parentClass);
			}
			final TsClassElement tsParentModel = tsGroup.find(parentClass);
			final String dataParent = tsParentModel.getZodName(parameterClassModel.valid(),
					parameterClassModel.groups());
			outContent.append(dataParent);
			outContent.append(".extend({\n");
			outContentEmpty.append(dataParent);
			outContentEmpty.append(";\n");
		} else {
			outContent.append("zod.object({\n");
		}
		boolean hasField = false;
		for (final FieldProperty field : model.getFields()) {
			if (!isCompatibleField(field, parameterClassModel.valid(), parameterClassModel.groups())) {
				continue;
			}
			hasField = true;
			final ClassModel fieldModel = field.model();
			if (field.comment() != null) {
				outContent.append("\t/**\n");
				outContent.append("\t * ");
				outContent.append(field.comment());
				outContent.append("\n\t */\n");
			}
			outContent.append("\t");
			outContent.append(field.name());
			outContent.append(": ");
			if (fieldModel instanceof ClassEnumModel || fieldModel instanceof ClassObjectModel) {
				final TsClassElement tsFieldModel = tsGroup.find(fieldModel);
				outContent.append(tsFieldModel.getZodName());
				if (imports != null) {
					imports.addZod(tsFieldModel.models.get(0));
				}
			} else if (fieldModel instanceof final ClassListModel fieldListModel) {
				final String data = generateTsList(fieldListModel, tsGroup, imports);
				outContent.append(data);
			} else if (fieldModel instanceof final ClassMapModel fieldMapModel) {
				final String data = generateTsMap(fieldMapModel, tsGroup, imports);
				outContent.append(data);
			}
			outContent.append(maxSizeZod(field));
			outContent.append(readOnlyZod(field));
			outContent.append(optionalTypeZod(field, parameterClassModel.valid(), parameterClassModel.groups()));
			outContent.append(",\n");
		}
		if (model.getExtendsClass() != null) {
			outContent.append("});\n");
		} else {
			outContent.append("\n});\n");
		}
		if (!hasField && model.getExtendsClass() != null) {
			out.append(outContentEmpty);
		} else {
			out.append(outContent);
		}
		out.append(generateZodInfer(this.getTypeName(parameterClassModel), this.getZodName(parameterClassModel)));
		out.append(generateExportCheckFunctionAppended("", parameterClassModel));
		return out.toString();
	}

	private static String generateZodInfer(final String tsName, final String zodName) {
		final StringBuilder out = new StringBuilder();
		out.append("\nexport type ");
		out.append(tsName);
		out.append(" = zod.infer<typeof ");
		out.append(zodName);
		out.append(">;\n");
		return out.toString();
	}

	private static String generateTsMap(
			final ClassMapModel model,
			final TsClassElementGroup tsGroup,
			final ImportModel imports) {
		final StringBuilder out = new StringBuilder();
		out.append("zod.record(");
		if (model.keyModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, tsGroup, imports);
			out.append(tmp);
		}
		out.append(", ");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, tsGroup, imports);
			out.append(tmp);
		}
		out.append(")");
		return out.toString();
	}

	private static String generateTsEnum(
			final ClassEnumModel model,
			final TsClassElementGroup tsGroup,
			final ImportModel imports) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		if (imports != null) {
			imports.addZod(model);
		}
		return tsParentModel.getZodName();
	}

	private static String generateTsObject(
			final ClassObjectModel model,
			final TsClassElementGroup tsGroup,
			final ImportModel imports) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		if (imports != null) {
			imports.addZod(model);
		}
		return tsParentModel.getZodName();
	}

	private static String generateTsList(
			final ClassListModel model,
			final TsClassElementGroup tsGroup,
			final ImportModel imports) {
		final StringBuilder out = new StringBuilder();
		imports.requestZod();
		out.append("zod.array(");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, tsGroup, imports);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, tsGroup, imports);
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

	private static String generateLocalModelBase(
			final ClassModel model,
			final TsClassElementGroup tsGroup,
			final ImportModel imports) {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateTsObject(objectModel, tsGroup, imports);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateTsEnum(enumModel, tsGroup, imports);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateTsList(listModel, tsGroup, imports);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateTsMap(mapModel, tsGroup, imports);
		}
		return "";
	}

	public static String generateLocalModel(
			final String modelName,
			final List<ClassModel> models,
			final TsClassElementGroup tsGroup,
			final ImportModel imports) {
		if (models.size() >= 1) {
			if (models.get(0) instanceof ClassObjectModel) {
				return null;
			}
			if (models.get(0) instanceof ClassEnumModel) {
				return null;
			}
			if (models.get(0).getOriginClasses() == Void.class || models.get(0).getOriginClasses() == void.class) {
				return null;
			}
		}
		imports.requestZod();
		final TsClassElement tsModel = tsGroup.find(models.get(0));
		final StringBuilder out = new StringBuilder();
		out.append("export const Zod");
		out.append(modelName);
		// out.append("export const /* check if it is the good type !!! */ ");
		// out.append(tsModel.getZodName());
		// imports.addZod(models.get(0));
		out.append(" = ");
		if (models.size() == 1) {
			out.append(generateLocalModelBase(models.get(0), tsGroup, imports));
			out.append(";");
		} else {
			out.append("z.union([\n");
			for (final ClassModel model : models) {
				out.append("\t");
				out.append(generateLocalModelBase(model, tsGroup, imports));
				out.append(",\n");
			}
			out.append("]);");
		}
		// model.getDependencyModels()
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

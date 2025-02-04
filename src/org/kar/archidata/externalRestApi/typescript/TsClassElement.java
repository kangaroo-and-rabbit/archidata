package org.kar.archidata.externalRestApi.typescript;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;

import org.kar.archidata.externalRestApi.model.ClassEnumModel;
import org.kar.archidata.externalRestApi.model.ClassListModel;
import org.kar.archidata.externalRestApi.model.ClassMapModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TsClassElement {
	static final Logger LOGGER = LoggerFactory.getLogger(TsClassElement.class);

	public enum DefinedPosition {
		NATIVE, // Native element of  TS language.
		BASIC, // basic wrapping for JAVA type.
		NORMAL // Normal Object to interpret.
	}

	public List<ClassModel> models;
	public String zodName;
	public String tsTypeName;
	public String tsCheckType;
	public String declaration;
	public String fileName = null;
	public String comment = null;
	public DefinedPosition nativeType = DefinedPosition.NORMAL;

	public static String determineFileName(final String className) {
		return className.replaceAll("([a-z])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2").toLowerCase();
	}

	public TsClassElement(final List<ClassModel> model, final String zodName, final String tsTypeName,
			final String tsCheckType, final String declaration, final DefinedPosition nativeType) {
		this.models = model;
		this.zodName = zodName;
		this.tsTypeName = tsTypeName;
		this.tsCheckType = tsCheckType;
		this.declaration = declaration;
		this.nativeType = nativeType;
		this.fileName = determineFileName(tsTypeName);
	}

	public TsClassElement(final ClassModel model) {
		this.models = List.of(model);
		this.zodName = "Zod" + model.getOriginClasses().getSimpleName();
		this.tsTypeName = model.getOriginClasses().getSimpleName();
		this.tsCheckType = "is" + model.getOriginClasses().getSimpleName();
		this.declaration = null;
		this.fileName = determineFileName(this.tsTypeName);
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

	public String generateEnum(final ClassEnumModel model, final TsClassElementGroup tsGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append("\n");
		//out.append(generateComment(model));

		if (System.getenv("ARCHIDATA_GENERATE_ZOD_ENUM") != null) {
			boolean first = true;
			out.append("export const ");
			out.append(this.tsTypeName);
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
			out.append(generateZodInfer(this.tsTypeName, this.zodName));
		} else {
			boolean first = true;
			out.append("export enum ");
			out.append(this.tsTypeName);
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
			out.append(this.zodName);
			out.append(" = zod.nativeEnum(");
			out.append(this.tsTypeName);
			out.append(");\n");
		}
		out.append(generateExportCheckFunctionWrite(""));
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
		out.append(zodName);
		out.append("""
				' error=${e}`);
						return false;
					}
				}
				""");
		return out.toString();
	}

	private String generateExportCheckFunctionWrite(final String writeString) {
		return generateExportCheckFunction(this.tsCheckType + writeString, this.tsTypeName + writeString,
				this.zodName + writeString);
	}

	public String generateImports(final List<ClassModel> depModels, final TsClassElementGroup tsGroup)
			throws IOException {
		final StringBuilder out = new StringBuilder();
		for (final ClassModel depModel : depModels) {
			final TsClassElement tsModel = tsGroup.find(depModel);
			if (tsModel.nativeType != DefinedPosition.NATIVE) {
				out.append("import {");
				out.append(tsModel.zodName);
				if (tsModel.nativeType == DefinedPosition.NORMAL && !(tsModel.models.get(0).isNoWriteSpecificMode())) {
					out.append(", ");
					out.append(tsModel.zodName);
					out.append("Write ");
				}
				out.append("} from \"./");
				out.append(tsModel.fileName);
				out.append("\";\n");
			}
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

	public boolean isOptionalTypeZod(final FieldProperty field) {
		// Common checking element (apply to List, Map, ...)
		if (field.nullable()) {
			return true;
		}
		if (field.notNull()) {
			return false;
		}
		// Other object:
		if (field.model().getOriginClasses() == null || field.model().getOriginClasses().isPrimitive()) {
			return false;
		}
		if (field.columnNotNull()) {
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
			/*Must be tested before
			if (field.pattern() != null) {
				builder.append(".regex((");
				builder.append(field.pattern().regexp());
				builder.append(")");
			}*/
			/*Must be tested before
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
		if (field.readOnly()) {
			return ".readonly()";
		}
		return "";
	}

	public String generateBaseObject() {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append("\n");

		out.append("export const ");
		out.append(this.zodName);
		out.append(" = ");
		out.append(this.declaration);
		out.append(";");
		out.append(generateZodInfer(this.tsTypeName, this.zodName));
		return out.toString();
	}

	public String generateObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append(generateImports(model.getDependencyModels(), tsGroup));
		out.append("\n");
		// ------------------------------------------------------------------------
		// -- Generate read mode
		// ------------------------------------------------------------------------
		out.append(generateComment(model));
		out.append("export const ");
		out.append(this.zodName);
		out.append(" = ");
		// Check if the object is empty:
		boolean isEmpty = model.getFields().size() == 0;

		if (model.getExtendsClass() != null) {
			final ClassModel parentClass = model.getExtendsClass();
			final TsClassElement tsParentModel = tsGroup.find(parentClass);
			out.append(tsParentModel.zodName);
			if (!isEmpty) {
				out.append(".extend({\n");
			}
		} else {
			out.append("zod.object({\n");
		}
		for (final FieldProperty field : model.getFields()) {
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
				out.append(tsFieldModel.zodName);
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
		final List<String> omitField = model.getReadOnlyField();
		if (model.getExtendsClass() != null && isEmpty) {
			out.append(";\n");
		} else {
			out.append("\n});\n");
		}
		out.append(generateZodInfer(this.tsTypeName, this.zodName));
		out.append(generateExportCheckFunctionWrite(""));
		// check if we need to generate write mode :
		if (!model.isNoWriteSpecificMode()) {
			// ------------------------------------------------------------------------
			// -- Generate write mode
			// ------------------------------------------------------------------------
			//out.append(generateComment(model));
			out.append("export const ");
			out.append(this.zodName);
			out.append("Write = ");
			isEmpty = model.getFields().stream().filter(field -> !field.readOnly()).count() == 0;
			if (model.getExtendsClass() != null) {
				final ClassModel parentClass = model.getExtendsClass();
				final TsClassElement tsParentModel = tsGroup.find(parentClass);
				out.append(tsParentModel.zodName);
				out.append("Write");
				if (!isEmpty) {
					out.append(".extend({\n");
				}
			} else {
				out.append("zod.object({\n");
			}
			for (final FieldProperty field : model.getFields()) {
				// remove all readOnly field
				if (field.readOnly()) {
					continue;
				}
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
				// all write field are optional
				if (field.model() instanceof final ClassObjectModel plop) {
					if (!plop.isPrimitive()) {
						out.append(".optional()");
					}
				} else {
					out.append(".optional()");
				}
				out.append(",\n");
			}
			if (model.getExtendsClass() != null && isEmpty) {
				out.append(";\n");
			} else {
				out.append("\n});\n");
			}
			out.append(generateZodInfer(this.tsTypeName + "Write", this.zodName + "Write"));
			// Check only the input value ==> no need of the output
			out.append(generateExportCheckFunctionWrite("Write"));
			// Generate the Write Type associated.
			/*
			out.append("\nexport const ");
			out.append(this.zodName);
			out.append("Write = ");
			out.append(this.zodName);
			if (omitField.size() != 0) {
				out.append(".omit({\n");
				for (final String elem : omitField) {
					out.append("\t");
					out.append(elem);
					out.append(": true,\n");
				}
				out.append("\n})");
			}
			out.append(".partial();\n");
			*/
		}
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
		return tsParentModel.zodName;
	}

	private static String generateTsObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		return tsParentModel.zodName;
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
		}
		out.append(")");
		return out.toString();
	}

	public void generateFile(final String pathPackage, final TsClassElementGroup tsGroup) throws IOException {
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
		final Path path = Paths.get(pathPackage + File.separator + "model");
		if (Files.notExists(path)) {
			Files.createDirectories(path);
		}
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "model" + File.separator + this.fileName + ".ts");
		myWriter.write(data);
		myWriter.close();
	}

	private static String generateLocalModelBase(final ClassModel model, final TsClassElementGroup tsGroup)
			throws IOException {
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
			final String ModelName,
			final List<ClassModel> models,
			final TsClassElementGroup tsGroup) throws IOException {
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
		out.append(ModelName);
		out.append(" = ");
		if (models.size() == 1) {
			out.append(generateLocalModelBase(models.get(0), tsGroup));
			out.append(";");
		} else {
			out.append("z.union([\n");
			for (final ClassModel model : models) {
				out.append("\t");
				out.append(generateLocalModelBase(models.get(0), tsGroup));
				out.append(",\n");
			}
			out.append("]);");
		}
		//model.getDependencyModels()
		out.append(generateZodInfer(ModelName, "Zod" + ModelName));
		out.append(generateExportCheckFunction("is" + ModelName, ModelName, "Zod" + ModelName));
		return out.toString();
	}

}
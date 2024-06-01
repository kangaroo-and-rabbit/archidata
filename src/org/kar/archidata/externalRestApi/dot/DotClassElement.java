package org.kar.archidata.externalRestApi.dot;

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

public class DotClassElement {
	static final Logger LOGGER = LoggerFactory.getLogger(DotClassElement.class);

	public enum DefinedPosition {
		NATIVE, // Native element of  dot language.
		BASIC, // basic wrapping for JAVA type.
		NORMAL // Normal Object to interpret.
	}

	public List<ClassModel> models;
	public String zodName;
	public String dotTypeName;
	public String dotCheckType;
	public String declaration;
	public String fileName = null;
	public String comment = null;
	public DefinedPosition nativeType = DefinedPosition.NORMAL;

	public static String determineFileName(final String className) {
		return className.replaceAll("([a-z])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2").toLowerCase();
	}

	public DotClassElement(final List<ClassModel> model, final String zodName, final String dotTypeName,
			final String dotCheckType, final String declaration, final DefinedPosition nativeType) {
		this.models = model;
		this.zodName = zodName;
		this.dotTypeName = dotTypeName;
		this.declaration = declaration;
		this.nativeType = nativeType;
	}

	public DotClassElement(final ClassModel model) {
		this.models = List.of(model);
		this.dotTypeName = model.getOriginClasses().getSimpleName();
		this.declaration = null;
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

	public String generateEnum(final ClassEnumModel model, final DotClassElementGroup dotGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append("\n");
		//out.append(generateComment(model));

		if (System.getenv("ARCHIDATA_GENERATE_ZOD_ENUM") != null) {
			boolean first = true;
			out.append("export const ");
			out.append(this.dotTypeName);
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
			out.append(generateZodInfer(this.dotTypeName, this.zodName));
		} else {
			boolean first = true;
			out.append("export enum ");
			out.append(this.dotTypeName);
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
			out.append(this.dotTypeName);
			out.append(");\n");
		}
		out.append(generateExportCheckFunctionWrite(""));
		return out.toString();
	}

	private static String generateExportCheckFunction(
			final String dotCheckType,
			final String dotTypeName,
			final String zodName) {
		final StringBuilder out = new StringBuilder();
		out.append("\nexport function ");
		out.append(dotCheckType);
		out.append("(data: any): data is ");
		out.append(dotTypeName);
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
		return generateExportCheckFunction(this.dotCheckType + writeString, this.dotTypeName + writeString,
				this.zodName + writeString);
	}

	public String generateImports(final List<ClassModel> depModels, final DotClassElementGroup dotGroup)
			throws IOException {
		final StringBuilder out = new StringBuilder();
		for (final ClassModel depModel : depModels) {
			final DotClassElement dotModel = dotGroup.find(depModel);
			if (dotModel.nativeType != DefinedPosition.NATIVE) {
				out.append("import {");
				out.append(dotModel.zodName);
				out.append("} from \"./");
				out.append(dotModel.fileName);
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

	public String optionalTypeZod(final FieldProperty field) {
		// Common checking element (apply to List, Map, ...)
		if (field.nullable()) {
			return ".optional()";
		}
		if (field.notNull()) {
			return "";
		}
		// Other object:
		if (field.model().getOriginClasses() == null || field.model().getOriginClasses().isPrimitive()) {
			return "";
		}
		if (field.columnNotNull()) {
			return "";
		}
		return ".optional()";
	}

	public String maxSizeZod(final FieldProperty field) {
		final StringBuilder builder = new StringBuilder();
		final Class<?> clazz = field.model().getOriginClasses();
		if (field.limitSize() > 0 && clazz == String.class) {
			builder.append(".max(");
			builder.append(field.limitSize());
			builder.append(")");
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
		out.append(generateZodInfer(this.dotTypeName, this.zodName));
		return out.toString();
	}

	public String generateObject(final ClassObjectModel model, final DotClassElementGroup dotGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append(generateImports(model.getDependencyModels(), dotGroup));
		out.append("\n");

		out.append(generateComment(model));
		out.append("export const ");
		out.append(this.zodName);
		out.append(" = ");

		if (model.getExtendsClass() != null) {
			final ClassModel parentClass = model.getExtendsClass();
			final DotClassElement dotParentModel = dotGroup.find(parentClass);
			out.append(dotParentModel.zodName);
			out.append(".extend({");
		} else {
			out.append("zod.object({");
		}
		out.append("\n");
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
				final DotClassElement dotFieldModel = dotGroup.find(fieldModel);
				out.append(dotFieldModel.zodName);
			} else if (fieldModel instanceof final ClassListModel fieldListModel) {
				final String data = generateTsList(fieldListModel, dotGroup);
				out.append(data);
			} else if (fieldModel instanceof final ClassMapModel fieldMapModel) {
				final String data = generateTsMap(fieldMapModel, dotGroup);
				out.append(data);
			}
			out.append(maxSizeZod(field));
			out.append(readOnlyZod(field));
			out.append(optionalTypeZod(field));
			out.append(",\n");
		}
		final List<String> omitField = model.getReadOnlyField();
		out.append("\n});\n");
		out.append(generateZodInfer(this.dotTypeName, this.zodName));
		out.append(generateExportCheckFunctionWrite(""));

		// Generate the Write Type associated.
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
		out.append(generateZodInfer(this.dotTypeName + "Write", this.zodName + "Write"));

		// Check only the input value ==> no need of the output
		out.append(generateExportCheckFunctionWrite("Write"));

		return out.toString();
	}

	private static String generateZodInfer(final String dotName, final String zodName) {
		final StringBuilder out = new StringBuilder();
		out.append("\nexport type ");
		out.append(dotName);
		out.append(" = zod.infer<typeof ");
		out.append(zodName);
		out.append(">;\n");
		return out.toString();
	}

	private static String generateTsMap(final ClassMapModel model, final DotClassElementGroup dotGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("zod.record(");
		if (model.keyModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, dotGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, dotGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, dotGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, dotGroup);
			out.append(tmp);
		}
		out.append(", ");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateTsEnum(fieldEnumModel, dotGroup);
			out.append(tmp);
		}
		out.append(")");
		return out.toString();
	}

	private static String generateTsEnum(final ClassEnumModel model, final DotClassElementGroup dotGroup) {
		final DotClassElement dotParentModel = dotGroup.find(model);
		return dotParentModel.zodName;
	}

	private static String generateTsObject(final ClassObjectModel model, final DotClassElementGroup dotGroup) {
		final DotClassElement dotParentModel = dotGroup.find(model);
		return dotParentModel.zodName;
	}

	private static String generateTsList(final ClassListModel model, final DotClassElementGroup dotGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("zod.array(");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateTsList(fieldListModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateTsMap(fieldMapModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateTsObject(fieldObjectModel, dotGroup);
			out.append(tmp);
		}
		out.append(")");
		return out.toString();
	}

	public void generateFile(final String pathPackage, final DotClassElementGroup dotGroup) throws IOException {
		if (this.nativeType == DefinedPosition.NATIVE) {
			return;
		}
		final ClassModel model = this.models.get(0);
		String data = "";
		if (this.nativeType == DefinedPosition.BASIC && model instanceof ClassObjectModel) {
			data = generateBaseObject();
		} else if (model instanceof final ClassEnumModel modelEnum) {
			data = generateEnum(modelEnum, dotGroup);
		} else if (model instanceof final ClassObjectModel modelObject) {
			data = generateObject(modelObject, dotGroup);
		}
		final Path path = Paths.get(pathPackage + File.separator + "model");
		if (Files.notExists(path)) {
			Files.createDirectories(path);
		}
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "model" + File.separator + this.fileName + ".dot");
		myWriter.write(data);
		myWriter.close();
	}

	private static String generateLocalModelBase(final ClassModel model, final DotClassElementGroup dotGroup)
			throws IOException {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateTsObject(objectModel, dotGroup);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateTsEnum(enumModel, dotGroup);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateTsList(listModel, dotGroup);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateTsMap(mapModel, dotGroup);
		}
		return "";
	}

	public static String generateLocalModel(
			final String ModelName,
			final List<ClassModel> models,
			final DotClassElementGroup dotGroup) throws IOException {
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
			out.append(generateLocalModelBase(models.get(0), dotGroup));
			out.append(";");
		} else {
			out.append("z.union([\n");
			for (final ClassModel model : models) {
				out.append("\t");
				out.append(generateLocalModelBase(models.get(0), dotGroup));
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
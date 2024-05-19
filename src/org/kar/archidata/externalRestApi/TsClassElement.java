package org.kar.archidata.externalRestApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.kar.archidata.externalRestApi.model.ClassEnumModel;
import org.kar.archidata.externalRestApi.model.ClassListModel;
import org.kar.archidata.externalRestApi.model.ClassMapModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;

public class TsClassElement {
	public List<ClassModel> models;
	public String zodName;
	public String tsTypeName;
	public String tsCheckType;
	public String declaration;
	public String fileName = null;
	public String comment = null;
	public boolean isEnum = false;
	public boolean nativeType;

	public TsClassElement(final List<ClassModel> model, final String zodName, final String tsTypeName,
			final String tsCheckType, final String declaration, final boolean nativeType) {
		this.models = model;
		this.zodName = zodName;
		this.tsTypeName = tsTypeName;
		this.tsCheckType = tsCheckType;
		this.declaration = declaration;
		this.nativeType = nativeType;
		this.fileName = tsTypeName.replaceAll("([a-z])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
				.toLowerCase();
	}

	public TsClassElement(final ClassModel model) {
		this.models = List.of(model);
		this.zodName = "Zod" + model.getOriginClasses().getSimpleName();
		this.tsTypeName = model.getOriginClasses().getSimpleName();
		this.tsCheckType = "is" + model.getOriginClasses().getSimpleName();
		this.declaration = null;
		this.nativeType = false;
		this.fileName = this.tsTypeName.replaceAll("([a-z])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
				.toLowerCase();
	}

	public boolean isCompatible(final ClassModel model) {
		return this.models.contains(model);
	}

	public String getBaseHeader() {
		return """
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from \"zod\";

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
			for (final String elem : model.getListOfValues()) {
				if (!first) {
					out.append(",\n\t");
				} else {
					out.append("\n\t");
					first = false;
				}
				out.append("'");
				out.append(elem);
				out.append("'");
			}
			if (first) {
				out.append("]}");
			} else {
				out.append("\n\t])");
			}
			out.append(";\n");

			out.append("\nexport type ");
			out.append(this.tsTypeName);
			out.append(" = zod.infer<typeof ");
			out.append(this.zodName);
			out.append(">;\n");
		} else {
			boolean first = true;
			out.append("export enum ");
			out.append(this.tsTypeName);
			out.append("  {");
			for (final String elem : model.getListOfValues()) {
				if (!first) {
					out.append(",\n\t");
				} else {
					out.append("\n\t");
					first = false;
				}
				out.append(elem);
				out.append(" = '");
				out.append(elem);
				out.append("'");
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
		out.append(generateExportCheckFunction());
		return out.toString();
	}

	private Object generateExportCheckFunction() {
		final StringBuilder out = new StringBuilder();
		out.append("\nexport function ");
		out.append(this.tsCheckType);
		out.append("(data: any): data is ");
		out.append(this.tsTypeName);
		out.append(" {\n\ttry {\n\t\t");
		out.append(this.zodName);
		out.append("""
				.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data ${e}`);
						return false;
					}
				}
				""");
		return out.toString();
	}

	public String generateImports(final List<ClassModel> depModels, final TsClassElementGroup tsGroup)
			throws IOException {
		final StringBuilder out = new StringBuilder();
		for (final ClassModel depModel : depModels) {
			final TsClassElement tsModel = tsGroup.find(depModel);
			if (!tsModel.nativeType) {
				out.append("import {");
				out.append(tsModel.zodName);
				out.append("} from \"");
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

	public String generateObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());
		out.append(generateImports(model.getDependencyModels(), tsGroup));
		out.append("\n");

		out.append(generateComment(model));
		out.append("export const ");
		out.append(this.tsTypeName);
		out.append(" = ");

		if (model.getExtendsClass() != null) {
			final ClassModel parentClass = model.getExtendsClass();
			final TsClassElement tsParentModel = tsGroup.find(parentClass);
			out.append(tsParentModel.zodName);
			out.append(".extend({");
		} else {
			out.append("zod.object({");
		}
		out.append("\n");
		for (final FieldProperty field : model.getFields()) {
			final ClassModel fieldModel = field.model();
			if (field.comment() != null) {
				out.append("\t/*\n");
				out.append("\t * ");
				out.append(field.comment());
				out.append("\n\t*/\n");
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
			out.append(";\n");
		}
		out.append("\n});\n");
		out.append("\nexport type ");
		out.append(this.tsTypeName);
		out.append(" = zod.infer<typeof ");
		out.append(this.zodName);
		out.append(">;\n");
		out.append(generateExportCheckFunction());
		return out.toString();
	}

	private String generateTsMap(final ClassMapModel model, final TsClassElementGroup tsGroup) {
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

	private String generateTsEnum(final ClassEnumModel model, final TsClassElementGroup tsGroup) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		return tsParentModel.zodName;
	}

	private String generateTsObject(final ClassObjectModel model, final TsClassElementGroup tsGroup) {
		final TsClassElement tsParentModel = tsGroup.find(model);
		return tsParentModel.zodName;
	}

	private String generateTsList(final ClassListModel model, final TsClassElementGroup tsGroup) {
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
		if (this.nativeType) {
			return;
		}
		final ClassModel model = this.models.get(0);
		String data = "";
		if (model instanceof final ClassEnumModel modelEnum) {
			data = generateEnum(modelEnum, tsGroup);
		}
		if (model instanceof final ClassObjectModel modelObject) {
			data = generateObject(modelObject, tsGroup);
		}
		final Path path = Paths.get(pathPackage + File.separator + "model");
		if (Files.notExists(path)) {
			Files.createDirectory(path);
		}
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "model" + File.separator + this.fileName + ".ts");
		myWriter.write(data);
		myWriter.close();
	}

}
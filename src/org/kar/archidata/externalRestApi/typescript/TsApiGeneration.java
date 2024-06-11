package org.kar.archidata.externalRestApi.typescript;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.kar.archidata.dataAccess.DataExport;
import org.kar.archidata.externalRestApi.model.ApiGroupModel;
import org.kar.archidata.externalRestApi.model.ApiModel;
import org.kar.archidata.externalRestApi.model.ClassEnumModel;
import org.kar.archidata.externalRestApi.model.ClassListModel;
import org.kar.archidata.externalRestApi.model.ClassMapModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel;
import org.kar.archidata.externalRestApi.model.RestTypeRequest;
import org.kar.archidata.externalRestApi.typescript.TsClassElement.DefinedPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;

public class TsApiGeneration {
	static final Logger LOGGER = LoggerFactory.getLogger(TsApiGeneration.class);

	public static String getBaseHeader() {
		return """
				/**
				 * Interface of the server (auto-generated code)
				 */
				""";
	}

	public static String generateClassEnumModelTypescript(
			final ClassEnumModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importWrite) throws IOException {
		imports.add(model);
		final TsClassElement tsModel = tsGroup.find(model);
		return tsModel.tsTypeName;
	}

	public static String generateClassObjectModelTypescript(
			final ClassObjectModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importWrite) throws IOException {
		final TsClassElement tsModel = tsGroup.find(model);
		if (tsModel.nativeType != DefinedPosition.NATIVE) {
			if (importWrite == null || tsModel.models.get(0).isNoWriteSpecificMode()) {
				imports.add(model);
			} else {
				importWrite.add(model);
			}
		}
		if (tsModel.nativeType != DefinedPosition.NORMAL) {
			return tsModel.tsTypeName;
		}
		if (importWrite != null && !tsModel.models.get(0).isNoWriteSpecificMode()) {
			return tsModel.tsTypeName + "Write";
		}
		return tsModel.tsTypeName;
	}

	public static String generateClassMapModelTypescript(
			final ClassMapModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importWrite) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append("{[key: ");
		out.append(generateClassModelTypescript(model.keyModel, tsGroup, imports, importWrite));
		out.append("]: ");
		out.append(generateClassModelTypescript(model.valueModel, tsGroup, imports, importWrite));
		out.append(";}");
		return out.toString();
	}

	public static String generateClassListModelTypescript(
			final ClassListModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importWrite) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append(generateClassModelTypescript(model.valueModel, tsGroup, imports, importWrite));
		out.append("[]");
		return out.toString();
	}

	public static String generateClassModelTypescript(
			final ClassModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importWrite) throws IOException {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateClassObjectModelTypescript(objectModel, tsGroup, imports, importWrite);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateClassListModelTypescript(listModel, tsGroup, imports, importWrite);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateClassMapModelTypescript(mapModel, tsGroup, imports, importWrite);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateClassEnumModelTypescript(enumModel, tsGroup, imports, importWrite);
		}
		throw new IOException("Impossible model:" + model);
	}

	public static String generateClassModelsTypescript(
			final List<ClassModel> models,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importWrite) throws IOException {
		if (models.size() == 0) {
			return "void";
		}
		final StringBuilder out = new StringBuilder();
		boolean isFirst = true;
		for (final ClassModel model : models) {
			if (isFirst) {
				isFirst = false;
			} else {
				out.append(" | ");
			}
			final String data = generateClassModelTypescript(model, tsGroup, imports, importWrite);
			out.append(data);
		}
		return out.toString();
	}

	public static String capitalizeFirstLetter(final String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static void generateApiFile(
			final ApiGroupModel element,
			final String pathPackage,
			final TsClassElementGroup tsGroup) throws IOException {
		final StringBuilder data = new StringBuilder();

		data.append("export namespace ");
		data.append(element.name);
		data.append(" {\n");
		final Set<ClassModel> imports = new HashSet<>();
		final Set<ClassModel> zodImports = new HashSet<>();
		final Set<ClassModel> isImports = new HashSet<>();
		final Set<ClassModel> writeImports = new HashSet<>();
		final Set<String> toolImports = new HashSet<>();
		for (final ApiModel interfaceElement : element.interfaces) {
			final List<String> consumes = interfaceElement.consumes;
			final List<String> produces = interfaceElement.produces;
			final boolean needGenerateProgress = interfaceElement.needGenerateProgress;
			final String returnModelNameIfComplex = capitalizeFirstLetter(interfaceElement.name) + "TypeReturn";
			final String returnComplexModel = TsClassElement.generateLocalModel(returnModelNameIfComplex,
					interfaceElement.returnTypes, tsGroup);
			if (returnComplexModel != null) {
				data.append("\n\n");
				data.append(returnComplexModel.replaceAll("(?m)^", "\t"));
				for (final ClassModel elem : interfaceElement.returnTypes) {
					zodImports.addAll(elem.getDependencyGroupModels());
				}
			}
			if (interfaceElement.description != null) {
				data.append("\n\t/**\n\t * ");
				data.append(interfaceElement.description);
				data.append("\n\t */");
			}
			data.append("\n\texport function ");
			data.append(interfaceElement.name);
			data.append("({\n\t\t\trestConfig,");
			if (!interfaceElement.queries.isEmpty()) {
				data.append("\n\t\t\tqueries,");
			}
			if (!interfaceElement.parameters.isEmpty()) {
				data.append("\n\t\t\tparams,");
			}
			if (produces != null && produces.size() > 1) {
				data.append("\n\t\t\tproduce,");
			}
			if (interfaceElement.unnamedElement.size() == 1 || interfaceElement.multiPartParameters.size() != 0) {
				data.append("\n\t\t\tdata,");
			}
			if (needGenerateProgress) {
				data.append("\n\t\t\tcallback,");
			}
			data.append("\n\t\t}: {");
			data.append("\n\t\trestConfig: RESTConfig,");
			toolImports.add("RESTConfig");
			if (!interfaceElement.queries.isEmpty()) {
				data.append("\n\t\tqueries: {");
				for (final Entry<String, List<ClassModel>> queryEntry : interfaceElement.queries.entrySet()) {
					data.append("\n\t\t\t");
					data.append(queryEntry.getKey());
					data.append("?: ");
					data.append(generateClassModelsTypescript(queryEntry.getValue(), tsGroup, imports, null));
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (!interfaceElement.parameters.isEmpty()) {
				data.append("\n\t\tparams: {");
				for (final Entry<String, List<ClassModel>> paramEntry : interfaceElement.parameters.entrySet()) {
					data.append("\n\t\t\t");
					data.append(paramEntry.getKey());
					data.append(": ");
					data.append(generateClassModelsTypescript(paramEntry.getValue(), tsGroup, imports, null));
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (interfaceElement.unnamedElement.size() == 1) {
				data.append("\n\t\tdata: ");
				data.append(generateClassModelTypescript(interfaceElement.unnamedElement.get(0), tsGroup, imports,
						writeImports));
				data.append(",");
			} else if (interfaceElement.multiPartParameters.size() != 0) {
				data.append("\n\t\tdata: {");
				for (final Entry<String, List<ClassModel>> pathEntry : interfaceElement.multiPartParameters
						.entrySet()) {
					data.append("\n\t\t\t");
					data.append(pathEntry.getKey());
					data.append(": ");
					data.append(generateClassModelsTypescript(pathEntry.getValue(), tsGroup, imports, writeImports));
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (produces != null && produces.size() > 1) {
				data.append("\n\t\tproduce: ");
				String isFist = null;
				for (final String elem : produces) {
					String lastElement = null;

					if (MediaType.APPLICATION_JSON.equals(elem)) {
						lastElement = "HTTPMimeType.JSON";
						toolImports.add("HTTPMimeType");
					}
					if (MediaType.MULTIPART_FORM_DATA.equals(elem)) {
						lastElement = "HTTPMimeType.MULTIPART";
						toolImports.add("HTTPMimeType");
					}
					if (DataExport.CSV_TYPE.equals(elem)) {
						lastElement = "HTTPMimeType.CSV";
						toolImports.add("HTTPMimeType");
					}
					if (lastElement != null) {
						if (isFist == null) {
							isFist = lastElement;
						} else {
							data.append(" | ");
						}
						data.append(lastElement);
					} else {
						LOGGER.error("Unmanaged model type: {}", elem);
					}
				}
				data.append(",");
			}
			if (needGenerateProgress) {
				data.append("\n\t\tcallbacks?: RESTCallbacks,");
				toolImports.add("RESTCallbacks");
			}
			data.append("\n\t}): Promise<");
			if (returnComplexModel != null) {
				data.append(returnModelNameIfComplex);
				data.append("> {");
				data.append("\n\t\treturn RESTRequestJson({");
				toolImports.add("RESTRequestJson");
			} else {
				final String returnType = generateClassModelsTypescript(interfaceElement.returnTypes, tsGroup, imports,
						null);
				data.append(returnType);
				data.append("> {");
				if ("void".equals(returnType)) {
					data.append("\n\t\treturn RESTRequestVoid({");
					toolImports.add("RESTRequestVoid");
				} else {
					isImports.addAll(interfaceElement.returnTypes);
					data.append("\n\t\treturn RESTRequestJson({");
					toolImports.add("RESTRequestJson");
				}
			}
			data.append("\n\t\t\trestModel: {");
			data.append("\n\t\t\t\tendPoint: \"");
			data.append(interfaceElement.restEndPoint);
			data.append("\",");
			data.append("\n\t\t\t\trequestType: HTTPRequestModel.");
			toolImports.add("HTTPRequestModel");
			data.append(interfaceElement.restTypeRequest);
			data.append(",");
			if (consumes != null) {
				for (final String elem : consumes) {
					if (MediaType.APPLICATION_JSON.equals(elem)) {
						data.append("\n\t\t\t\tcontentType: HTTPMimeType.JSON,");
						toolImports.add("HTTPMimeType");
						break;
					} else if (MediaType.MULTIPART_FORM_DATA.equals(elem)) {
						data.append("\n\t\t\t\tcontentType: HTTPMimeType.MULTIPART,");
						toolImports.add("HTTPMimeType");
						break;
					} else if (MediaType.TEXT_PLAIN.equals(elem)) {
						data.append("\n\t\t\t\tcontentType: HTTPMimeType.TEXT_PLAIN,");
						toolImports.add("HTTPMimeType");
						break;
					}
				}
			} else if (RestTypeRequest.DELETE.equals(interfaceElement.restTypeRequest)) {
				data.append("\n\t\t\t\tcontentType: HTTPMimeType.TEXT_PLAIN,");
				toolImports.add("HTTPMimeType");
			}
			if (produces != null) {
				if (produces.size() > 1) {
					data.append("\n\t\t\t\taccept: produce,");
				} else {
					final String returnType = generateClassModelsTypescript(interfaceElement.returnTypes, tsGroup,
							imports, null);
					if (!"void".equals(returnType)) {
						for (final String elem : produces) {
							if (MediaType.APPLICATION_JSON.equals(elem)) {
								data.append("\n\t\t\t\taccept: HTTPMimeType.JSON,");
								toolImports.add("HTTPMimeType");
								break;
							}
						}
					}
				}
			}
			data.append("\n\t\t\t},");
			data.append("\n\t\t\trestConfig,");
			if (!interfaceElement.parameters.isEmpty()) {
				data.append("\n\t\t\tparams,");
			}
			if (!interfaceElement.queries.isEmpty()) {
				data.append("\n\t\t\tqueries,");
			}
			if (interfaceElement.unnamedElement.size() == 1) {
				data.append("\n\t\t\tdata,");
			} else if (interfaceElement.multiPartParameters.size() != 0) {
				data.append("\n\t\t\tdata,");
			}
			if (needGenerateProgress) {
				data.append("\n\t\t\tcallback,");
			}
			data.append("\n\t\t}");
			if (returnComplexModel != null) {
				data.append(", is");
				data.append(returnModelNameIfComplex);
			} else {
				final TsClassElement retType = tsGroup.find(interfaceElement.returnTypes.get(0));
				if (retType.tsCheckType != null) {
					data.append(", ");
					data.append(retType.tsCheckType);
					imports.add(interfaceElement.returnTypes.get(0));
				}
			}
			data.append(");");
			data.append("\n\t};");
		}
		data.append("\n}\n");

		final StringBuilder out = new StringBuilder();
		out.append(getBaseHeader());

		final List<String> toolImportsList = new ArrayList<>(toolImports);
		Collections.sort(toolImportsList);
		if (toolImportsList.size() != 0) {
			out.append("import {");
			for (final String elem : toolImportsList) {
				out.append("\n\t");
				out.append(elem);
				out.append(",");
			}
			out.append("\n} from \"../rest-tools\";\n\n");
		}

		if (zodImports.size() != 0) {
			out.append("import { z as zod } from \"zod\"\n");
		}

		final Set<String> finalImportSet = new TreeSet<>();

		for (final ClassModel model : imports) {
			final TsClassElement tsModel = tsGroup.find(model);
			if (tsModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			finalImportSet.add(tsModel.tsTypeName);
		}
		for (final ClassModel model : isImports) {
			final TsClassElement tsModel = tsGroup.find(model);
			if (tsModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			if (tsModel.tsCheckType != null) {
				finalImportSet.add(tsModel.tsCheckType);
			}
		}
		for (final ClassModel model : zodImports) {
			final TsClassElement tsModel = tsGroup.find(model);
			if (tsModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			finalImportSet.add("Zod" + tsModel.tsTypeName);
		}
		for (final ClassModel model : writeImports) {
			final TsClassElement tsModel = tsGroup.find(model);
			if (tsModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			finalImportSet.add(tsModel.tsTypeName + "Write");
		}

		if (finalImportSet.size() != 0) {
			out.append("import {");
			for (final String elem : finalImportSet) {
				out.append("\n\t");
				out.append(elem);
				out.append(",");
			}
			out.append("\n} from \"../model\";\n\n");
		}

		out.append(data.toString());

		final String fileName = TsClassElement.determineFileName(element.name);
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "api" + File.separator + fileName + ".ts");
		myWriter.write(out.toString());
		myWriter.close();
	}

}
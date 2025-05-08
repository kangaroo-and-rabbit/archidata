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
import java.util.TreeSet;

import org.atriasoft.archidata.dataAccess.DataExport;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel.OptionalClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.RestTypeRequest;
import org.atriasoft.archidata.externalRestApi.typescript.TsClassElement.DefinedPosition;
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
			final Set<ClassModel> importUpdate,
			final Set<ClassModel> importCreate,
			final boolean partialObject) {
		imports.add(model);
		final TsClassElement tsModel = tsGroup.find(model);
		return tsModel.tsTypeName;
	}

	public static String generateClassObjectModelTypescript(
			final ClassObjectModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importUpdate,
			final Set<ClassModel> importCreate,
			final boolean partialObject) {
		final TsClassElement tsModel = tsGroup.find(model);
		if (tsModel.nativeType != DefinedPosition.NATIVE) {
			if (importCreate != null && tsModel.models.get(0).getApiGenerationMode().create()) {
				importCreate.add(model);
			} else if (importUpdate != null && tsModel.models.get(0).getApiGenerationMode().update()) {
				importUpdate.add(model);
			} else {
				imports.add(model);
			}
		}
		String out = tsModel.tsTypeName;
		if (tsModel.nativeType != DefinedPosition.NORMAL) {
			out = tsModel.tsTypeName;
		} else if (importCreate != null && tsModel.models.get(0).getApiGenerationMode().create()) {
			out = tsModel.tsTypeName + TsClassElement.MODEL_TYPE_CREATE;
		} else if (importUpdate != null && tsModel.models.get(0).getApiGenerationMode().update()) {
			out = tsModel.tsTypeName + TsClassElement.MODEL_TYPE_UPDATE;
		}
		if (partialObject) {
			return "Partial<" + out + ">";
		}
		return out;
	}

	public static String generateClassMapModelTypescript(
			final ClassMapModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importUpdate,
			final Set<ClassModel> importCreate,
			final boolean partialObject) {
		final StringBuilder out = new StringBuilder();
		out.append("{[key: ");
		out.append(generateClassModelTypescript(model.keyModel, tsGroup, imports, importUpdate, importCreate,
				partialObject));
		out.append("]: ");
		out.append(generateClassModelTypescript(model.valueModel, tsGroup, imports, importUpdate, importCreate,
				partialObject));
		out.append(";}");
		return out.toString();
	}

	public static String generateClassListModelTypescript(
			final ClassListModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importUpdate,
			final Set<ClassModel> importCreate,
			final boolean partialObject) {
		final StringBuilder out = new StringBuilder();
		out.append(generateClassModelTypescript(model.valueModel, tsGroup, imports, importUpdate, importCreate,
				partialObject));
		out.append("[]");
		return out.toString();
	}

	public static String generateClassModelTypescript(
			final ClassModel model,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importUpdate,
			final Set<ClassModel> importCreate,
			final boolean partialObject) {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateClassObjectModelTypescript(objectModel, tsGroup, imports, importUpdate, importCreate,
					partialObject);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateClassListModelTypescript(listModel, tsGroup, imports, importUpdate, importCreate,
					partialObject);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateClassMapModelTypescript(mapModel, tsGroup, imports, importUpdate, importCreate,
					partialObject);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateClassEnumModelTypescript(enumModel, tsGroup, imports, importUpdate, importCreate,
					partialObject);
		}
		throw new RuntimeException("Impossible model:" + model);
	}

	public static String generateClassModelsTypescript(
			final List<ClassModel> models,
			final TsClassElementGroup tsGroup,
			final Set<ClassModel> imports,
			final Set<ClassModel> importUpdate,
			final Set<ClassModel> importCreate,
			final boolean partialObject) {
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
			final String data = generateClassModelTypescript(model, tsGroup, imports, importUpdate, importCreate,
					partialObject);
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
			final TsClassElementGroup tsGroup,
			final Map<Path, String> generation) {
		final StringBuilder data = new StringBuilder();

		data.append("export namespace ");
		data.append(element.name);
		data.append(" {\n");
		final Set<ClassModel> imports = new HashSet<>();
		final Set<ClassModel> zodImports = new HashSet<>();
		final Set<ClassModel> isImports = new HashSet<>();
		final Set<ClassModel> updateImports = new HashSet<>();
		final Set<ClassModel> createImports = new HashSet<>();
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
					// TODO maybe need to update this with the type of zod requested (like update, create ...
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
			if (!interfaceElement.headers.isEmpty()) {
				data.append("\n\t\t\theaders,");
			}
			if (needGenerateProgress) {
				data.append("\n\t\t\tcallbacks,");
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
					data.append(
							generateClassModelsTypescript(queryEntry.getValue(), tsGroup, imports, null, null, false));
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
					data.append(
							generateClassModelsTypescript(paramEntry.getValue(), tsGroup, imports, null, null, false));
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (interfaceElement.unnamedElement.size() == 1) {
				data.append("\n\t\tdata: ");
				if (interfaceElement.restTypeRequest == RestTypeRequest.POST) {
					data.append(generateClassModelTypescript(interfaceElement.unnamedElement.get(0), tsGroup, imports,
							null, createImports, false));
				} else if (interfaceElement.restTypeRequest == RestTypeRequest.PUT) {
					data.append(generateClassModelTypescript(interfaceElement.unnamedElement.get(0), tsGroup, imports,
							updateImports, null, false));
				} else if (interfaceElement.restTypeRequest == RestTypeRequest.PATCH) {
					data.append(generateClassModelTypescript(interfaceElement.unnamedElement.get(0), tsGroup, imports,
							updateImports, null, true));
				} else {
					data.append(generateClassModelTypescript(interfaceElement.unnamedElement.get(0), tsGroup, imports,
							null, null, true));
				}
				data.append(",");
			} else if (interfaceElement.multiPartParameters.size() != 0) {
				data.append("\n\t\tdata: {");
				for (final Entry<String, OptionalClassModel> pathEntry : interfaceElement.multiPartParameters
						.entrySet()) {
					data.append("\n\t\t\t");
					data.append(pathEntry.getKey());
					if (pathEntry.getValue().optional()) {
						data.append("?");
					}
					data.append(": ");
					if (interfaceElement.restTypeRequest == RestTypeRequest.POST) {
						data.append(generateClassModelsTypescript(pathEntry.getValue().model(), tsGroup, imports, null,
								createImports, false));
					} else if (interfaceElement.restTypeRequest == RestTypeRequest.PUT) {
						data.append(generateClassModelsTypescript(pathEntry.getValue().model(), tsGroup, imports,
								updateImports, null, false));
					} else if (interfaceElement.restTypeRequest == RestTypeRequest.PATCH) {
						data.append(generateClassModelsTypescript(pathEntry.getValue().model(), tsGroup, imports,
								updateImports, null, true));
					} else {
						data.append(generateClassModelsTypescript(pathEntry.getValue().model(), tsGroup, imports, null,
								null, true));
					}
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (!interfaceElement.headers.isEmpty()) {
				data.append("\n\t\theaders?: {");
				for (final Entry<String, OptionalClassModel> headerEntry : interfaceElement.headers.entrySet()) {
					data.append("\n\t\t\t");
					data.append(headerEntry.getKey());
					if (headerEntry.getValue().optional()) {
						data.append("?");
					}
					data.append(": ");
					data.append(generateClassModelsTypescript(headerEntry.getValue().model(), tsGroup, imports, null,
							null, false));
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
						null, null, false);
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
							imports, null, null, false);
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
				data.append("\n\t\t\tcallbacks,");
			}
			if (!interfaceElement.headers.isEmpty()) {
				data.append("\n\t\t\theaders,");
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
			if (!tsModel.models.get(0).getApiGenerationMode().read()) {
				continue;
			}
			finalImportSet.add("Zod" + tsModel.tsTypeName);
		}
		for (final ClassModel model : updateImports) {
			final TsClassElement tsModel = tsGroup.find(model);
			if (tsModel.nativeType != DefinedPosition.NORMAL) {
				continue;
			}
			if (!tsModel.models.get(0).getApiGenerationMode().update()) {
				continue;
			}
			finalImportSet.add(tsModel.tsTypeName + TsClassElement.MODEL_TYPE_UPDATE);
		}
		for (final ClassModel model : createImports) {
			final TsClassElement tsModel = tsGroup.find(model);
			if (tsModel.nativeType != DefinedPosition.NORMAL) {
				continue;
			}
			if (!tsModel.models.get(0).getApiGenerationMode().create()) {
				continue;
			}
			finalImportSet.add(tsModel.tsTypeName + TsClassElement.MODEL_TYPE_CREATE);
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
		generation.put(Paths.get("api").resolve(fileName), out.toString());
	}

}
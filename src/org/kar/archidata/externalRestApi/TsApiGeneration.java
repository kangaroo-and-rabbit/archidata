package org.kar.archidata.externalRestApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.kar.archidata.dataAccess.DataExport;
import org.kar.archidata.externalRestApi.model.ApiGroupModel;
import org.kar.archidata.externalRestApi.model.ApiModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.RestTypeRequest;
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
				import {
				  HTTPMimeType,
				  HTTPRequestModel,
				  ModelResponseHttp,
				  RESTCallbacks,
				  RESTConfig,
				  RESTRequestJson,
				  RESTRequestJsonArray,
				  RESTRequestVoid
				} from "../rest-tools"

				""";
	}

	public static void generateApiFile(
			final ApiGroupModel element,
			final String pathPackage,
			final TsClassElementGroup tsGroup) throws IOException {
		final StringBuilder data = new StringBuilder();
		data.append(getBaseHeader());

		data.append("export namespace ");
		data.append(element.name);
		data.append(" {\n");

		for (final ApiModel interfaceElement : element.interfaces) {
			final String methodName = interfaceElement.name;
			final String methodPath = interfaceElement.restEndPoint;
			final RestTypeRequest methodType = interfaceElement.restTypeRequest;
			final List<String> consumes = interfaceElement.consumes;
			final List<String> produces = interfaceElement.produces;
			final boolean needGenerateProgress = interfaceElement.needGenerateProgress;
			final List<ClassModel> returnTypeModel = interfaceElement.returnTypes;

			if (interfaceElement.description != null) {
				data.append("\n\t/**\n\t * ");
				data.append(interfaceElement.description);
				data.append("\n\t */");
			}
			data.append("\n\texport function ");
			data.append(methodName);
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
			if (!interfaceElement.queries.isEmpty()) {
				data.append("\n\t\tqueries: {");
				for (final Entry<String, List<ClassModel>> queryEntry : interfaceElement.queries.entrySet()) {
					data.append("\n\t\t\t");
					data.append(queryEntry.getKey());
					data.append("?: ");
					data.append(queryEntry.getValue());
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (!interfaceElement.parameters.isEmpty()) {
				data.append("\n\t\tparams: {");
				for (final Entry<String, List<ClassModel>> pathEntry : interfaceElement.parameters.entrySet()) {
					data.append("\n\t\t\t");
					data.append(pathEntry.getKey());
					data.append(": ");
					data.append(pathEntry.getValue());
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			if (interfaceElement.unnamedElement.size() == 1) {
				data.append("\n\t\tdata: ");
				data.append(interfaceElement.unnamedElement.get(0));
				data.append(",");
			} else if (interfaceElement.multiPartParameters.size() != 0) {
				data.append("\n\t\tdata: {");
				for (final Entry<String, List<ClassModel>> pathEntry : interfaceElement.multiPartParameters
						.entrySet()) {
					data.append("\n\t\t\t");
					data.append(pathEntry.getKey());
					data.append(": ");
					data.append(pathEntry.getValue());
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
					}
					if (MediaType.MULTIPART_FORM_DATA.equals(elem)) {
						lastElement = "HTTPMimeType.MULTIPART";
					}
					if (DataExport.CSV_TYPE.equals(elem)) {
						lastElement = "HTTPMimeType.CSV";
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
				data.append("\n\t\tcallback?: RESTCallbacks,");
			}
			data.append("\n\t}): Promise<");
			/**
			if (interfaceElement.returnTypes.size() == 0 //
					|| tmpReturn.get(0).tsTypeName == null //
					|| tmpReturn.get(0).tsTypeName.equals("void")) {
				data.append("void");
			} else {
				data.append(ApiTool.convertInTypeScriptType(tmpReturn, returnModelIsArray));
			}
			*/
			data.append("> {");
			/**
			if (tmpReturn.size() == 0 //
					|| tmpReturn.get(0).tsTypeName == null //
					|| tmpReturn.get(0).tsTypeName.equals("void")) {
				data.append("\n\t\treturn RESTRequestVoid({");
			} else if (returnModelIsArray) {
				data.append("\n\t\treturn RESTRequestJsonArray({");
			} else {
				data.append("\n\t\treturn RESTRequestJson({");
			}
			*/
			data.append("\n\t\t\trestModel: {");
			data.append("\n\t\t\t\tendPoint: \"");
			data.append(interfaceElement.restEndPoint);
			data.append("\",");
			data.append("\n\t\t\t\trequestType: HTTPRequestModel.");
			data.append(methodType);
			data.append(",");
			if (consumes != null) {
				for (final String elem : consumes) {
					if (MediaType.APPLICATION_JSON.equals(elem)) {
						data.append("\n\t\t\t\tcontentType: HTTPMimeType.JSON,");
						break;
					} else if (MediaType.MULTIPART_FORM_DATA.equals(elem)) {
						data.append("\n\t\t\t\tcontentType: HTTPMimeType.MULTIPART,");
						break;
					} else if (MediaType.TEXT_PLAIN.equals(elem)) {
						data.append("\n\t\t\t\tcontentType: HTTPMimeType.TEXT_PLAIN,");
						break;
					}
				}
			} else if ("DELETE".equals(methodType)) {
				data.append("\n\t\t\t\tcontentType: HTTPMimeType.TEXT_PLAIN,");
			}
			if (produces != null) {
				if (produces.size() > 1) {
					data.append("\n\t\t\t\taccept: produce,");
				} else {
					for (final String elem : produces) {
						if (MediaType.APPLICATION_JSON.equals(elem)) {
							data.append("\n\t\t\t\taccept: HTTPMimeType.JSON,");
							break;
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
			/**
			if (tmpReturn.size() != 0 && tmpReturn.get(0).tsTypeName != null
					&& !tmpReturn.get(0).tsTypeName.equals("void")) {
				data.append(", ");
				// TODO: correct this it is really bad ...
				data.append(ApiTool.convertInTypeScriptCheckType(tmpReturn));
			}
			**/
			data.append(");");
			data.append("\n\t};");
		}
		data.append("\n}\n");
		final String fileName = TsClassElement.determineFileName(element.name);
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "api" + File.separator + fileName + ".ts");
		myWriter.write(data.toString());
		myWriter.close();
	}

}
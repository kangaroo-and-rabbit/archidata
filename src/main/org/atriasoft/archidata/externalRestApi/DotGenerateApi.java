package org.atriasoft.archidata.externalRestApi;

import java.io.FileWriter;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.externalRestApi.dot.DotApiGeneration;
import org.atriasoft.archidata.externalRestApi.dot.DotClassElement;
import org.atriasoft.archidata.externalRestApi.dot.DotClassElement.DefinedPosition;
import org.atriasoft.archidata.externalRestApi.dot.DotClassElementGroup;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public class DotGenerateApi {
	private DotGenerateApi() {
		// Utility class
	}

	public static void generateApi(final AnalyzeApi api, final String pathDotFile) throws Exception {
		final List<DotClassElement> localModel = generateApiModel(api);
		final DotClassElementGroup dotGroup = new DotClassElementGroup(localModel);

		try (final FileWriter myWriter = new FileWriter(pathDotFile)) {
			myWriter.write("""
					# Architecture auto-generated file
					digraph UML_Class_diagram {
					    #rankdir=NS;
						graph [
							pad="0.5"
							nodesep="1"
							#ranksep="2"
							label="Rest API server Model"
							labelloc="t"
							fontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"
						]
						node [
							fontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"
							shape=record
							style=filled
							fillcolor=gray95
						]
						edge [fontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"]
					""");
			for (final ApiGroupModel element : api.apiModels) {
				final String tmp = DotApiGeneration.generateApiFile(element, dotGroup);
				myWriter.write(tmp);
				myWriter.write("\n");
			}
			// create an invisible link to force all element to be link together:
			// Generates all MODEL files
			for (final DotClassElement element : localModel) {
				final String tmp = element.generateFile(dotGroup);
				myWriter.write(tmp);
				myWriter.write("\n");
			}
			myWriter.write("""
					}
					""");
		}
	}

	private static List<DotClassElement> generateApiModel(final AnalyzeApi api) throws Exception {
		// First step is to add all specific basic elements the wrap correctly the model
		final List<DotClassElement> dotModels = new ArrayList<>();
		List<ClassModel> models = api.getCompatibleModels(List.of(Void.class, void.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "void", "void", null, null, DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(Object.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Object", "object", null, "Object", DefinedPosition.NATIVE));
		}
		// Map is binded to any ==> can not determine this complex model for now
		models = api.getCompatibleModels(List.of(Map.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Object", "any", null, null, DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(String.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "String", "string", null, "String", DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(
				List.of(InputStream.class, FormDataContentDisposition.class, ContentDisposition.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "File", "File", null, "File", DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(Boolean.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Boolean", "boolean", null, "Boolean", DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(boolean.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "boolean", "boolean", null, "boolean", DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(UUID.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "UUID", "UUID", "isUUID", "UUID", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(long.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "long", "Long", "isLong", "long", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Long.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Long", "Long", "isLong", "Long", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(short.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "short", "Short", "isShort", "short", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Short.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Short", "Short", "isShort", "Short", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(int.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "int", "Integer", "isInteger", "int", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Integer.class));
		if (models != null) {
			dotModels.add(
					new DotClassElement(models, "Integer", "Integer", "isInteger", "Integer", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(double.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Double", "Double", "isDouble", "double", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Double.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Double", "Double", "isDouble", "Double", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(float.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "float", "Float", "isFloat", "float", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Float.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Float", "Float", "isFloat", "Float", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Instant.class));
		if (models != null) {
			dotModels.add(
					new DotClassElement(models, "Instant", "Instant", "isInstant", "Instant", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Date.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "Date", "IsoDate", "isIsoDate", "Date", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(LocalDate.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "LocalDate", "LocalDate", "isLocalDate", "LocalDate",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(LocalTime.class));
		if (models != null) {
			dotModels.add(new DotClassElement(models, "LocalTime", "LocalTime", "isLocalTime", "LocalTime",
					DefinedPosition.BASIC));
		}
		// needed for Rest interface
		api.addModel(RestErrorResponse.class);
		for (final ClassModel model : api.getAllModel()) {
			boolean alreadyExist = false;
			for (final DotClassElement elem : dotModels) {
				if (elem.isCompatible(model)) {
					alreadyExist = true;
					break;
				}
			}
			if (alreadyExist) {
				continue;
			}
			dotModels.add(new DotClassElement(model));
		}
		return dotModels;

	}

}

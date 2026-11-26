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
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * Generates Graphviz DOT diagrams from {@link AnalyzeApi} introspection data.
 *
 * <p>Produces UML-style class diagrams in DOT format that can be rendered
 * with Graphviz tools (e.g., {@code dot}, {@code neato}).
 */
public class DotGenerateApi {
	/** Private constructor to prevent instantiation of this utility class. */
	private DotGenerateApi() {
		// Utility class
	}

	/**
	 * Generates a Graphviz DOT file representing the API models and endpoints.
	 * @param api the analyzed API model to generate from
	 * @param pathDotFile the file path where the DOT file will be written
	 * @throws Exception if generation or file writing fails
	 */
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
		GeneratedModels.addNative(api, dotModels, List.of(Void.class, void.class),
				models -> new DotClassElement(models, "void", "void", null, null, DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, dotModels, List.of(Object.class),
				models -> new DotClassElement(models, "Object", "object", null, "Object", DefinedPosition.NATIVE));
		// Map is binded to any ==> can not determine this complex model for now
		GeneratedModels.addNative(api, dotModels, List.of(Map.class),
				models -> new DotClassElement(models, "Object", "any", null, null, DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, dotModels, List.of(String.class),
				models -> new DotClassElement(models, "String", "string", null, "String", DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, dotModels,
				List.of(InputStream.class, FormDataContentDisposition.class, ContentDisposition.class),
				models -> new DotClassElement(models, "File", "File", null, "File", DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, dotModels, List.of(Boolean.class),
				models -> new DotClassElement(models, "Boolean", "boolean", null, "Boolean", DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, dotModels, List.of(boolean.class),
				models -> new DotClassElement(models, "boolean", "boolean", null, "boolean", DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, dotModels, List.of(UUID.class),
				models -> new DotClassElement(models, "UUID", "UUID", "isUUID", "UUID", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(long.class),
				models -> new DotClassElement(models, "long", "Long", "isLong", "long", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Long.class),
				models -> new DotClassElement(models, "Long", "Long", "isLong", "Long", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(short.class),
				models -> new DotClassElement(models, "short", "Short", "isShort", "short", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Short.class),
				models -> new DotClassElement(models, "Short", "Short", "isShort", "Short", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(int.class),
				models -> new DotClassElement(models, "int", "Integer", "isInteger", "int", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Integer.class), models -> new DotClassElement(models,
				"Integer", "Integer", "isInteger", "Integer", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(double.class),
				models -> new DotClassElement(models, "Double", "Double", "isDouble", "double", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Double.class),
				models -> new DotClassElement(models, "Double", "Double", "isDouble", "Double", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(float.class),
				models -> new DotClassElement(models, "float", "Float", "isFloat", "float", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Float.class),
				models -> new DotClassElement(models, "Float", "Float", "isFloat", "Float", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Instant.class), models -> new DotClassElement(models,
				"Instant", "Instant", "isInstant", "Instant", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(Date.class),
				models -> new DotClassElement(models, "Date", "IsoDate", "isIsoDate", "Date", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(LocalDate.class), models -> new DotClassElement(models,
				"LocalDate", "LocalDate", "isLocalDate", "LocalDate", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, dotModels, List.of(LocalTime.class), models -> new DotClassElement(models,
				"LocalTime", "LocalTime", "isLocalTime", "LocalTime", DefinedPosition.BASIC));
		// needed for Rest interface
		api.addModel(RestErrorResponse.class);
		GeneratedModels.addRemaining(api, dotModels, DotClassElement::isCompatible, DotClassElement::new);
		return dotModels;

	}

}

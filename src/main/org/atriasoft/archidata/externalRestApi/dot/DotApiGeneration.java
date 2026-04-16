package org.atriasoft.archidata.externalRestApi.dot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.externalRestApi.dot.DotClassElement.DefinedPosition;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Graphviz DOT API diagram nodes representing REST endpoint groups.
 */
public class DotApiGeneration {
	/** Private constructor to prevent instantiation of this utility class. */
	private DotApiGeneration() {
		// Utility class
	}

	/** Logger for this class. */
	static final Logger LOGGER = LoggerFactory.getLogger(DotApiGeneration.class);

	/**
	 * Generates the DOT type name for an enum model and registers its import.
	 * @param model the enum model
	 * @param dotGroup the group registry for resolving type references
	 * @param imports the set of imported class models to populate
	 * @return the DOT type name string
	 * @throws IOException if generation fails
	 */
	public static String generateClassEnumModelTypescript(
			final ClassEnumModel model,
			final DotClassElementGroup dotGroup,
			final Set<ClassModel> imports) throws IOException {
		imports.add(model);
		final DotClassElement dotModel = dotGroup.find(model);
		return dotModel.dotTypeName;
	}

	/**
	 * Generates the DOT type name for an object model and registers its import.
	 * @param model the object model
	 * @param dotGroup the group registry for resolving type references
	 * @param imports the set of imported class models to populate
	 * @return the DOT type name string
	 * @throws IOException if generation fails
	 */
	public static String generateClassObjectModelTypescript(
			final ClassObjectModel model,
			final DotClassElementGroup dotGroup,
			final Set<ClassModel> imports) throws IOException {
		final DotClassElement dotModel = dotGroup.find(model);
		if (dotModel.nativeType != DefinedPosition.NATIVE) {
			imports.add(model);
		}
		if (dotModel.nativeType != DefinedPosition.NORMAL) {
			return dotModel.dotTypeName;
		}
		return dotModel.dotTypeName;
	}

	/**
	 * Generates the DOT type representation for a map model.
	 * @param model the map model
	 * @param dotGroup the group registry for resolving type references
	 * @param imports the set of imported class models to populate
	 * @return the DOT map type string with HTML-escaped angle brackets
	 * @throws IOException if generation fails
	 */
	public static String generateClassMapModelTypescript(
			final ClassMapModel model,
			final DotClassElementGroup dotGroup,
			final Set<ClassModel> imports) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append("Map&lt;");
		out.append(generateClassModelTypescript(model.keyModel, dotGroup, imports));
		out.append(", ");
		out.append(generateClassModelTypescript(model.valueModel, dotGroup, imports));
		out.append("&gt;");
		return out.toString();
	}

	/**
	 * Generates the DOT type representation for a list model.
	 * @param model the list model
	 * @param dotGroup the group registry for resolving type references
	 * @param imports the set of imported class models to populate
	 * @return the DOT list type string with HTML-escaped angle brackets
	 * @throws IOException if generation fails
	 */
	public static String generateClassListModelTypescript(
			final ClassListModel model,
			final DotClassElementGroup dotGroup,
			final Set<ClassModel> imports) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append("List&lt;");
		out.append(generateClassModelTypescript(model.valueModel, dotGroup, imports));
		out.append("&gt;");
		return out.toString();
	}

	/**
	 * Generates the DOT type representation for any class model by dispatching to the appropriate handler.
	 * @param model the class model to generate a type for
	 * @param dotGroup the group registry for resolving type references
	 * @param imports the set of imported class models to populate
	 * @return the DOT type string
	 * @throws IOException if the model type is not supported
	 */
	public static String generateClassModelTypescript(
			final ClassModel model,
			final DotClassElementGroup dotGroup,
			final Set<ClassModel> imports) throws IOException {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateClassObjectModelTypescript(objectModel, dotGroup, imports);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateClassListModelTypescript(listModel, dotGroup, imports);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateClassMapModelTypescript(mapModel, dotGroup, imports);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateClassEnumModelTypescript(enumModel, dotGroup, imports);
		}
		throw new IOException("Impossible model:" + model);
	}

	/**
	 * Generates the DOT type representation for a list of models, possibly as a union.
	 * @param models the list of class models
	 * @param dotGroup the group registry for resolving type references
	 * @return the DOT type string, or "void" if empty
	 * @throws IOException if generation fails
	 */
	public static String generateClassModelsTypescript(
			final List<ClassModel> models,
			final DotClassElementGroup dotGroup) throws IOException {
		if (models.size() == 0) {
			return "void";
		}
		final StringBuilder out = new StringBuilder();
		if (models.size() > 1) {
			out.append("Union&lt;");
		}
		boolean isFirst = true;
		for (final ClassModel model : models) {
			if (isFirst) {
				isFirst = false;
			} else {
				out.append(", ");
			}
			final String data = DotClassElement.generateClassModelTypescript(model, dotGroup);
			out.append(data);
		}
		if (models.size() > 1) {
			out.append("&gt;");
		}
		return out.toString();
	}

	/**
	 * Generates DOT link target names for a list of class models.
	 * @param models the list of class models
	 * @param dotGroup the group registry for resolving type references
	 * @return the list of link target names (excluding null entries)
	 * @throws IOException if generation fails
	 */
	public static List<String> generateClassModelsLinks(
			final List<ClassModel> models,
			final DotClassElementGroup dotGroup) throws IOException {
		if (models.isEmpty()) {
			return new ArrayList<>();
		}
		final List<String> out = new ArrayList<>();
		for (final ClassModel model : models) {
			final String data = DotClassElement.generateClassModelTypescriptLink(model, dotGroup);
			if (data != null) {
				out.add(data);
			}
		}
		return out;
	}

	/**
	 * Capitalizes the first letter of a string.
	 * @param str the string to capitalize
	 * @return the string with its first letter capitalized, or the original if null/empty
	 */
	public static String capitalizeFirstLetter(final String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * Generates a DOT API diagram node for a resource group with its endpoint methods.
	 * @param element the API group model containing endpoint definitions
	 * @param dotGroup the group registry for resolving type references
	 * @return the generated DOT node and edge definitions
	 * @throws IOException if generation fails
	 */
	public static String generateApiFile(final ApiGroupModel element, final DotClassElementGroup dotGroup)
			throws IOException {
		final StringBuilder data = new StringBuilder();
		final StringBuilder outLinks = new StringBuilder();
		data.append("""
					%s [
						shape=plain
						label=<<table color="#FF3333" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						<tr>
							<td><b>%s</b><br/>(REST)</td>
						</tr>
						<tr>
							<td>
								<table border="0" cellborder="0" cellspacing="0" >
				""".formatted(element.name, element.name));
		//final Set<ClassModel> imports = new HashSet<>();
		//final Set<ClassModel> zodImports = new HashSet<>();
		//final Set<ClassModel> isImports = new HashSet<>();
		final Set<ClassModel> writeImports = new HashSet<>();
		//final Set<String> toolImports = new HashSet<>();
		for (final ApiModel interfaceElement : element.interfaces) {
			//final List<String> consumes = interfaceElement.consumes;
			//final List<String> produces = interfaceElement.produces;
			//final boolean needGenerateProgress = interfaceElement.needGenerateProgress;
			/*
			if (returnComplexModel != null) {
				data.append(returnComplexModel.replaceAll("(?m)^", "\t"));
				for (final ClassModel elem : interfaceElement.returnTypes) {
					zodImports.addAll(elem.getDependencyGroupModels());
				}
			}
			*/
			data.append("\t\t\t\t\t<tr><td  align=\"left\" port=\"");
			data.append(interfaceElement.name);
			data.append("\"><b> + ");
			data.append(interfaceElement.name);
			data.append("(");
			boolean hasParam = false;
			/*
			if (!interfaceElement.queries.isEmpty()) {
				data.append("\n\t\tqueries: {");
				for (final Entry<String, List<ClassModel>> queryEntry : interfaceElement.queries.entrySet()) {
					data.append("\n\t\t\t");
					data.append(queryEntry.getKey());
					data.append("?: ");
					data.append(generateClassModelsTypescript(queryEntry.getValue(), dotGroup, imports, false));
					data.append(",");
				}
				data.append("\n\t\t},");
			}
			*/
			/* fonctionnel mais trop de donnée
			if (!interfaceElement.parameters.isEmpty()) {
				//data.append("params: {");
				for (final Entry<String, List<ClassModel>> paramEntry : interfaceElement.parameters.entrySet()) {
					data.append("");
					data.append(paramEntry.getKey());
					data.append(": ");
					data.append(generateClassModelsTypescript(paramEntry.getValue(), dotGroup, imports, false));
					data.append(",");
				}
				//data.append("},");
			}
			*/
			if (interfaceElement.unnamedElement.size() == 1) {
				if (hasParam) {
					data.append(", ");
				}
				hasParam = true;
				data.append("data: ");
				// TODO: set it back: data.append(	generateClassModelTypescript(interfaceElement.unnamedElement.get(0), dotGroup, writeImports));
			} else if (interfaceElement.multiPartParameters.size() != 0) {
				if (hasParam) {
					data.append(", ");
				}
				hasParam = true;
				final boolean hasParam2 = false;
				data.append("data: {");
				// TODO: set it back:
				/*
				for (final Entry<String, OptionalClassModel> pathEntry : interfaceElement.multiPartParameters
						.entrySet()) {
					if (hasParam2) {
						data.append(", ");
					}
					hasParam2 = true;
					data.append(pathEntry.getKey());
					data.append(": ");
					data.append(generateClassModelsTypescript(pathEntry.getValue().model(), dotGroup));
				}
				*/
				data.append("}");
			}
			data.append("): ");
			/*
			String tmp = DotClassElement.generateLocalModel(
					final String ModelName,
					final List<ClassModel> models,
					final DotClassElementGroup dotGroup)
					public static String generateClassModelsTypescript(
							final List<ClassModel> models,
							final DotClassElementGroup dotGroup,
							final Set<ClassModel> imports) throws IOException {
			*/
			final String returnType = generateClassModelsTypescript(interfaceElement.returnTypes, dotGroup);
			data.append(returnType);
			final List<String> returnLinks = generateClassModelsLinks(interfaceElement.returnTypes, dotGroup);
			for (final String link : returnLinks) {
				outLinks.append("\t");
				outLinks.append(element.name);
				outLinks.append(":");
				outLinks.append(interfaceElement.name);
				outLinks.append(":e -> ");
				outLinks.append(link);
				outLinks.append(":NAME:w\n");
			}
			data.append("</b>");
			//data.append("<br align=\"left\"/>&nbsp;&nbsp;&nbsp;&nbsp;");
			data.append("</td></tr>\n\t\t\t\t\t\t\t<tr><td  align=\"left\">    ");
			/*
			data.append("\n\t\t\trestModel: {");
			data.append("\n\t\t\t\tendPoint: \"");
			*/
			data.append(interfaceElement.restEndPoint);
			/*
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
					final String returnType = generateClassModelsTypescript(interfaceElement.returnTypes, dotGroup,
							imports, false);
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
				final DotClassElement retType = dotGroup.find(interfaceElement.returnTypes.get(0));
				if (retType.dotCheckType != null) {
					data.append(", ");
					data.append(retType.dotCheckType);
					imports.add(interfaceElement.returnTypes.get(0));
				}
			}
			*/
			data.append("</td></tr>\n");
		}
		/*
		data.append("\n}\n");

		final StringBuilder out = new StringBuilder();

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
			final DotClassElement dotModel = dotGroup.find(model);
			if (dotModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			finalImportSet.add(dotModel.dotTypeName);
		}
		for (final ClassModel model : isImports) {
			final DotClassElement dotModel = dotGroup.find(model);
			if (dotModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			if (dotModel.dotCheckType != null) {
				finalImportSet.add(dotModel.dotCheckType);
			}
		}
		for (final ClassModel model : zodImports) {
			final DotClassElement dotModel = dotGroup.find(model);
			if (dotModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			finalImportSet.add("Zod" + dotModel.dotTypeName);
		}
		for (final ClassModel model : writeImports) {
			final DotClassElement dotModel = dotGroup.find(model);
			if (dotModel.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			finalImportSet.add(dotModel.dotTypeName + "Write");
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
		*/

		data.append("""
									</table>
								</td>
							</tr>
						</table>>
					]
				""");
		data.append(outLinks.toString());
		return data.toString();
	}

}
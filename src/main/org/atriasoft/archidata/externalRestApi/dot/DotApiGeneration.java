package org.atriasoft.archidata.externalRestApi.dot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.atriasoft.archidata.externalRestApi.dot.DotClassElement.DefinedPosition;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModelList;
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
				""".replace("%s", element.name));
		for (final ApiModel interfaceElement : element.interfaces) {
			data.append("\t\t\t\t\t<tr><td  align=\"left\" port=\"");
			data.append(interfaceElement.name);
			data.append("\"><b> + ");
			data.append(interfaceElement.name);
			data.append("(");
			boolean hasParam = false;
			if (interfaceElement.unnamedElement.size() == 1) {
				if (hasParam) {
					data.append(", ");
				}
				hasParam = true;
				data.append("data: ");
				data.append(generateClassModelsTypescript(interfaceElement.unnamedElement.get(0).models(), dotGroup));
			} else if (interfaceElement.multiPartParameters.size() != 0) {
				if (hasParam) {
					data.append(", ");
				}
				hasParam = true;
				data.append("data: {");
				boolean isFirstPart = true;
				for (final Entry<String, ParameterClassModelList> part : new TreeMap<>(
						interfaceElement.multiPartParameters).entrySet()) {
					if (!isFirstPart) {
						data.append(", ");
					}
					isFirstPart = false;
					data.append(part.getKey());
					if (part.getValue().optional()) {
						data.append("?");
					}
					data.append(": ");
					data.append(generateClassModelsTypescript(part.getValue().models(), dotGroup));
				}
				data.append("}");
			}
			data.append("): ");
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
			data.append("</td></tr>\n\t\t\t\t\t\t\t<tr><td  align=\"left\">    ");
			data.append(interfaceElement.restEndPoint);
			data.append("</td></tr>\n");
		}
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
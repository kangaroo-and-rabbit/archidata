package org.kar.archidata.externalRestApi.dot;

import java.io.IOException;
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
	
	public String generateEnum(final ClassEnumModel model, final DotClassElementGroup dotGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		out.append("""
					%s [
						shape=plain
						label=<<table color="#33FF33" border="2" cellborder="1" cellspacing="0" cellpadding="4">
							<tr>
								<td port="NAME"><b>%s</b><br/>(ENUM)</td>
							</tr>
							<tr>
								<td>
									<table border="0" cellborder="0" cellspacing="0" >
				""".formatted(this.dotTypeName, this.dotTypeName));
		final boolean first = true;
		for (final Entry<String, Object> elem : model.getListOfValues().entrySet()) {
			out.append("\t\t\t\t\t\t<tr><td  align=\"left\"><b> + ");
			out.append(elem.getKey());
			out.append("</b> = ");
			if (elem.getValue() instanceof final Integer value) {
				out.append(value);
			} else {
				out.append("'");
				out.append(elem.getValue());
				out.append("'");
			}
			out.append("</td></tr>\n");
		}
		out.append("""
									</table>
								</td>
							</tr>
						</table>>
					]
				""");
		return out.toString();
	}

	public String generateImporDot(final List<ClassModel> depModels, final DotClassElementGroup dotGroup)
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
		if (clazz == String.class) {
			if (field.sizeMin() > 0) {
				builder.append(".min(");
				builder.append(field.sizeMin());
				builder.append(")");
			}
			if (field.sizeMax() > 0) {
				builder.append(".max(");
				builder.append(field.sizeMax());
				builder.append(")");
			}
		}
		if (clazz == short.class || clazz == Short.class || clazz == int.class || clazz == Integer.class
				|| clazz == long.class || clazz == Long.class || clazz == float.class || clazz == Float.class
				|| clazz == double.class || clazz == Double.class) {
			if (field.min() != null && field.min() > 0) {
				builder.append(".min(");
				builder.append(field.min());
				builder.append(")");
			}
			if (field.max() != null && field.max() > 0) {
				builder.append(".max(");
				builder.append(field.max());
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
		return out.toString();
	}
	
	public String convertHtml(final String data) {
		return data.replace("<", "&lt;").replace(">", "&gt;");
	}

	public String generateObject(final ClassObjectModel model, final DotClassElementGroup dotGroup) throws IOException {
		final StringBuilder out = new StringBuilder();
		final StringBuilder outLinks = new StringBuilder();
		out.append("""
					%s [
						shape=plain
						ranksep="2"
						label=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
							<tr>
								<td port="NAME"><b>%s</b></td>
							</tr>
							<tr>
								<td>
									<table border="0" cellborder="0" cellspacing="0" >
				""".formatted(this.dotTypeName, this.dotTypeName));
		String inheritence = null;
		if (model.getExtendsClass() != null) {
			final ClassModel parentClass = model.getExtendsClass();
			final DotClassElement dotParentModel = dotGroup.find(parentClass);
			inheritence = dotParentModel.dotTypeName;
		}
		if (model.getFields().size() == 0) {
			out.append("\t\t\t\t\t\t<tr><td> <i>(empty)</i> </td></tr>");
		}
		for (final FieldProperty field : model.getFields()) {
			final ClassModel fieldModel = field.model();
			if (field.comment() != null) {
				out.append("\t\t\t\t\t\t<tr><td  align=\"left\"><i> // ");
				out.append(convertHtml(field.comment()));
				out.append("</i></td></tr>\n");
			}
			out.append("\t\t\t\t\t\t<tr><td  align=\"left\" port=\"");
			out.append(field.name());
			out.append("\"><b> + ");
			out.append(field.name());
			out.append("</b>: ");
			
			if (fieldModel instanceof ClassEnumModel) {
				final DotClassElement dotFieldModel = dotGroup.find(fieldModel);
				out.append(dotFieldModel.dotTypeName);
				outLinks.append("\t");
				outLinks.append(this.dotTypeName);
				outLinks.append(":");
				outLinks.append(field.name());
				outLinks.append(":e -> ");
				outLinks.append(dotFieldModel.dotTypeName);
				outLinks.append(":NAME:w\n");
			} else if (fieldModel instanceof ClassObjectModel) {
				final DotClassElement dotFieldModel = dotGroup.find(fieldModel);
				out.append(dotFieldModel.dotTypeName);
				if (dotFieldModel.nativeType == DefinedPosition.NORMAL) {
					outLinks.append(this.dotTypeName);
					outLinks.append(":");
					outLinks.append(field.name());
					outLinks.append(":e -> ");
					outLinks.append(dotFieldModel.dotTypeName);
					outLinks.append(":NAME:w\n");
				}
			} else if (fieldModel instanceof final ClassListModel fieldListModel) {
				final String data = generateDotList(fieldListModel, dotGroup);
				out.append(data);
				final String className = generateDotListClassName(fieldListModel, dotGroup);
				if (className != null) {
					outLinks.append(this.dotTypeName);
					outLinks.append(":");
					outLinks.append(field.name());
					outLinks.append(":e -> ");
					outLinks.append(className);
					outLinks.append(":NAME:w\n");
				}
			} else if (fieldModel instanceof final ClassMapModel fieldMapModel) {
				final String data = generateDotMap(fieldMapModel, dotGroup);
				out.append(data);
				final String className = generateDotMapClassName(fieldMapModel, dotGroup);
				if (className != null) {
					outLinks.append(this.dotTypeName);
					outLinks.append(":");
					outLinks.append(field.name());
					outLinks.append(":e -> ");
					outLinks.append(className);
					outLinks.append(":NAME:w\n");
				}
			} /*
				out.append(maxSizeZod(field));
				out.append(readOnlyZod(field));
				out.append(optionalTypeZod(field));
				out.append(",\n");
				*/
			out.append("</td></tr>\n");
		}
		out.append("""
									</table>
								</td>
							</tr>
						</table>>
					]
				""");
		if (inheritence != null) {
			out.append("\tedge [dir=back arrowtail=empty arrowsize=2]\n");
			out.append("\t");
			out.append(inheritence);
			// heritage stop link on the "s" South
			out.append(":s -> ");
			out.append(this.dotTypeName);
			// heritage start link on the "n" North
			out.append(":n\n");
		}
		if (!outLinks.isEmpty()) {
			out.append("\tedge [dir=back arrowtail=diamond arrowsize=2]\n");
			//out.append("\tedge [arrowhead=diamond arrowsize=2]\n");
			out.append(outLinks.toString());
			
		}
		return out.toString();
		
	}

	private static String generateDotMap(final ClassMapModel model, final DotClassElementGroup dotGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("Map&lt;");
		if (model.keyModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateDotList(fieldListModel, dotGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateDotMap(fieldMapModel, dotGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateDotObject(fieldObjectModel, dotGroup);
			out.append(tmp);
		} else if (model.keyModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateDotEnum(fieldEnumModel, dotGroup);
			out.append(tmp);
		}
		out.append(", ");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateDotList(fieldListModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateDotMap(fieldMapModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateDotObject(fieldObjectModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			final String tmp = generateDotEnum(fieldEnumModel, dotGroup);
			out.append(tmp);
		}
		out.append("&gt;");
		return out.toString();
	}
	
	private static String generateDotEnum(final ClassEnumModel model, final DotClassElementGroup dotGroup) {
		final DotClassElement dotParentModel = dotGroup.find(model);
		return dotParentModel.dotTypeName;
	}
	
	private static String generateDotObject(final ClassObjectModel model, final DotClassElementGroup dotGroup) {
		final DotClassElement dotParentModel = dotGroup.find(model);
		return dotParentModel.dotTypeName;
	}

	private static String generateDotObjectClassName(
			final ClassObjectModel model,
			final DotClassElementGroup dotGroup) {
		final DotClassElement dotParentModel = dotGroup.find(model);
		if (dotParentModel.nativeType == DefinedPosition.NORMAL) {
			return dotParentModel.dotTypeName;
		}
		return null;
	}
	
	private static String generateDotListClassName(final ClassListModel model, final DotClassElementGroup dotGroup) {
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			return generateDotListClassName(fieldListModel, dotGroup);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			return generateDotMapClassName(fieldMapModel, dotGroup);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			return generateDotObjectClassName(fieldObjectModel, dotGroup);
		}
		return null;
	}
	
	private static String generateDotMapClassName(final ClassMapModel model, final DotClassElementGroup dotGroup) {
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			return generateDotListClassName(fieldListModel, dotGroup);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			return generateDotMapClassName(fieldMapModel, dotGroup);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			return generateDotObjectClassName(fieldObjectModel, dotGroup);
		} else if (model.valueModel instanceof final ClassEnumModel fieldEnumModel) {
			return generateDotEnum(fieldEnumModel, dotGroup);
		}
		return null;
	}

	private static String generateDotList(final ClassListModel model, final DotClassElementGroup dotGroup) {
		final StringBuilder out = new StringBuilder();
		out.append("List&lt;");
		if (model.valueModel instanceof final ClassListModel fieldListModel) {
			final String tmp = generateDotList(fieldListModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassMapModel fieldMapModel) {
			final String tmp = generateDotMap(fieldMapModel, dotGroup);
			out.append(tmp);
		} else if (model.valueModel instanceof final ClassObjectModel fieldObjectModel) {
			final String tmp = generateDotObject(fieldObjectModel, dotGroup);
			out.append(tmp);
		}
		out.append("&gt;");
		return out.toString();
	}
	
	public String generateFile(final DotClassElementGroup dotGroup) throws IOException {
		if (this.nativeType == DefinedPosition.NATIVE) {
			return "";
		}
		final ClassModel model = this.models.get(0);
		String data = "";
		if (this.nativeType == DefinedPosition.BASIC && model instanceof ClassObjectModel) {
			// nothing to do___ data = generateBaseObject();
		} else if (model instanceof final ClassEnumModel modelEnum) {
			data = generateEnum(modelEnum, dotGroup);
		} else if (model instanceof final ClassObjectModel modelObject) {
			data = generateObject(modelObject, dotGroup);
		}
		return data;
	}
	
	private static String generateLocalModelBase(final ClassModel model, final DotClassElementGroup dotGroup)
			throws IOException {
		if (model instanceof final ClassObjectModel objectModel) {
			return generateDotObject(objectModel, dotGroup);
		}
		if (model instanceof final ClassEnumModel enumModel) {
			return generateDotEnum(enumModel, dotGroup);
		}
		if (model instanceof final ClassListModel listModel) {
			return generateDotList(listModel, dotGroup);
		}
		if (model instanceof final ClassMapModel mapModel) {
			return generateDotMap(mapModel, dotGroup);
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
		if (models.size() == 1) {
			out.append(generateLocalModelBase(models.get(0), dotGroup));
		} else {
			out.append("Union&lt;");
			for (final ClassModel model : models) {
				out.append("\t");
				out.append(generateLocalModelBase(models.get(0), dotGroup));
				out.append(",\n");
			}
			out.append("&gt;");
		}
		return out.toString();
	}
	
}
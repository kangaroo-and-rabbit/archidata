package org.atriasoft.archidata.externalRestApi;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.CheckForeignKey;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModelList;
import org.atriasoft.archidata.externalRestApi.model.RestTypeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Draw.io (.drawio) diagrams from {@link AnalyzeApi} introspection data.
 *
 * <p>Produces editable UML-style diagrams in mxGraph XML format that can be
 * opened and modified in Draw.io / diagrams.net.
 *
 * <p>Three generation modes are available:
 * <ul>
 *   <li>{@link #generateApi} — models + REST endpoints (combined)</li>
 *   <li>{@link #generateModels} — data models only</li>
 *   <li>{@link #generateRestApi} — REST endpoints only</li>
 * </ul>
 */
public class DrawioGenerateApi {
	private static final Logger LOGGER = LoggerFactory.getLogger(DrawioGenerateApi.class);

	// Layout constants
	private static final int COLUMN_SPACING = 280;
	private static final int ROW_SPACING = 40;
	private static final int HEADER_HEIGHT = 30;
	private static final int FIELD_HEIGHT = 20;
	private static final int MIN_BOX_WIDTH = 200;
	private static final int CHAR_WIDTH = 7;
	private static final int MAX_COLUMN_HEIGHT = 2000;
	private static final int INITIAL_X = 50;
	private static final int INITIAL_Y = 50;

	// Styles
	private static final String STYLE_MODEL = "swimlane;fontStyle=1;align=center;startSize=26;fillColor=#dae8fc;strokeColor=#6c8ebf;html=1;collapsible=0;";
	private static final String STYLE_ENUM = "swimlane;fontStyle=1;align=center;startSize=26;fillColor=#d5e8d4;strokeColor=#82b366;html=1;collapsible=0;";
	private static final String STYLE_REST = "swimlane;fontStyle=1;align=center;startSize=26;fillColor=#f8cecc;strokeColor=#b85450;html=1;collapsible=0;";
	private static final String STYLE_FIELD = "text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=4;spacingRight=4;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;fontStyle=0;html=1;";
	private static final String STYLE_SEPARATOR = "line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;spacingLeft=3;spacingRight=10;rotatable=0;labelPosition=left;points=[];portConstraint=eastwest;strokeColor=inherit;html=1;";
	private static final String STYLE_INHERITANCE = "endArrow=block;endFill=0;strokeWidth=2;edgeStyle=orthogonalEdgeStyle;rounded=1;exitX=0.5;exitY=0;exitDx=0;exitDy=0;entryX=0.5;entryY=1;entryDx=0;entryDy=0;";
	private static final String STYLE_RELATION = "endArrow=diamondThin;endFill=1;strokeWidth=2;edgeStyle=orthogonalEdgeStyle;rounded=1;";
	private static final String STYLE_ASSOCIATION = "endArrow=open;endFill=1;dashed=1;strokeWidth=2;edgeStyle=orthogonalEdgeStyle;rounded=1;";
	private static final String STYLE_REST_LINK = "endArrow=open;dashed=1;strokeWidth=2;strokeColor=#b85450;edgeStyle=orthogonalEdgeStyle;rounded=1;";
	private static final String STYLE_FOREIGN_KEY = "endArrow=open;endFill=0;dashed=1;strokeWidth=2;strokeColor=#9673a6;edgeStyle=orthogonalEdgeStyle;rounded=1;";

	private DrawioGenerateApi() {
		// Utility class
	}

	/**
	 * Generate a Draw.io diagram with both data models and REST endpoints.
	 */
	public static void generateApi(final AnalyzeApi api, final String pathDrawioFile) throws Exception {
		final String xml = buildDrawio(api, true, true);
		writeFile(pathDrawioFile, xml);
	}

	/**
	 * Generate a Draw.io diagram with data models only.
	 */
	public static void generateModels(final AnalyzeApi api, final String pathDrawioFile) throws Exception {
		final String xml = buildDrawio(api, true, false);
		writeFile(pathDrawioFile, xml);
	}

	/**
	 * Generate a Draw.io diagram with REST endpoints only.
	 */
	public static void generateRestApi(final AnalyzeApi api, final String pathDrawioFile) throws Exception {
		final String xml = buildDrawio(api, false, true);
		writeFile(pathDrawioFile, xml);
	}

	/**
	 * Generate a Draw.io diagram as a string (for testing).
	 */
	public static String generateApiString(final AnalyzeApi api) throws Exception {
		return buildDrawio(api, true, true);
	}

	// ========== CORE GENERATION ==========

	private static String buildDrawio(final AnalyzeApi api, final boolean includeModels, final boolean includeRest)
			throws Exception {
		final StringBuilder cells = new StringBuilder();
		final IdCounter idCounter = new IdCounter();
		// Maps ClassModel/ApiGroupModel → mxCell id (for edge generation)
		final Map<ClassModel, String> modelNodeIds = new HashMap<>();
		final Map<ApiGroupModel, String> restNodeIds = new HashMap<>();

		// 1. Collect elements to render
		final List<ClassObjectModel> objectModels = new ArrayList<>();
		final List<ClassEnumModel> enumModels = new ArrayList<>();
		if (includeModels) {
			for (final ClassModel model : api.getAllModel()) {
				if (model instanceof ClassObjectModel) {
					final ClassObjectModel objModel = (ClassObjectModel) model;
					if (!isDisplayable(objModel)) {
						continue;
					}
					objectModels.add(objModel);
				} else if (model instanceof ClassEnumModel) {
					enumModels.add((ClassEnumModel) model);
				}
			}
		}
		final List<ApiGroupModel> restGroups = new ArrayList<>();
		if (includeRest) {
			restGroups.addAll(api.getAllApi());
		}

		// 2. Sort object models: parents before children (topological order for inheritance)
		final List<ClassObjectModel> sortedModels = topologicalSort(objectModels);

		// 3. Calculate dimensions for each element
		final Map<Object, int[]> dimensions = new LinkedHashMap<>(); // [width, height]
		for (final ClassObjectModel model : sortedModels) {
			dimensions.put(model, computeModelDimensions(model));
		}
		for (final ClassEnumModel model : enumModels) {
			dimensions.put(model, computeEnumDimensions(model));
		}
		for (final ApiGroupModel group : restGroups) {
			dimensions.put(group, computeRestDimensions(group));
		}

		// 4. Layout: place elements in columns
		final Map<Object, int[]> positions = new LinkedHashMap<>(); // [x, y]
		layoutElements(restGroups, sortedModels, enumModels, dimensions, positions);

		// 5. Generate model nodes
		for (final ClassObjectModel model : sortedModels) {
			final int[] pos = positions.get(model);
			final int[] dim = dimensions.get(model);
			final String nodeId = generateModelNode(cells, idCounter, model, pos[0], pos[1], dim[0], dim[1]);
			modelNodeIds.put(model, nodeId);
		}

		// 6. Generate enum nodes
		for (final ClassEnumModel model : enumModels) {
			final int[] pos = positions.get(model);
			final int[] dim = dimensions.get(model);
			final String nodeId = generateEnumNode(cells, idCounter, model, pos[0], pos[1], dim[0], dim[1]);
			modelNodeIds.put(model, nodeId);
		}

		// 7. Generate REST nodes
		for (final ApiGroupModel group : restGroups) {
			final int[] pos = positions.get(group);
			final int[] dim = dimensions.get(group);
			final String nodeId = generateRestNode(cells, idCounter, group, pos[0], pos[1], dim[0], dim[1]);
			restNodeIds.put(group, nodeId);
		}

		// 8. Generate edges (with position-aware port routing)
		generateEdges(cells, idCounter, sortedModels, restGroups, modelNodeIds, restNodeIds, api, positions,
				dimensions);

		// 9. Compute total diagram size
		int maxX = 0;
		int maxY = 0;
		for (final Map.Entry<Object, int[]> entry : positions.entrySet()) {
			final int[] pos = entry.getValue();
			final int[] dim = dimensions.get(entry.getKey());
			if (pos[0] + dim[0] > maxX) {
				maxX = pos[0] + dim[0];
			}
			if (pos[1] + dim[1] > maxY) {
				maxY = pos[1] + dim[1];
			}
		}

		return wrapInDrawio(cells.toString(), maxX + 100, maxY + 100);
	}

	// ========== NODE GENERATION ==========

	private static String generateModelNode(
			final StringBuilder cells,
			final IdCounter idCounter,
			final ClassObjectModel model,
			final int x,
			final int y,
			final int width,
			final int height) {
		final String parentId = idCounter.next();
		final String name = getSimpleName(model);
		cells.append(mxCell(parentId, escapeXml(name), STYLE_MODEL, "1", x, y, width, height));

		// Separator line
		final String sepId = idCounter.next();
		cells.append(mxCellChild(sepId, parentId, "", STYLE_SEPARATOR, 0, HEADER_HEIGHT, width, 8));

		// Fields
		int fieldY = HEADER_HEIGHT + 8;
		for (final FieldProperty field : model.getFields()) {
			final String fieldId = idCounter.next();
			String fieldText = "+ " + field.name() + ": " + resolveTypeName(field.model());
			// Annotate FK target when no linkClass (standalone @CheckForeignKey)
			if (field.checkForeignKey() != null && field.linkClass() == null) {
				fieldText += " \u2192 " + field.checkForeignKey().target().getSimpleName();
			}
			cells.append(
					mxCellChild(fieldId, parentId, escapeXml(fieldText), STYLE_FIELD, 0, fieldY, width, FIELD_HEIGHT));
			fieldY += FIELD_HEIGHT;
		}

		return parentId;
	}

	private static String generateEnumNode(
			final StringBuilder cells,
			final IdCounter idCounter,
			final ClassEnumModel model,
			final int x,
			final int y,
			final int width,
			final int height) {
		final String parentId = idCounter.next();
		final String name = "\u00ABenum\u00BB " + getSimpleName(model);
		cells.append(mxCell(parentId, escapeXml(name), STYLE_ENUM, "1", x, y, width, height));

		// Separator
		final String sepId = idCounter.next();
		cells.append(mxCellChild(sepId, parentId, "", STYLE_SEPARATOR, 0, HEADER_HEIGHT, width, 8));

		// Values
		int fieldY = HEADER_HEIGHT + 8;
		for (final String value : model.getListOfValues().keySet()) {
			final String fieldId = idCounter.next();
			cells.append(mxCellChild(fieldId, parentId, escapeXml(value), STYLE_FIELD, 0, fieldY, width, FIELD_HEIGHT));
			fieldY += FIELD_HEIGHT;
		}

		return parentId;
	}

	private static String generateRestNode(
			final StringBuilder cells,
			final IdCounter idCounter,
			final ApiGroupModel group,
			final int x,
			final int y,
			final int width,
			final int height) {
		final String parentId = idCounter.next();
		final String name = "\u00ABREST\u00BB " + group.name;
		cells.append(mxCell(parentId, escapeXml(name), STYLE_REST, "1", x, y, width, height));

		// Separator
		final String sepId = idCounter.next();
		cells.append(mxCellChild(sepId, parentId, "", STYLE_SEPARATOR, 0, HEADER_HEIGHT, width, 8));

		// Endpoints
		int fieldY = HEADER_HEIGHT + 8;
		for (final ApiModel endpoint : group.interfaces) {
			final String fieldId = idCounter.next();
			final String method = toHttpMethodName(endpoint.restTypeRequest);
			final String path = normalizePath(endpoint.restEndPoint);
			final String returnType = resolveReturnType(endpoint);
			final String line = method + " " + path + " \u2192 " + returnType;
			cells.append(mxCellChild(fieldId, parentId, escapeXml(line), STYLE_FIELD, 0, fieldY, width, FIELD_HEIGHT));
			fieldY += FIELD_HEIGHT;
		}

		return parentId;
	}

	// ========== EDGE GENERATION ==========

	private static void generateEdges(
			final StringBuilder cells,
			final IdCounter idCounter,
			final List<ClassObjectModel> models,
			final List<ApiGroupModel> restGroups,
			final Map<ClassModel, String> modelNodeIds,
			final Map<ApiGroupModel, String> restNodeIds,
			final AnalyzeApi api) {

		// Inheritance edges (child → parent, arrow points UP to parent)
		// These use fixed top/bottom ports defined in STYLE_INHERITANCE
		for (final ClassObjectModel model : models) {
			final ClassModel parent = model.getExtendsClass();
			if (parent != null && modelNodeIds.containsKey(parent)) {
				final String sourceId = modelNodeIds.get(model);
				final String targetId = modelNodeIds.get(parent);
				final String edgeId = idCounter.next();
				cells.append(mxEdge(edgeId, sourceId, targetId, STYLE_INHERITANCE, null));
			}
		}

		// Field relation edges (linkClass = entity relationship)
		for (final ClassObjectModel model : models) {
			final String sourceId = modelNodeIds.get(model);
			for (final FieldProperty field : model.getFields()) {
				if (field.linkClass() != null && modelNodeIds.containsKey(field.linkClass())) {
					final String targetId = modelNodeIds.get(field.linkClass());
					if (!sourceId.equals(targetId)) {
						final String edgeId = idCounter.next();
						final String style = appendPortStyle(STYLE_RELATION, model, field.linkClass(), positions,
								dimensions);
						cells.append(mxEdge(edgeId, sourceId, targetId, style, field.name()));
					}
				}
			}
		}

		// @CheckForeignKey edges (standalone FK without linkClass)
		for (final ClassObjectModel model : models) {
			final String sourceId = modelNodeIds.get(model);
			for (final FieldProperty field : model.getFields()) {
				if (field.linkClass() != null) {
					continue;
				}
				final CheckForeignKey fk = field.checkForeignKey();
				if (fk == null) {
					continue;
				}
				final String targetId = findModelNodeIdByClass(fk.target(), modelNodeIds);
				if (targetId != null && !sourceId.equals(targetId)) {
					final String edgeId = idCounter.next();
					final ClassModel fkTarget = findModelByClass(fk.target(), models);
					final String style = appendPortStyle(STYLE_FOREIGN_KEY, model, fkTarget, positions, dimensions);
					cells.append(mxEdge(edgeId, sourceId, targetId, style, field.name()));
				}
			}
		}

		// Field type association edges (non-primitive field types)
		for (final ClassObjectModel model : models) {
			final String sourceId = modelNodeIds.get(model);
			for (final FieldProperty field : model.getFields()) {
				if (field.linkClass() != null) {
					continue;
				}
				if (field.checkForeignKey() != null) {
					continue;
				}
				final ClassModel referencedModel = resolveLeafModel(field.model());
				if (referencedModel != null && modelNodeIds.containsKey(referencedModel)) {
					final String targetId = modelNodeIds.get(referencedModel);
					if (!sourceId.equals(targetId)) {
						final String edgeId = idCounter.next();
						final String style = appendPortStyle(STYLE_ASSOCIATION, model, referencedModel, positions,
								dimensions);
						cells.append(mxEdge(edgeId, sourceId, targetId, style, field.name()));
					}
				}
			}
		}

		// REST → Model edges (return types and request body)
		for (final ApiGroupModel group : restGroups) {
			final String sourceId = restNodeIds.get(group);
			if (sourceId == null) {
				continue;
			}
			for (final ApiModel endpoint : group.interfaces) {
				for (final ClassModel returnModel : endpoint.returnTypes) {
					final ClassModel leaf = resolveLeafModel(returnModel);
					if (leaf != null && modelNodeIds.containsKey(leaf)) {
						final String targetId = modelNodeIds.get(leaf);
						final String edgeId = idCounter.next();
						final String style = appendPortStyle(STYLE_REST_LINK, group, leaf, positions, dimensions);
						cells.append(mxEdge(edgeId, sourceId, targetId, style, null));
					}
				}
				for (final ParameterClassModelList param : endpoint.unnamedElement) {
					for (final ClassModel bodyModel : param.models()) {
						final ClassModel leaf = resolveLeafModel(bodyModel);
						if (leaf != null && modelNodeIds.containsKey(leaf)) {
							final String targetId = modelNodeIds.get(leaf);
							final String edgeId = idCounter.next();
							final String style = appendPortStyle(STYLE_REST_LINK, group, leaf, positions, dimensions);
							cells.append(mxEdge(edgeId, sourceId, targetId, style, null));
						}
					}
				}
			}
		}
	}

	/**
	 * Compute exit/entry port positions based on relative placement of source and target,
	 * and append them to the base style string.
	 * <ul>
	 *   <li>If target is mostly to the right → exit right side, enter left side</li>
	 *   <li>If target is mostly to the left → exit left side, enter right side</li>
	 *   <li>If target is mostly above → exit top, enter bottom</li>
	 *   <li>If target is mostly below → exit bottom, enter top</li>
	 * </ul>
	 */
	private static String appendPortStyle(final String baseStyle, final Object source, final Object target,
			final Map<Object, int[]> positions, final Map<Object, int[]> dimensions) {
		final int[] srcPos = positions.get(source);
		final int[] srcDim = dimensions.get(source);
		final int[] tgtPos = positions.get(target);
		final int[] tgtDim = dimensions.get(target);
		if (srcPos == null || srcDim == null || tgtPos == null || tgtDim == null) {
			return baseStyle;
		}
		// Centers
		final double srcCx = srcPos[0] + srcDim[0] * 0.5;
		final double srcCy = srcPos[1] + srcDim[1] * 0.5;
		final double tgtCx = tgtPos[0] + tgtDim[0] * 0.5;
		final double tgtCy = tgtPos[1] + tgtDim[1] * 0.5;
		final double dx = tgtCx - srcCx;
		final double dy = tgtCy - srcCy;

		// Determine dominant direction (account for box aspect ratios)
		final double absDx = Math.abs(dx);
		final double absDy = Math.abs(dy);

		double exitX;
		double exitY;
		double entryX;
		double entryY;

		if (absDx > absDy) {
			// Horizontal dominant
			if (dx > 0) {
				// Target is to the right
				exitX = 1.0;
				exitY = 0.5;
				entryX = 0.0;
				entryY = 0.5;
			} else {
				// Target is to the left
				exitX = 0.0;
				exitY = 0.5;
				entryX = 1.0;
				entryY = 0.5;
			}
		} else {
			// Vertical dominant
			if (dy > 0) {
				// Target is below
				exitX = 0.5;
				exitY = 1.0;
				entryX = 0.5;
				entryY = 0.0;
			} else {
				// Target is above
				exitX = 0.5;
				exitY = 0.0;
				entryX = 0.5;
				entryY = 1.0;
			}
		}

		return baseStyle + "exitX=" + exitX + ";exitY=" + exitY + ";exitDx=0;exitDy=0;"
				+ "entryX=" + entryX + ";entryY=" + entryY + ";entryDx=0;entryDy=0;";
	}

	/**
	 * Find a model node ID by its origin Java class.
	 */
	private static String findModelNodeIdByClass(
			final Class<?> targetClass,
			final Map<ClassModel, String> modelNodeIds) {
		for (final Map.Entry<ClassModel, String> entry : modelNodeIds.entrySet()) {
			if (entry.getKey().getOriginClasses() == targetClass) {
				return entry.getValue();
			}
		}
		return null;
	}

	// ========== LAYOUT (Hybrid: Sugiyama for inheritance + Force-directed for associations) ==========

	// ---------- Data structures ----------

	private static class LayoutEdge {
		final Object source;
		final Object target;
		final int weight;
		final boolean inheritance;

		LayoutEdge(final Object source, final Object target, final int weight, final boolean inheritance) {
			this.source = source;
			this.target = target;
			this.weight = weight;
			this.inheritance = inheritance;
		}
	}

	/** A tree of classes linked by inheritance. Root = topmost parent. */
	private static class InheritanceTree {
		final ClassObjectModel root;
		final List<InheritanceTree> children = new ArrayList<>();

		InheritanceTree(final ClassObjectModel root) {
			this.root = root;
		}
	}

	// ---------- Edge collection ----------

	private static List<LayoutEdge> collectLayoutEdges(
			final List<ClassObjectModel> objectModels,
			final List<ClassEnumModel> enumModels,
			final List<ApiGroupModel> restGroups) {
		final List<LayoutEdge> edges = new ArrayList<>();
		final Set<ClassObjectModel> objectModelSet = new HashSet<>(objectModels);
		final Set<ClassEnumModel> enumModelSet = new HashSet<>(enumModels);

		for (final ClassObjectModel model : objectModels) {
			// Inheritance edges (weight=3): child → parent
			final ClassModel parent = model.getExtendsClass();
			if (parent instanceof ClassObjectModel && objectModelSet.contains(parent)) {
				edges.add(new LayoutEdge(model, parent, 3, true));
			}
			for (final FieldProperty field : model.getFields()) {
				// linkClass/Relation (weight=2)
				if (field.linkClass() != null && objectModelSet.contains(field.linkClass())) {
					if (field.linkClass() != model) {
						edges.add(new LayoutEdge(model, field.linkClass(), 2, false));
					}
				}
				// CheckForeignKey (weight=2)
				if (field.linkClass() == null && field.checkForeignKey() != null) {
					final ClassModel fkTarget = findModelByClass(field.checkForeignKey().target(), objectModels);
					if (fkTarget != null && fkTarget != model) {
						edges.add(new LayoutEdge(model, fkTarget, 2, false));
					}
				}
				// Association (weight=1)
				if (field.linkClass() == null && field.checkForeignKey() == null) {
					final ClassModel leaf = resolveLeafModel(field.model());
					if (leaf != null && leaf != model) {
						if (leaf instanceof ClassObjectModel && objectModelSet.contains(leaf)) {
							edges.add(new LayoutEdge(model, leaf, 1, false));
						} else if (leaf instanceof ClassEnumModel && enumModelSet.contains(leaf)) {
							edges.add(new LayoutEdge(model, leaf, 1, false));
						}
					}
				}
			}
		}

		// REST → model edges (weight=2)
		for (final ApiGroupModel group : restGroups) {
			for (final ApiModel endpoint : group.interfaces) {
				for (final ClassModel returnModel : endpoint.returnTypes) {
					final ClassModel leaf = resolveLeafModel(returnModel);
					if (leaf != null && (objectModelSet.contains(leaf) || enumModelSet.contains(leaf))) {
						edges.add(new LayoutEdge(group, leaf, 2, false));
					}
				}
				for (final ParameterClassModelList param : endpoint.unnamedElement) {
					for (final ClassModel bodyModel : param.models()) {
						final ClassModel leaf = resolveLeafModel(bodyModel);
						if (leaf != null && (objectModelSet.contains(leaf) || enumModelSet.contains(leaf))) {
							edges.add(new LayoutEdge(group, leaf, 2, false));
						}
					}
				}
			}
		}

		return edges;
	}

	private static ClassModel findModelByClass(final Class<?> targetClass, final List<ClassObjectModel> objectModels) {
		for (final ClassObjectModel model : objectModels) {
			if (model.getOriginClasses() == targetClass) {
				return model;
			}
		}
		return null;
	}

	// ---------- Main layout algorithm (Hybrid) ----------

	/**
	 * Hybrid layout algorithm:
	 * <ol>
	 *   <li>Build inheritance forests and layout each tree with Sugiyama (vertical hierarchy)</li>
	 *   <li>Treat each tree, standalone class, enum, and REST group as a "block"</li>
	 *   <li>Use Fruchterman-Reingold force-directed placement for blocks</li>
	 * </ol>
	 */
	private static void layoutElements(
			final List<ApiGroupModel> restGroups,
			final List<ClassObjectModel> objectModels,
			final List<ClassEnumModel> enumModels,
			final Map<Object, int[]> dimensions,
			final Map<Object, int[]> positions) {

		if (objectModels.isEmpty() && enumModels.isEmpty() && restGroups.isEmpty()) {
			return;
		}

		final List<LayoutEdge> allEdges = collectLayoutEdges(objectModels, enumModels, restGroups);

		// Phase 1: Build inheritance forests
		final List<InheritanceTree> forest = buildInheritanceForest(objectModels);

		// Phase 2: Layout each tree vertically (Sugiyama-style: parent on top, children below)
		// Each tree becomes a "block" with internal positions relative to (0,0)
		// blockPositions: element → [relativeX, relativeY] within the block
		// blockDimensions: tree-root → [totalWidth, totalHeight] of the block
		final Map<Object, int[]> blockPositions = new LinkedHashMap<>();
		final Map<Object, int[]> blockDimensions = new LinkedHashMap<>();
		final Map<Object, Object> elementToBlock = new HashMap<>(); // element → block-key

		// Collect standalone classes (not in any tree)
		final Set<ClassObjectModel> inTree = new HashSet<>();
		for (final InheritanceTree tree : forest) {
			collectTreeMembers(tree, inTree);
		}

		// Layout each inheritance tree as a block
		for (final InheritanceTree tree : forest) {
			final Map<Object, int[]> treePos = new LinkedHashMap<>();
			final int[] treeDim = layoutTree(tree, dimensions, treePos, 0, 0);
			for (final Map.Entry<Object, int[]> entry : treePos.entrySet()) {
				blockPositions.put(entry.getKey(), entry.getValue());
				elementToBlock.put(entry.getKey(), tree.root);
			}
			blockDimensions.put(tree.root, treeDim);
		}

		// Standalone classes become their own blocks
		for (final ClassObjectModel model : objectModels) {
			if (!inTree.contains(model)) {
				final int[] dim = dimensions.get(model);
				blockPositions.put(model, new int[] { 0, 0 });
				blockDimensions.put(model, new int[] { dim[0], dim[1] });
				elementToBlock.put(model, model);
			}
		}

		// Enums as blocks
		for (final ClassEnumModel model : enumModels) {
			final int[] dim = dimensions.get(model);
			blockPositions.put(model, new int[] { 0, 0 });
			blockDimensions.put(model, new int[] { dim[0], dim[1] });
			elementToBlock.put(model, model);
		}

		// REST groups as blocks
		for (final ApiGroupModel group : restGroups) {
			final int[] dim = dimensions.get(group);
			blockPositions.put(group, new int[] { 0, 0 });
			blockDimensions.put(group, new int[] { dim[0], dim[1] });
			elementToBlock.put(group, group);
		}

		// Phase 3: Collect unique block keys and their non-inheritance edges
		final List<Object> blockKeys = new ArrayList<>();
		final Set<Object> blockKeySet = new LinkedHashSet<>();
		for (final InheritanceTree tree : forest) {
			if (blockKeySet.add(tree.root)) {
				blockKeys.add(tree.root);
			}
		}
		for (final ClassObjectModel model : objectModels) {
			if (!inTree.contains(model) && blockKeySet.add(model)) {
				blockKeys.add(model);
			}
		}
		for (final ClassEnumModel model : enumModels) {
			if (blockKeySet.add(model)) {
				blockKeys.add(model);
			}
		}
		for (final ApiGroupModel group : restGroups) {
			if (blockKeySet.add(group)) {
				blockKeys.add(group);
			}
		}

		// Build inter-block edges from non-inheritance edges
		final List<int[]> blockEdges = new ArrayList<>(); // [blockIdx1, blockIdx2, weight]
		final Map<Object, Integer> blockIndex = new HashMap<>();
		for (int i = 0; i < blockKeys.size(); i++) {
			blockIndex.put(blockKeys.get(i), i);
		}
		for (final LayoutEdge edge : allEdges) {
			if (edge.inheritance) {
				continue;
			}
			final Object srcBlock = elementToBlock.get(edge.source);
			final Object tgtBlock = elementToBlock.get(edge.target);
			if (srcBlock == null || tgtBlock == null || srcBlock == tgtBlock) {
				continue;
			}
			final Integer srcIdx = blockIndex.get(srcBlock);
			final Integer tgtIdx = blockIndex.get(tgtBlock);
			if (srcIdx != null && tgtIdx != null && !srcIdx.equals(tgtIdx)) {
				blockEdges.add(new int[] { srcIdx, tgtIdx, edge.weight });
			}
		}

		// Phase 4: Force-directed placement for blocks
		final int blockCount = blockKeys.size();
		final double[] bx = new double[blockCount];
		final double[] by = new double[blockCount];
		final int[] bw = new int[blockCount];
		final int[] bh = new int[blockCount];

		for (int i = 0; i < blockCount; i++) {
			final int[] dim = blockDimensions.get(blockKeys.get(i));
			bw[i] = dim[0];
			bh[i] = dim[1];
		}

		// Initial placement: compact grid
		final int cols = Math.max(1, (int) Math.ceil(Math.sqrt(blockCount)));
		int gridX = 0;
		int gridY = 0;
		int rowMaxH = 0;
		for (int i = 0; i < blockCount; i++) {
			final int col = i % cols;
			if (col == 0 && i > 0) {
				gridY += rowMaxH + ROW_SPACING;
				gridX = 0;
				rowMaxH = 0;
			}
			bx[i] = gridX;
			by[i] = gridY;
			gridX += bw[i] + COLUMN_SPACING;
			if (bh[i] > rowMaxH) {
				rowMaxH = bh[i];
			}
		}

		// Ideal gap between block edges — leave room for orthogonal edge routing
		final double idealGap = 180.0;
		double temperature = 200.0;
		final int iterations = 150;

		for (int iter = 0; iter < iterations; iter++) {
			final double[] dispX = new double[blockCount];
			final double[] dispY = new double[blockCount];

			// Repulsive forces — based on gap between box edges, not center distance
			for (int i = 0; i < blockCount; i++) {
				for (int j = i + 1; j < blockCount; j++) {
					final double cx1 = bx[i] + bw[i] * 0.5;
					final double cy1 = by[i] + bh[i] * 0.5;
					final double cx2 = bx[j] + bw[j] * 0.5;
					final double cy2 = by[j] + bh[j] * 0.5;
					double dx = cx1 - cx2;
					double dy = cy1 - cy2;
					// Gap = distance between edges (negative = overlap)
					final double gapX = Math.abs(dx) - (bw[i] + bw[j]) * 0.5;
					final double gapY = Math.abs(dy) - (bh[i] + bh[j]) * 0.5;
					final double gap = Math.max(gapX, gapY);
					// Only repel if boxes are closer than 2x ideal gap
					if (gap < idealGap * 2) {
						final double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1.0);
						// Stronger repulsion when overlapping, fades quickly with distance
						final double effectiveGap = Math.max(gap, 1.0);
						final double force = (idealGap * idealGap) / (effectiveGap * effectiveGap) * 2.0;
						dispX[i] += (dx / dist) * force;
						dispY[i] += (dy / dist) * force;
						dispX[j] -= (dx / dist) * force;
						dispY[j] -= (dy / dist) * force;
					}
				}
			}

			// Attractive forces along edges — spring toward ideal gap
			for (final int[] edge : blockEdges) {
				final int i = edge[0];
				final int j = edge[1];
				final int w = edge[2];
				final double cx1 = bx[i] + bw[i] * 0.5;
				final double cy1 = by[i] + bh[i] * 0.5;
				final double cx2 = bx[j] + bw[j] * 0.5;
				final double cy2 = by[j] + bh[j] * 0.5;
				double dx = cx1 - cx2;
				double dy = cy1 - cy2;
				final double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1.0);
				// Ideal center distance = half-widths + ideal gap
				final double idealDist = (bw[i] + bw[j]) * 0.5 + idealGap;
				// Spring force: proportional to (dist - ideal), scaled by weight
				final double force = (dist - idealDist) / dist * w * 1.5;
				dispX[i] -= (dx / dist) * force;
				dispY[i] -= (dy / dist) * force;
				dispX[j] += (dx / dist) * force;
				dispY[j] += (dy / dist) * force;
			}

			// Gravity toward center — prevents circular explosion
			double centerX = 0;
			double centerY = 0;
			for (int i = 0; i < blockCount; i++) {
				centerX += bx[i] + bw[i] * 0.5;
				centerY += by[i] + bh[i] * 0.5;
			}
			centerX /= blockCount;
			centerY /= blockCount;
			final double gravity = 0.3;
			for (int i = 0; i < blockCount; i++) {
				final double cx = bx[i] + bw[i] * 0.5;
				final double cy = by[i] + bh[i] * 0.5;
				dispX[i] -= (cx - centerX) * gravity;
				dispY[i] -= (cy - centerY) * gravity;
			}

			// Apply displacements with temperature limiting
			for (int i = 0; i < blockCount; i++) {
				final double dLen = Math.max(Math.sqrt(dispX[i] * dispX[i] + dispY[i] * dispY[i]), 0.001);
				final double scale = Math.min(dLen, temperature) / dLen;
				bx[i] += dispX[i] * scale;
				by[i] += dispY[i] * scale;
			}

			// Cool down
			temperature *= 0.97;
		}

		// Phase 5: Remove overlaps with sweep-based adjustment
		removeOverlaps(bx, by, bw, bh, blockCount);

		// Phase 6: Normalize positions (shift so minimum is at INITIAL_X, INITIAL_Y)
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		for (int i = 0; i < blockCount; i++) {
			if (bx[i] < minX) {
				minX = bx[i];
			}
			if (by[i] < minY) {
				minY = by[i];
			}
		}
		for (int i = 0; i < blockCount; i++) {
			bx[i] = bx[i] - minX + INITIAL_X;
			by[i] = by[i] - minY + INITIAL_Y;
		}

		// Phase 7: Apply block positions to individual elements
		for (int i = 0; i < blockCount; i++) {
			final Object blockKey = blockKeys.get(i);
			final int blockX = (int) Math.round(bx[i]);
			final int blockY = (int) Math.round(by[i]);

			// Find all elements belonging to this block
			for (final Map.Entry<Object, Object> entry : elementToBlock.entrySet()) {
				if (entry.getValue() == blockKey) {
					final Object element = entry.getKey();
					final int[] relPos = blockPositions.get(element);
					if (relPos != null) {
						positions.put(element, new int[] { blockX + relPos[0], blockY + relPos[1] });
					}
				}
			}
		}
	}

	// ---------- Inheritance forest ----------

	private static List<InheritanceTree> buildInheritanceForest(final List<ClassObjectModel> objectModels) {
		final Set<ClassObjectModel> modelSet = new HashSet<>(objectModels);
		final Map<ClassObjectModel, InheritanceTree> treeNodes = new LinkedHashMap<>();

		// Create tree nodes
		for (final ClassObjectModel model : objectModels) {
			treeNodes.put(model, new InheritanceTree(model));
		}

		// Link children to parents
		final Set<ClassObjectModel> hasParent = new HashSet<>();
		for (final ClassObjectModel model : objectModels) {
			final ClassModel parent = model.getExtendsClass();
			if (parent instanceof ClassObjectModel && modelSet.contains(parent)) {
				final InheritanceTree parentTree = treeNodes.get(parent);
				final InheritanceTree childTree = treeNodes.get(model);
				if (parentTree != null && childTree != null) {
					parentTree.children.add(childTree);
					hasParent.add(model);
				}
			}
		}

		// Roots = models without a parent in the set
		final List<InheritanceTree> roots = new ArrayList<>();
		for (final ClassObjectModel model : objectModels) {
			if (!hasParent.contains(model) && treeNodes.containsKey(model)) {
				final InheritanceTree tree = treeNodes.get(model);
				// Only include as tree if it actually has children
				if (!tree.children.isEmpty()) {
					roots.add(tree);
				}
			}
		}

		return roots;
	}

	private static void collectTreeMembers(final InheritanceTree tree, final Set<ClassObjectModel> members) {
		members.add(tree.root);
		for (final InheritanceTree child : tree.children) {
			collectTreeMembers(child, members);
		}
	}

	// ---------- Tree layout (Sugiyama-style vertical hierarchy) ----------

	/**
	 * Layout an inheritance tree: parent centered on top, children spread below.
	 * Returns [totalWidth, totalHeight] of the tree block.
	 */
	private static int[] layoutTree(
			final InheritanceTree tree,
			final Map<Object, int[]> dimensions,
			final Map<Object, int[]> treePos,
			final int offsetX,
			final int offsetY) {
		final int[] rootDim = dimensions.get(tree.root);
		final int rootW = rootDim[0];
		final int rootH = rootDim[1];

		if (tree.children.isEmpty()) {
			treePos.put(tree.root, new int[] { offsetX, offsetY });
			return new int[] { rootW, rootH };
		}

		// Layout children first to determine total children width
		final int childSpacing = 60;
		final int verticalGap = 80;
		int totalChildrenWidth = 0;
		final List<int[]> childDims = new ArrayList<>();
		final List<Map<Object, int[]>> childPositions = new ArrayList<>();

		for (int i = 0; i < tree.children.size(); i++) {
			final Map<Object, int[]> childPos = new LinkedHashMap<>();
			final int[] childDim = layoutTree(tree.children.get(i), dimensions, childPos, 0, 0);
			childDims.add(childDim);
			childPositions.add(childPos);
			totalChildrenWidth += childDim[0];
			if (i < tree.children.size() - 1) {
				totalChildrenWidth += childSpacing;
			}
		}

		final int totalWidth = Math.max(rootW, totalChildrenWidth);
		final int totalHeight = rootH + verticalGap + maxHeight(childDims);

		// Center root
		final int rootX = offsetX + (totalWidth - rootW) / 2;
		treePos.put(tree.root, new int[] { rootX, offsetY });

		// Place children below, centered under the total width
		int childX = offsetX + (totalWidth - totalChildrenWidth) / 2;
		final int childY = offsetY + rootH + verticalGap;
		for (int i = 0; i < tree.children.size(); i++) {
			final Map<Object, int[]> childPos = childPositions.get(i);
			final int[] childDim = childDims.get(i);
			// Shift child positions by (childX, childY)
			for (final Map.Entry<Object, int[]> entry : childPos.entrySet()) {
				final int[] pos = entry.getValue();
				treePos.put(entry.getKey(), new int[] { childX + pos[0], childY + pos[1] });
			}
			childX += childDim[0] + childSpacing;
		}

		return new int[] { totalWidth, totalHeight };
	}

	private static int maxHeight(final List<int[]> dims) {
		int max = 0;
		for (final int[] dim : dims) {
			if (dim[1] > max) {
				max = dim[1];
			}
		}
		return max;
	}

	// ---------- Overlap removal ----------

	/**
	 * Remove overlaps between blocks using iterative push-apart.
	 */
	private static void removeOverlaps(
			final double[] bx,
			final double[] by,
			final int[] bw,
			final int[] bh,
			final int count) {
		final int padding = 120;
		for (int pass = 0; pass < 50; pass++) {
			boolean moved = false;
			for (int i = 0; i < count; i++) {
				for (int j = i + 1; j < count; j++) {
					final double overlapX = (bx[i] + bw[i] + padding) - bx[j];
					final double overlapY = (by[i] + bh[i] + padding) - by[j];
					final double overlapXr = (bx[j] + bw[j] + padding) - bx[i];
					final double overlapYr = (by[j] + bh[j] + padding) - by[i];

					// Check if rectangles actually overlap
					if (overlapX > 0 && overlapXr > 0 && overlapY > 0 && overlapYr > 0) {
						// Push apart in the direction of minimum overlap
						final double pushX = Math.min(overlapX, overlapXr);
						final double pushY = Math.min(overlapY, overlapYr);
						if (pushX < pushY) {
							// Push horizontally
							if (bx[i] < bx[j]) {
								bx[i] -= pushX * 0.5;
								bx[j] += pushX * 0.5;
							} else {
								bx[i] += pushX * 0.5;
								bx[j] -= pushX * 0.5;
							}
						} else {
							// Push vertically
							if (by[i] < by[j]) {
								by[i] -= pushY * 0.5;
								by[j] += pushY * 0.5;
							} else {
								by[i] += pushY * 0.5;
								by[j] -= pushY * 0.5;
							}
						}
						moved = true;
					}
				}
			}
			if (!moved) {
				break;
			}
		}
	}

	// ---------- Helpers ----------

	private static String getElementName(final Object elem) {
		if (elem instanceof ClassObjectModel) {
			return getSimpleName((ClassModel) elem);
		}
		if (elem instanceof ClassEnumModel) {
			return getSimpleName((ClassModel) elem);
		}
		if (elem instanceof ApiGroupModel) {
			return ((ApiGroupModel) elem).name;
		}
		return elem.toString();
	}

	// ========== DIMENSIONS ==========

	private static int[] computeModelDimensions(final ClassObjectModel model) {
		final int fieldCount = model.getFields().size();
		final int height = HEADER_HEIGHT + 8 + Math.max(fieldCount, 1) * FIELD_HEIGHT + 8;
		int maxTextLen = getSimpleName(model).length();
		for (final FieldProperty field : model.getFields()) {
			String line = "+ " + field.name() + ": " + resolveTypeName(field.model());
			if (field.checkForeignKey() != null && field.linkClass() == null) {
				line += " \u2192 " + field.checkForeignKey().target().getSimpleName();
			}
			if (line.length() > maxTextLen) {
				maxTextLen = line.length();
			}
		}
		final int width = Math.max(MIN_BOX_WIDTH, maxTextLen * CHAR_WIDTH + 20);
		return new int[] { width, height };
	}

	private static int[] computeEnumDimensions(final ClassEnumModel model) {
		final int valueCount = model.getListOfValues().size();
		final int height = HEADER_HEIGHT + 8 + Math.max(valueCount, 1) * FIELD_HEIGHT + 8;
		int maxTextLen = ("\u00ABenum\u00BB " + getSimpleName(model)).length();
		for (final String value : model.getListOfValues().keySet()) {
			if (value.length() > maxTextLen) {
				maxTextLen = value.length();
			}
		}
		final int width = Math.max(MIN_BOX_WIDTH, maxTextLen * CHAR_WIDTH + 20);
		return new int[] { width, height };
	}

	private static int[] computeRestDimensions(final ApiGroupModel group) {
		final int endpointCount = group.interfaces.size();
		final int height = HEADER_HEIGHT + 8 + Math.max(endpointCount, 1) * FIELD_HEIGHT + 8;
		int maxTextLen = ("\u00ABREST\u00BB " + group.name).length();
		for (final ApiModel endpoint : group.interfaces) {
			final String method = toHttpMethodName(endpoint.restTypeRequest);
			final String path = normalizePath(endpoint.restEndPoint);
			final String returnType = resolveReturnType(endpoint);
			final String line = method + " " + path + " \u2192 " + returnType;
			if (line.length() > maxTextLen) {
				maxTextLen = line.length();
			}
		}
		final int width = Math.max(MIN_BOX_WIDTH, maxTextLen * CHAR_WIDTH + 20);
		return new int[] { width, height };
	}

	// ========== TOPOLOGICAL SORT ==========

	/**
	 * Sort models so that parent classes appear before children.
	 * This ensures inheritance arrows go top-to-bottom in the diagram.
	 */
	private static List<ClassObjectModel> topologicalSort(final List<ClassObjectModel> models) {
		final Map<ClassObjectModel, Boolean> visited = new LinkedHashMap<>();
		final List<ClassObjectModel> sorted = new ArrayList<>();
		final Map<ClassModel, ClassObjectModel> modelMap = new HashMap<>();

		for (final ClassObjectModel model : models) {
			modelMap.put(model, model);
		}

		for (final ClassObjectModel model : models) {
			if (!visited.containsKey(model)) {
				topoVisit(model, visited, sorted, modelMap);
			}
		}
		return sorted;
	}

	private static void topoVisit(
			final ClassObjectModel model,
			final Map<ClassObjectModel, Boolean> visited,
			final List<ClassObjectModel> sorted,
			final Map<ClassModel, ClassObjectModel> modelMap) {
		if (visited.containsKey(model)) {
			return;
		}
		visited.put(model, true);
		// Visit parent first
		final ClassModel parent = model.getExtendsClass();
		if (parent != null) {
			final ClassObjectModel parentObj = modelMap.get(parent);
			if (parentObj != null) {
				topoVisit(parentObj, visited, sorted, modelMap);
			}
		}
		sorted.add(model);
	}

	// ========== XML BUILDING ==========

	private static String mxCell(
			final String id,
			final String value,
			final String style,
			final String parent,
			final int x,
			final int y,
			final int width,
			final int height) {
		return "\t\t\t\t<mxCell id=\"" + id + "\" value=\"" + value + "\" style=\"" + style
				+ "\" vertex=\"1\" parent=\"" + parent + "\">\n" + "\t\t\t\t\t<mxGeometry x=\"" + x + "\" y=\"" + y
				+ "\" width=\"" + width + "\" height=\"" + height + "\" as=\"geometry\"/>\n" + "\t\t\t\t</mxCell>\n";
	}

	private static String mxCellChild(
			final String id,
			final String parentId,
			final String value,
			final String style,
			final int x,
			final int y,
			final int width,
			final int height) {
		return "\t\t\t\t<mxCell id=\"" + id + "\" value=\"" + value + "\" style=\"" + style
				+ "\" vertex=\"1\" parent=\"" + parentId + "\">\n" + "\t\t\t\t\t<mxGeometry x=\"" + x + "\" y=\"" + y
				+ "\" width=\"" + width + "\" height=\"" + height + "\" as=\"geometry\"/>\n" + "\t\t\t\t</mxCell>\n";
	}

	private static String mxEdge(
			final String id,
			final String sourceId,
			final String targetId,
			final String style,
			final String label) {
		final String value = (label != null && !label.isEmpty()) ? escapeXml(label) : "";
		return "\t\t\t\t<mxCell id=\"" + id + "\" value=\"" + value + "\" style=\"" + style
				+ "\" edge=\"1\" parent=\"1\" source=\"" + sourceId + "\" target=\"" + targetId + "\">\n"
				+ "\t\t\t\t\t<mxGeometry relative=\"1\" as=\"geometry\"/>\n" + "\t\t\t\t</mxCell>\n";
	}

	private static String wrapInDrawio(final String cellsContent, final int dx, final int dy) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<mxfile host=\"archidata\">\n"
				+ "\t<diagram id=\"archidata-diagram\" name=\"Page-1\">\n" + "\t\t<mxGraphModel dx=\"" + dx + "\" dy=\""
				+ dy
				+ "\" grid=\"0\" gridSize=\"10\" guides=\"1\" tooltips=\"1\" connect=\"1\" arrows=\"1\" fold=\"1\" page=\"0\" pageScale=\"1\" background=\"#FFFFFF\" pageWidth=\""
				+ Math.max(1100, dx + 100) + "\" pageHeight=\"" + Math.max(850, dy + 100)
				+ "\" math=\"0\" shadow=\"0\">\n" + "\t\t\t<root>\n" + "\t\t\t\t<mxCell id=\"0\"/>\n"
				+ "\t\t\t\t<mxCell id=\"1\" parent=\"0\"/>\n" + cellsContent + "\t\t\t</root>\n"
				+ "\t\t</mxGraphModel>\n" + "\t</diagram>\n" + "</mxfile>\n";
	}

	// ========== HELPERS ==========

	private static String getSimpleName(final ClassModel model) {
		final Class<?> clazz = model.getOriginClasses();
		if (clazz == null) {
			return "Unknown";
		}
		final String name = clazz.getSimpleName();
		return name.isEmpty() ? clazz.getName() : name;
	}

	private static String resolveTypeName(final ClassModel model) {
		if (model instanceof ClassObjectModel) {
			return getSimpleName(model);
		}
		if (model instanceof ClassEnumModel) {
			return getSimpleName(model);
		}
		if (model instanceof ClassListModel) {
			final ClassListModel listModel = (ClassListModel) model;
			return "List&lt;" + resolveTypeName(listModel.valueModel) + "&gt;";
		}
		if (model instanceof ClassMapModel) {
			final ClassMapModel mapModel = (ClassMapModel) model;
			return "Map&lt;" + resolveTypeName(mapModel.keyModel) + ", " + resolveTypeName(mapModel.valueModel)
					+ "&gt;";
		}
		return "Object";
	}

	private static String resolveReturnType(final ApiModel endpoint) {
		if (endpoint.returnTypes.isEmpty()) {
			return "void";
		}
		final ClassModel returnModel = endpoint.returnTypes.get(0);
		return resolveTypeName(returnModel);
	}

	/**
	 * Resolves the "leaf" model for a potentially wrapped type (List, Map).
	 * Returns null if the leaf is a primitive/basic type.
	 */
	private static ClassModel resolveLeafModel(final ClassModel model) {
		if (model instanceof ClassListModel) {
			return resolveLeafModel(((ClassListModel) model).valueModel);
		}
		if (model instanceof ClassMapModel) {
			return resolveLeafModel(((ClassMapModel) model).valueModel);
		}
		if (model instanceof ClassObjectModel) {
			final ClassObjectModel objModel = (ClassObjectModel) model;
			if (isDisplayable(objModel)) {
				return objModel;
			}
			return null;
		}
		if (model instanceof ClassEnumModel) {
			return model;
		}
		return null;
	}

	private static boolean isDisplayable(final ClassObjectModel model) {
		if (model.isPrimitive()) {
			return false;
		}
		final Class<?> clazz = model.getOriginClasses();
		if (clazz == null) {
			return false;
		}
		if (clazz == Void.class || clazz == void.class || clazz == Object.class) {
			return false;
		}
		// Filter out basic wrapper types
		if (clazz == String.class || clazz == Boolean.class || clazz == boolean.class || clazz == Integer.class
				|| clazz == int.class || clazz == Long.class || clazz == long.class || clazz == Float.class
				|| clazz == float.class || clazz == Double.class || clazz == double.class || clazz == Short.class
				|| clazz == short.class || clazz == Character.class || clazz == char.class || clazz == byte[].class) {
			return false;
		}
		// Filter out common value types that don't need their own box
		if (clazz == java.util.Date.class || clazz == org.bson.types.ObjectId.class || clazz == org.bson.Document.class
				|| clazz == CharSequence.class || clazz == java.io.InputStream.class) {
			return false;
		}
		return true;
	}

	private static String toHttpMethodName(final RestTypeRequest restType) {
		switch (restType) {
			case GET:
				return "GET";
			case POST:
				return "POST";
			case PUT:
				return "PUT";
			case PATCH:
				return "PATCH";
			case DELETE:
				return "DELETE";
			case ARCHIVE:
				return "ARCHIVE";
			case RESTORE:
				return "RESTORE";
			case CALL:
				return "CALL";
			default:
				return restType.name();
		}
	}

	private static String normalizePath(final String path) {
		String normalized = path.replaceAll("//+", "/");
		if (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	private static String escapeXml(final String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&apos;");
	}

	private static void writeFile(final String path, final String content) throws Exception {
		final File file = new File(path);
		final File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		try (final FileWriter writer = new FileWriter(file)) {
			writer.write(content);
		}
		LOGGER.info("Draw.io diagram written to: {}", path);
	}

	// ========== ID COUNTER ==========

	private static class IdCounter {
		private int current = 2; // 0 and 1 are reserved

		public String next() {
			return String.valueOf(this.current++);
		}
	}
}

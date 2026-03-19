package org.atriasoft.archidata.externalRestApi;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
	private static final String STYLE_INHERITANCE = "endArrow=block;endFill=0;strokeWidth=2;edgeStyle=orthogonalEdgeStyle;exitX=0.5;exitY=0;exitDx=0;exitDy=0;entryX=0.5;entryY=1;entryDx=0;entryDy=0;";
	private static final String STYLE_RELATION = "endArrow=diamondThin;endFill=1;strokeWidth=2;edgeStyle=orthogonalEdgeStyle;";
	private static final String STYLE_ASSOCIATION = "endArrow=open;endFill=1;dashed=1;strokeWidth=2;edgeStyle=orthogonalEdgeStyle;";
	private static final String STYLE_REST_LINK = "endArrow=open;dashed=1;strokeWidth=2;strokeColor=#b85450;edgeStyle=orthogonalEdgeStyle;";
	private static final String STYLE_FOREIGN_KEY = "endArrow=open;endFill=0;dashed=1;strokeWidth=2;strokeColor=#9673a6;edgeStyle=orthogonalEdgeStyle;";

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

		// 8. Generate edges
		generateEdges(cells, idCounter, sortedModels, restGroups, modelNodeIds, restNodeIds, api);

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

	private static String generateModelNode(final StringBuilder cells, final IdCounter idCounter,
			final ClassObjectModel model, final int x, final int y, final int width, final int height) {
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
			cells.append(mxCellChild(fieldId, parentId, escapeXml(fieldText), STYLE_FIELD, 0, fieldY, width,
					FIELD_HEIGHT));
			fieldY += FIELD_HEIGHT;
		}

		return parentId;
	}

	private static String generateEnumNode(final StringBuilder cells, final IdCounter idCounter,
			final ClassEnumModel model, final int x, final int y, final int width, final int height) {
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
			cells.append(mxCellChild(fieldId, parentId, escapeXml(value), STYLE_FIELD, 0, fieldY, width,
					FIELD_HEIGHT));
			fieldY += FIELD_HEIGHT;
		}

		return parentId;
	}

	private static String generateRestNode(final StringBuilder cells, final IdCounter idCounter,
			final ApiGroupModel group, final int x, final int y, final int width, final int height) {
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
			cells.append(mxCellChild(fieldId, parentId, escapeXml(line), STYLE_FIELD, 0, fieldY, width,
					FIELD_HEIGHT));
			fieldY += FIELD_HEIGHT;
		}

		return parentId;
	}

	// ========== EDGE GENERATION ==========

	private static void generateEdges(final StringBuilder cells, final IdCounter idCounter,
			final List<ClassObjectModel> models, final List<ApiGroupModel> restGroups,
			final Map<ClassModel, String> modelNodeIds, final Map<ApiGroupModel, String> restNodeIds,
			final AnalyzeApi api) {

		// Inheritance edges (child → parent, arrow points UP to parent)
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
						cells.append(mxEdge(edgeId, sourceId, targetId, STYLE_RELATION, field.name()));
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
				// Find the target model by class
				final String targetId = findModelNodeIdByClass(fk.target(), modelNodeIds);
				if (targetId != null && !sourceId.equals(targetId)) {
					final String edgeId = idCounter.next();
					cells.append(mxEdge(edgeId, sourceId, targetId, STYLE_FOREIGN_KEY, field.name()));
				}
			}
		}

		// Field type association edges (non-primitive field types)
		for (final ClassObjectModel model : models) {
			final String sourceId = modelNodeIds.get(model);
			for (final FieldProperty field : model.getFields()) {
				// Skip if already handled by linkClass or checkForeignKey
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
						cells.append(mxEdge(edgeId, sourceId, targetId, STYLE_ASSOCIATION, field.name()));
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
				// Return types
				for (final ClassModel returnModel : endpoint.returnTypes) {
					final ClassModel leaf = resolveLeafModel(returnModel);
					if (leaf != null && modelNodeIds.containsKey(leaf)) {
						final String targetId = modelNodeIds.get(leaf);
						final String edgeId = idCounter.next();
						cells.append(mxEdge(edgeId, sourceId, targetId, STYLE_REST_LINK, null));
					}
				}
				// Request body (unnamed element)
				for (final ParameterClassModelList param : endpoint.unnamedElement) {
					for (final ClassModel bodyModel : param.models()) {
						final ClassModel leaf = resolveLeafModel(bodyModel);
						if (leaf != null && modelNodeIds.containsKey(leaf)) {
							final String targetId = modelNodeIds.get(leaf);
							final String edgeId = idCounter.next();
							cells.append(mxEdge(edgeId, sourceId, targetId, STYLE_REST_LINK, null));
						}
					}
				}
			}
		}
	}

	/**
	 * Find a model node ID by its origin Java class.
	 */
	private static String findModelNodeIdByClass(final Class<?> targetClass,
			final Map<ClassModel, String> modelNodeIds) {
		for (final Map.Entry<ClassModel, String> entry : modelNodeIds.entrySet()) {
			if (entry.getKey().getOriginClasses() == targetClass) {
				return entry.getValue();
			}
		}
		return null;
	}

	// ========== LAYOUT ==========

	/**
	 * Main layout algorithm. Places elements as clusters (model tree + associated REST).
	 * <ol>
	 *   <li>Build inheritance trees and place them as tree sub-layouts</li>
	 *   <li>Order trees by connectivity (most-connected first)</li>
	 *   <li>Place REST groups below their associated model tree (same cluster)</li>
	 *   <li>Place enums near the models that use them</li>
	 * </ol>
	 */
	private static void layoutElements(final List<ApiGroupModel> restGroups,
			final List<ClassObjectModel> objectModels, final List<ClassEnumModel> enumModels,
			final Map<Object, int[]> dimensions, final Map<Object, int[]> positions) {

		// Step 1: Build inheritance forest (trees of parent→children)
		final List<InheritanceTree> forest = buildInheritanceForest(objectModels);

		// Step 2: Compute connectivity score for each tree
		final Map<InheritanceTree, Integer> treeConnectivity = new HashMap<>();
		for (final InheritanceTree tree : forest) {
			int score = 0;
			for (final ClassObjectModel model : tree.allModels()) {
				score += countModelReferences(model, objectModels, restGroups);
			}
			treeConnectivity.put(tree, score);
		}
		forest.sort((final InheritanceTree a, final InheritanceTree b) -> treeConnectivity.getOrDefault(b, 0)
				- treeConnectivity.getOrDefault(a, 0));

		// Step 3: Associate REST groups with their primary model's tree
		final Map<InheritanceTree, List<ApiGroupModel>> treeRestGroups = new LinkedHashMap<>();
		final List<ApiGroupModel> unassociatedRest = new ArrayList<>();
		final Set<ClassObjectModel> objectModelSet = new HashSet<>(objectModels);
		for (final InheritanceTree tree : forest) {
			treeRestGroups.put(tree, new ArrayList<>());
		}
		for (final ApiGroupModel group : restGroups) {
			final ClassObjectModel primaryModel = findPrimaryModel(group, objectModels);
			boolean associated = false;
			if (primaryModel != null) {
				for (final InheritanceTree tree : forest) {
					if (tree.allModels().contains(primaryModel)) {
						treeRestGroups.get(tree).add(group);
						associated = true;
						break;
					}
				}
			}
			if (!associated) {
				unassociatedRest.add(group);
			}
		}

		// Step 4: Place clusters (tree + REST below it)
		int clusterX = INITIAL_X;
		int maxEndX = INITIAL_X;

		for (final InheritanceTree tree : forest) {
			// Place the inheritance tree
			final int treeMaxY = placeInheritanceTree(tree, tree.root, clusterX, INITIAL_Y, dimensions, positions);

			// Place associated REST groups below the tree
			int restY = treeMaxY + ROW_SPACING;
			final List<ApiGroupModel> associatedRest = treeRestGroups.get(tree);
			for (final ApiGroupModel group : associatedRest) {
				final int[] dim = dimensions.get(group);
				positions.put(group, new int[] { clusterX, restY });
				restY += dim[1] + ROW_SPACING;
			}

			final int treeWidth = computeTreeWidth(tree, tree.root, dimensions);
			// Account for REST width too
			int clusterWidth = treeWidth;
			for (final ApiGroupModel group : associatedRest) {
				final int[] dim = dimensions.get(group);
				if (dim[0] > clusterWidth) {
					clusterWidth = dim[0];
				}
			}
			if (clusterX + clusterWidth > maxEndX) {
				maxEndX = clusterX + clusterWidth;
			}
			clusterX += clusterWidth + COLUMN_SPACING;
		}

		// Step 5: Place unassociated REST groups at the end
		int unassocRestY = INITIAL_Y;
		for (final ApiGroupModel group : unassociatedRest) {
			final int[] dim = dimensions.get(group);
			positions.put(group, new int[] { clusterX, unassocRestY });
			unassocRestY += dim[1] + ROW_SPACING;
		}

		// Step 6: Place enums near the models that reference them
		final int enumFallbackX = unassociatedRest.isEmpty() ? clusterX : clusterX + COLUMN_SPACING;
		placeEnumsNearModels(enumModels, objectModels, dimensions, positions, enumFallbackX, INITIAL_X);
	}

	// ---------- Inheritance tree data structure ----------

	private static class InheritanceTree {
		final ClassObjectModel root;
		final Map<ClassObjectModel, List<ClassObjectModel>> childrenMap = new LinkedHashMap<>();

		InheritanceTree(final ClassObjectModel root) {
			this.root = root;
		}

		void addChild(final ClassObjectModel parent, final ClassObjectModel child) {
			this.childrenMap.computeIfAbsent(parent, (final ClassObjectModel k) -> new ArrayList<>()).add(child);
		}

		List<ClassObjectModel> getChildren(final ClassObjectModel model) {
			return this.childrenMap.getOrDefault(model, List.of());
		}

		/** Returns all models in this tree via BFS. */
		List<ClassObjectModel> allModels() {
			final List<ClassObjectModel> result = new ArrayList<>();
			final LinkedList<ClassObjectModel> queue = new LinkedList<>();
			queue.add(this.root);
			while (!queue.isEmpty()) {
				final ClassObjectModel model = queue.poll();
				result.add(model);
				queue.addAll(getChildren(model));
			}
			return result;
		}
	}

	/**
	 * Build a forest of inheritance trees from the flat list of models.
	 * Models without a parent (or whose parent is not in the list) become tree roots.
	 */
	private static List<InheritanceTree> buildInheritanceForest(final List<ClassObjectModel> models) {
		final Set<ClassObjectModel> modelSet = new HashSet<>(models);
		final Map<ClassObjectModel, ClassObjectModel> parentMap = new HashMap<>();
		final Map<ClassObjectModel, InheritanceTree> rootToTree = new LinkedHashMap<>();

		// Build parent-child relationships
		for (final ClassObjectModel model : models) {
			final ClassModel extendsClass = model.getExtendsClass();
			if (extendsClass instanceof ClassObjectModel) {
				final ClassObjectModel parent = (ClassObjectModel) extendsClass;
				if (modelSet.contains(parent)) {
					parentMap.put(model, parent);
				}
			}
		}

		// Find roots (models with no parent in the set)
		for (final ClassObjectModel model : models) {
			if (!parentMap.containsKey(model)) {
				rootToTree.put(model, new InheritanceTree(model));
			}
		}

		// Build trees
		for (final Map.Entry<ClassObjectModel, ClassObjectModel> entry : parentMap.entrySet()) {
			final ClassObjectModel child = entry.getKey();
			final ClassObjectModel parent = entry.getValue();
			// Find the root of this parent
			ClassObjectModel root = parent;
			while (parentMap.containsKey(root)) {
				root = parentMap.get(root);
			}
			final InheritanceTree tree = rootToTree.get(root);
			if (tree != null) {
				tree.addChild(parent, child);
			}
		}

		return new ArrayList<>(rootToTree.values());
	}

	// ---------- Tree layout ----------

	/**
	 * Place an inheritance tree: parent centered above its children.
	 * Returns the max Y used by this subtree.
	 */
	private static int placeInheritanceTree(final InheritanceTree tree, final ClassObjectModel model,
			final int subtreeX, final int y, final Map<Object, int[]> dimensions,
			final Map<Object, int[]> positions) {
		final int[] dim = dimensions.get(model);
		if (dim == null) {
			return y;
		}
		final List<ClassObjectModel> children = tree.getChildren(model);

		if (children.isEmpty()) {
			// Leaf node: just place it
			positions.put(model, new int[] { subtreeX, y });
			return y + dim[1];
		}

		// Recursively place children side-by-side below
		final int childrenY = y + dim[1] + ROW_SPACING;
		int childX = subtreeX;
		int maxChildY = childrenY;
		final List<int[]> childPositions = new ArrayList<>();

		for (final ClassObjectModel child : children) {
			final int childMaxY = placeInheritanceTree(tree, child, childX, childrenY, dimensions, positions);
			final int childTreeWidth = computeTreeWidth(tree, child, dimensions);
			childPositions.add(new int[] { childX, childTreeWidth });
			childX += childTreeWidth + ROW_SPACING;
			if (childMaxY > maxChildY) {
				maxChildY = childMaxY;
			}
		}

		// Center parent above children span
		final int childrenSpanStart = childPositions.get(0)[0];
		final int lastChild = childPositions.get(childPositions.size() - 1)[0];
		final int lastChildWidth = childPositions.get(childPositions.size() - 1)[1];
		final int childrenSpanEnd = lastChild + lastChildWidth;
		final int parentX = childrenSpanStart + (childrenSpanEnd - childrenSpanStart - dim[0]) / 2;

		positions.put(model, new int[] { Math.max(subtreeX, parentX), y });
		return maxChildY;
	}

	/**
	 * Compute the total width of a subtree rooted at the given model.
	 */
	private static int computeTreeWidth(final InheritanceTree tree, final ClassObjectModel model,
			final Map<Object, int[]> dimensions) {
		final int[] dim = dimensions.get(model);
		if (dim == null) {
			return 0;
		}
		final List<ClassObjectModel> children = tree.getChildren(model);
		if (children.isEmpty()) {
			return dim[0];
		}
		int childrenWidth = 0;
		for (int i = 0; i < children.size(); i++) {
			if (i > 0) {
				childrenWidth += ROW_SPACING;
			}
			childrenWidth += computeTreeWidth(tree, children.get(i), dimensions);
		}
		return Math.max(dim[0], childrenWidth);
	}

	// ---------- Enum placement ----------

	/**
	 * Place enums near the models that reference them.
	 * If an enum is referenced by a model, place it to the right of that model.
	 * Unreferenced enums go in a column at the far right.
	 */
	private static void placeEnumsNearModels(final List<ClassEnumModel> enumModels,
			final List<ClassObjectModel> objectModels, final Map<Object, int[]> dimensions,
			final Map<Object, int[]> positions, final int fallbackX, final int modelsStartX) {
		final Set<ClassEnumModel> placed = new HashSet<>();

		// For each enum, find the first model that references it
		for (final ClassEnumModel enumModel : enumModels) {
			ClassObjectModel bestReferencer = null;
			for (final ClassObjectModel objModel : objectModels) {
				for (final FieldProperty field : objModel.getFields()) {
					final ClassModel leaf = resolveLeafModel(field.model());
					if (leaf == enumModel) {
						bestReferencer = objModel;
						break;
					}
				}
				if (bestReferencer != null) {
					break;
				}
			}
			if (bestReferencer != null && positions.containsKey(bestReferencer)) {
				// Place enum to the right of the referencing model
				final int[] refPos = positions.get(bestReferencer);
				final int[] refDim = dimensions.get(bestReferencer);
				final int[] enumDim = dimensions.get(enumModel);
				final int enumX = refPos[0] + refDim[0] + COLUMN_SPACING / 2;
				final int enumY = refPos[1];
				// Check for overlap with existing positions and shift down if needed
				final int adjustedY = findNonOverlappingY(enumX, enumY, enumDim, positions, dimensions);
				positions.put(enumModel, new int[] { enumX, adjustedY });
				placed.add(enumModel);
			}
		}

		// Place remaining enums in a fallback column
		int fallbackY = INITIAL_Y;
		for (final ClassEnumModel enumModel : enumModels) {
			if (!placed.contains(enumModel)) {
				final int[] dim = dimensions.get(enumModel);
				positions.put(enumModel, new int[] { fallbackX, fallbackY });
				fallbackY += dim[1] + ROW_SPACING;
			}
		}
	}

	/**
	 * Find a Y position that doesn't overlap with existing positioned elements.
	 */
	private static int findNonOverlappingY(final int x, final int startY, final int[] dim,
			final Map<Object, int[]> positions, final Map<Object, int[]> dimensions) {
		int y = startY;
		boolean collision = true;
		while (collision) {
			collision = false;
			for (final Map.Entry<Object, int[]> entry : positions.entrySet()) {
				final int[] existingPos = entry.getValue();
				final int[] existingDim = dimensions.get(entry.getKey());
				if (existingDim == null) {
					continue;
				}
				// Check horizontal overlap
				if (x < existingPos[0] + existingDim[0] && x + dim[0] > existingPos[0]) {
					// Check vertical overlap
					if (y < existingPos[1] + existingDim[1] + ROW_SPACING / 2
							&& y + dim[1] > existingPos[1] - ROW_SPACING / 2) {
						y = existingPos[1] + existingDim[1] + ROW_SPACING;
						collision = true;
						break;
					}
				}
			}
		}
		return y;
	}

	/**
	 * Find the model most referenced by a REST group (via return types and request bodies).
	 */
	private static ClassObjectModel findPrimaryModel(final ApiGroupModel group,
			final List<ClassObjectModel> objectModels) {
		final Map<ClassObjectModel, Integer> refCount = new HashMap<>();
		final Set<ClassObjectModel> objectModelSet = new HashSet<>(objectModels);

		for (final ApiModel endpoint : group.interfaces) {
			for (final ClassModel returnModel : endpoint.returnTypes) {
				final ClassModel leaf = resolveLeafModel(returnModel);
				if (leaf instanceof ClassObjectModel && objectModelSet.contains(leaf)) {
					refCount.merge((ClassObjectModel) leaf, 1, Integer::sum);
				}
			}
			for (final ParameterClassModelList param : endpoint.unnamedElement) {
				for (final ClassModel bodyModel : param.models()) {
					final ClassModel leaf = resolveLeafModel(bodyModel);
					if (leaf instanceof ClassObjectModel && objectModelSet.contains(leaf)) {
						refCount.merge((ClassObjectModel) leaf, 1, Integer::sum);
					}
				}
			}
		}

		ClassObjectModel best = null;
		int bestCount = 0;
		for (final Map.Entry<ClassObjectModel, Integer> entry : refCount.entrySet()) {
			if (entry.getValue() > bestCount) {
				bestCount = entry.getValue();
				best = entry.getKey();
			}
		}
		return best;
	}

	// ---------- Helper for connectivity scoring ----------

	/**
	 * Count how many times a model is referenced by other models and REST endpoints.
	 */
	private static int countModelReferences(final ClassObjectModel model,
			final List<ClassObjectModel> allModels, final List<ApiGroupModel> restGroups) {
		int count = 0;
		// Count references from other models' fields
		for (final ClassObjectModel other : allModels) {
			if (other == model) {
				continue;
			}
			for (final FieldProperty field : other.getFields()) {
				final ClassModel leaf = resolveLeafModel(field.model());
				if (leaf == model) {
					count++;
				}
				if (field.linkClass() == model) {
					count++;
				}
			}
		}
		// Count references from REST endpoints
		for (final ApiGroupModel group : restGroups) {
			for (final ApiModel endpoint : group.interfaces) {
				for (final ClassModel returnModel : endpoint.returnTypes) {
					if (resolveLeafModel(returnModel) == model) {
						count++;
					}
				}
				for (final ParameterClassModelList param : endpoint.unnamedElement) {
					for (final ClassModel bodyModel : param.models()) {
						if (resolveLeafModel(bodyModel) == model) {
							count++;
						}
					}
				}
			}
		}
		return count;
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

	private static void topoVisit(final ClassObjectModel model, final Map<ClassObjectModel, Boolean> visited,
			final List<ClassObjectModel> sorted, final Map<ClassModel, ClassObjectModel> modelMap) {
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

	private static String mxCell(final String id, final String value, final String style, final String parent,
			final int x, final int y, final int width, final int height) {
		return "\t\t\t\t<mxCell id=\"" + id + "\" value=\"" + value + "\" style=\"" + style
				+ "\" vertex=\"1\" parent=\"" + parent + "\">\n"
				+ "\t\t\t\t\t<mxGeometry x=\"" + x + "\" y=\"" + y + "\" width=\"" + width + "\" height=\"" + height
				+ "\" as=\"geometry\"/>\n"
				+ "\t\t\t\t</mxCell>\n";
	}

	private static String mxCellChild(final String id, final String parentId, final String value, final String style,
			final int x, final int y, final int width, final int height) {
		return "\t\t\t\t<mxCell id=\"" + id + "\" value=\"" + value + "\" style=\"" + style
				+ "\" vertex=\"1\" parent=\"" + parentId + "\">\n"
				+ "\t\t\t\t\t<mxGeometry x=\"" + x + "\" y=\"" + y + "\" width=\"" + width + "\" height=\"" + height
				+ "\" as=\"geometry\"/>\n"
				+ "\t\t\t\t</mxCell>\n";
	}

	private static String mxEdge(final String id, final String sourceId, final String targetId, final String style,
			final String label) {
		final String value = (label != null && !label.isEmpty()) ? escapeXml(label) : "";
		return "\t\t\t\t<mxCell id=\"" + id + "\" value=\"" + value + "\" style=\"" + style
				+ "\" edge=\"1\" parent=\"1\" source=\"" + sourceId + "\" target=\"" + targetId + "\">\n"
				+ "\t\t\t\t\t<mxGeometry relative=\"1\" as=\"geometry\"/>\n"
				+ "\t\t\t\t</mxCell>\n";
	}

	private static String wrapInDrawio(final String cellsContent, final int dx, final int dy) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<mxfile host=\"archidata\">\n"
				+ "\t<diagram id=\"archidata-diagram\" name=\"Page-1\">\n"
				+ "\t\t<mxGraphModel dx=\"" + dx + "\" dy=\"" + dy
				+ "\" grid=\"1\" gridSize=\"10\" guides=\"1\" tooltips=\"1\" connect=\"1\" arrows=\"1\" fold=\"1\" page=\"1\" pageScale=\"1\" pageWidth=\""
				+ Math.max(1100, dx + 100) + "\" pageHeight=\"" + Math.max(850, dy + 100)
				+ "\" math=\"0\" shadow=\"0\">\n"
				+ "\t\t\t<root>\n"
				+ "\t\t\t\t<mxCell id=\"0\"/>\n"
				+ "\t\t\t\t<mxCell id=\"1\" parent=\"0\"/>\n"
				+ cellsContent
				+ "\t\t\t</root>\n"
				+ "\t\t</mxGraphModel>\n"
				+ "\t</diagram>\n"
				+ "</mxfile>\n";
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
		if (clazz == String.class || clazz == Boolean.class || clazz == boolean.class
				|| clazz == Integer.class || clazz == int.class || clazz == Long.class || clazz == long.class
				|| clazz == Float.class || clazz == float.class || clazz == Double.class || clazz == double.class
				|| clazz == Short.class || clazz == short.class || clazz == Character.class || clazz == char.class
				|| clazz == byte[].class) {
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
		return text.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
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

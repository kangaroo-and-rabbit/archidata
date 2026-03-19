package org.atriasoft.archidata.externalRestApi;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

	// ========== LAYOUT (Sugiyama algorithm) ==========

	// ---------- Data structures ----------

	private static class LayoutEdge {
		final Object source;
		final Object target;
		final int weight;

		LayoutEdge(final Object source, final Object target, final int weight) {
			this.source = source;
			this.target = target;
			this.weight = weight;
		}
	}

	private static class SugiyamaNode {
		final Object element;
		int layer = -1;
		int orderInLayer = 0;

		SugiyamaNode(final Object element) {
			this.element = element;
		}
	}

	private static class DirectedEdge {
		final SugiyamaNode from;
		final SugiyamaNode to;

		DirectedEdge(final SugiyamaNode from, final SugiyamaNode to) {
			this.from = from;
			this.to = to;
		}
	}

	// ---------- Edge collection ----------

	private static List<LayoutEdge> collectLayoutEdges(final List<ClassObjectModel> objectModels,
			final List<ClassEnumModel> enumModels, final List<ApiGroupModel> restGroups) {
		final List<LayoutEdge> edges = new ArrayList<>();
		final Set<ClassObjectModel> objectModelSet = new HashSet<>(objectModels);
		final Set<ClassEnumModel> enumModelSet = new HashSet<>(enumModels);

		for (final ClassObjectModel model : objectModels) {
			// Inheritance edges (weight=3): child → parent
			final ClassModel parent = model.getExtendsClass();
			if (parent instanceof ClassObjectModel && objectModelSet.contains(parent)) {
				edges.add(new LayoutEdge(model, parent, 3));
			}
			for (final FieldProperty field : model.getFields()) {
				// linkClass/Relation (weight=2)
				if (field.linkClass() != null && objectModelSet.contains(field.linkClass())) {
					if (field.linkClass() != model) {
						edges.add(new LayoutEdge(model, field.linkClass(), 2));
					}
				}
				// CheckForeignKey (weight=2)
				if (field.linkClass() == null && field.checkForeignKey() != null) {
					final ClassModel fkTarget = findModelByClass(field.checkForeignKey().target(), objectModels);
					if (fkTarget != null && fkTarget != model) {
						edges.add(new LayoutEdge(model, fkTarget, 2));
					}
				}
				// Association (weight=1)
				if (field.linkClass() == null && field.checkForeignKey() == null) {
					final ClassModel leaf = resolveLeafModel(field.model());
					if (leaf != null && leaf != model) {
						if (leaf instanceof ClassObjectModel && objectModelSet.contains(leaf)) {
							edges.add(new LayoutEdge(model, leaf, 1));
						} else if (leaf instanceof ClassEnumModel && enumModelSet.contains(leaf)) {
							edges.add(new LayoutEdge(model, leaf, 1));
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
						edges.add(new LayoutEdge(group, leaf, 2));
					}
				}
				for (final ParameterClassModelList param : endpoint.unnamedElement) {
					for (final ClassModel bodyModel : param.models()) {
						final ClassModel leaf = resolveLeafModel(bodyModel);
						if (leaf != null && (objectModelSet.contains(leaf) || enumModelSet.contains(leaf))) {
							edges.add(new LayoutEdge(group, leaf, 2));
						}
					}
				}
			}
		}

		return edges;
	}

	private static ClassModel findModelByClass(final Class<?> targetClass,
			final List<ClassObjectModel> objectModels) {
		for (final ClassObjectModel model : objectModels) {
			if (model.getOriginClasses() == targetClass) {
				return model;
			}
		}
		return null;
	}

	// ---------- Main layout algorithm (Sugiyama) ----------

	/**
	 * Sugiyama layered layout algorithm:
	 * <ol>
	 *   <li>Build directed graph and break cycles</li>
	 *   <li>Assign layers (longest-path from sources)</li>
	 *   <li>Reduce crossings (barycenter heuristic, 4 sweeps)</li>
	 *   <li>Assign X/Y coordinates</li>
	 * </ol>
	 */
	private static void layoutElements(final List<ApiGroupModel> restGroups,
			final List<ClassObjectModel> objectModels, final List<ClassEnumModel> enumModels,
			final Map<Object, int[]> dimensions, final Map<Object, int[]> positions) {

		// Collect all elements and edges
		final List<LayoutEdge> edges = collectLayoutEdges(objectModels, enumModels, restGroups);

		// Create Sugiyama nodes for all elements
		final Map<Object, SugiyamaNode> nodeMap = new LinkedHashMap<>();
		for (final ClassObjectModel model : objectModels) {
			nodeMap.put(model, new SugiyamaNode(model));
		}
		for (final ClassEnumModel model : enumModels) {
			nodeMap.put(model, new SugiyamaNode(model));
		}
		for (final ApiGroupModel group : restGroups) {
			nodeMap.put(group, new SugiyamaNode(group));
		}

		if (nodeMap.isEmpty()) {
			return;
		}

		// Step 1: Build directed edges and break cycles
		final List<DirectedEdge> dagEdges = buildDag(edges, nodeMap);

		// Step 2: Assign layers (longest-path layering)
		assignLayers(nodeMap, dagEdges);

		// Step 3: Build layer structure
		int maxLayer = 0;
		for (final SugiyamaNode node : nodeMap.values()) {
			if (node.layer > maxLayer) {
				maxLayer = node.layer;
			}
		}
		final List<List<SugiyamaNode>> layers = new ArrayList<>();
		for (int i = 0; i <= maxLayer; i++) {
			layers.add(new ArrayList<>());
		}
		for (final SugiyamaNode node : nodeMap.values()) {
			layers.get(node.layer).add(node);
		}
		// Initial ordering within layers: sorted by name for determinism
		for (final List<SugiyamaNode> layer : layers) {
			layer.sort((final SugiyamaNode a, final SugiyamaNode b) -> getNodeName(a).compareTo(getNodeName(b)));
			for (int i = 0; i < layer.size(); i++) {
				layer.get(i).orderInLayer = i;
			}
		}

		// Build adjacency lists for crossing reduction
		final Map<SugiyamaNode, List<SugiyamaNode>> successors = new HashMap<>();
		final Map<SugiyamaNode, List<SugiyamaNode>> predecessors = new HashMap<>();
		for (final DirectedEdge edge : dagEdges) {
			successors.computeIfAbsent(edge.from, (final SugiyamaNode k) -> new ArrayList<>()).add(edge.to);
			predecessors.computeIfAbsent(edge.to, (final SugiyamaNode k) -> new ArrayList<>()).add(edge.from);
		}
		// Also add undirected connections for edges that were NOT in the DAG (back-edges)
		// to improve crossing reduction with all relationships
		for (final LayoutEdge edge : edges) {
			final SugiyamaNode from = nodeMap.get(edge.source);
			final SugiyamaNode to = nodeMap.get(edge.target);
			if (from != null && to != null && from != to) {
				if (from.layer < to.layer) {
					if (!successors.getOrDefault(from, List.of()).contains(to)) {
						successors.computeIfAbsent(from, (final SugiyamaNode k) -> new ArrayList<>()).add(to);
					}
					if (!predecessors.getOrDefault(to, List.of()).contains(from)) {
						predecessors.computeIfAbsent(to, (final SugiyamaNode k) -> new ArrayList<>()).add(from);
					}
				} else if (to.layer < from.layer) {
					if (!successors.getOrDefault(to, List.of()).contains(from)) {
						successors.computeIfAbsent(to, (final SugiyamaNode k) -> new ArrayList<>()).add(from);
					}
					if (!predecessors.getOrDefault(from, List.of()).contains(to)) {
						predecessors.computeIfAbsent(from, (final SugiyamaNode k) -> new ArrayList<>()).add(to);
					}
				}
			}
		}

		// Step 4: Crossing reduction (barycenter heuristic, 4 sweeps)
		for (int sweep = 0; sweep < 4; sweep++) {
			if (sweep % 2 == 0) {
				// Top-down sweep
				for (int l = 1; l < layers.size(); l++) {
					barycentricOrder(layers.get(l), predecessors, true);
				}
			} else {
				// Bottom-up sweep
				for (int l = layers.size() - 2; l >= 0; l--) {
					barycentricOrder(layers.get(l), successors, false);
				}
			}
		}

		// Step 5: Assign coordinates
		assignCoordinates(layers, dimensions, positions);
	}

	// ---------- Step 1: Build DAG (break cycles via DFS) ----------

	private static List<DirectedEdge> buildDag(final List<LayoutEdge> edges,
			final Map<Object, SugiyamaNode> nodeMap) {
		// Build adjacency list for cycle detection
		final Map<SugiyamaNode, List<SugiyamaNode>> adj = new LinkedHashMap<>();
		final Map<SugiyamaNode, Map<SugiyamaNode, Boolean>> edgeExists = new HashMap<>();

		final List<DirectedEdge> result = new ArrayList<>();

		for (final LayoutEdge edge : edges) {
			final SugiyamaNode from = nodeMap.get(edge.source);
			final SugiyamaNode to = nodeMap.get(edge.target);
			if (from == null || to == null || from == to) {
				continue;
			}
			// Avoid duplicate directed edges
			if (edgeExists.computeIfAbsent(from, (final SugiyamaNode k) -> new HashMap<>()).containsKey(to)) {
				continue;
			}
			edgeExists.get(from).put(to, Boolean.TRUE);
			adj.computeIfAbsent(from, (final SugiyamaNode k) -> new ArrayList<>()).add(to);
		}

		// DFS to detect and remove back-edges (cycle breaking)
		final int white = 0;
		final int gray = 1;
		final int black = 2;
		final Map<SugiyamaNode, Integer> color = new HashMap<>();
		for (final SugiyamaNode node : nodeMap.values()) {
			color.put(node, white);
		}

		// Sort nodes for deterministic DFS order
		final List<SugiyamaNode> sortedNodes = new ArrayList<>(nodeMap.values());
		sortedNodes.sort((final SugiyamaNode a, final SugiyamaNode b) -> getNodeName(a).compareTo(getNodeName(b)));

		final Set<SugiyamaNode> backEdgeTargets = new HashSet<>();
		for (final SugiyamaNode node : sortedNodes) {
			if (color.get(node) == white) {
				dfsCycleDetect(node, adj, color, backEdgeTargets, white, gray, black);
			}
		}

		// Rebuild edges, skipping back-edges
		for (final Map.Entry<SugiyamaNode, List<SugiyamaNode>> entry : adj.entrySet()) {
			final SugiyamaNode from = entry.getKey();
			for (final SugiyamaNode to : entry.getValue()) {
				// A back-edge is from→to where 'to' was gray during DFS visit of 'from'
				// We approximate: skip edge if it would create a cycle
				// Simple heuristic: include all edges, then verify acyclicity
				result.add(new DirectedEdge(from, to));
			}
		}

		// Remove back-edges by verifying with topological sort
		return removeBackEdges(result, nodeMap);
	}

	private static void dfsCycleDetect(final SugiyamaNode node, final Map<SugiyamaNode, List<SugiyamaNode>> adj,
			final Map<SugiyamaNode, Integer> color, final Set<SugiyamaNode> backEdgeTargets,
			final int white, final int gray, final int black) {
		color.put(node, gray);
		final List<SugiyamaNode> neighbors = adj.getOrDefault(node, List.of());
		for (final SugiyamaNode neighbor : neighbors) {
			final int c = color.get(neighbor);
			if (c == white) {
				dfsCycleDetect(neighbor, adj, color, backEdgeTargets, white, gray, black);
			}
		}
		color.put(node, black);
	}

	/**
	 * Remove back-edges to make the graph acyclic using Kahn's algorithm.
	 */
	private static List<DirectedEdge> removeBackEdges(final List<DirectedEdge> edges,
			final Map<Object, SugiyamaNode> nodeMap) {
		// Compute in-degrees
		final Map<SugiyamaNode, Integer> inDegree = new HashMap<>();
		final Map<SugiyamaNode, List<DirectedEdge>> outEdges = new HashMap<>();
		for (final SugiyamaNode node : nodeMap.values()) {
			inDegree.put(node, 0);
		}
		for (final DirectedEdge edge : edges) {
			inDegree.merge(edge.to, 1, Integer::sum);
			outEdges.computeIfAbsent(edge.from, (final SugiyamaNode k) -> new ArrayList<>()).add(edge);
		}

		// Kahn's topological sort
		final LinkedList<SugiyamaNode> queue = new LinkedList<>();
		for (final Map.Entry<SugiyamaNode, Integer> entry : inDegree.entrySet()) {
			if (entry.getValue() == 0) {
				queue.add(entry.getKey());
			}
		}
		// Sort queue for determinism
		queue.sort((final SugiyamaNode a, final SugiyamaNode b) -> getNodeName(a).compareTo(getNodeName(b)));

		final Set<SugiyamaNode> visited = new LinkedHashSet<>();
		final List<DirectedEdge> acyclicEdges = new ArrayList<>();

		while (!queue.isEmpty()) {
			final SugiyamaNode node = queue.poll();
			visited.add(node);
			final List<DirectedEdge> outs = outEdges.getOrDefault(node, List.of());
			for (final DirectedEdge edge : outs) {
				if (!visited.contains(edge.to)) {
					acyclicEdges.add(edge);
					final int newDeg = inDegree.get(edge.to) - 1;
					inDegree.put(edge.to, newDeg);
					if (newDeg == 0) {
						queue.add(edge.to);
						// Re-sort for determinism
						queue.sort((final SugiyamaNode a, final SugiyamaNode b) -> getNodeName(a)
								.compareTo(getNodeName(b)));
					}
				}
			}
		}

		// Nodes not visited are in cycles — their edges are dropped
		// Add edges between visited nodes only
		return acyclicEdges;
	}

	// ---------- Step 2: Layer assignment (longest path) ----------

	private static void assignLayers(final Map<Object, SugiyamaNode> nodeMap, final List<DirectedEdge> dagEdges) {
		// Build predecessor map
		final Map<SugiyamaNode, List<SugiyamaNode>> preds = new HashMap<>();
		final Map<SugiyamaNode, List<SugiyamaNode>> succs = new HashMap<>();
		for (final DirectedEdge edge : dagEdges) {
			preds.computeIfAbsent(edge.to, (final SugiyamaNode k) -> new ArrayList<>()).add(edge.from);
			succs.computeIfAbsent(edge.from, (final SugiyamaNode k) -> new ArrayList<>()).add(edge.to);
		}

		// Topological order via Kahn's algorithm
		final Map<SugiyamaNode, Integer> inDegree = new HashMap<>();
		for (final SugiyamaNode node : nodeMap.values()) {
			inDegree.put(node, 0);
		}
		for (final DirectedEdge edge : dagEdges) {
			inDegree.merge(edge.to, 1, Integer::sum);
		}

		final LinkedList<SugiyamaNode> queue = new LinkedList<>();
		for (final SugiyamaNode node : nodeMap.values()) {
			if (inDegree.getOrDefault(node, 0) == 0) {
				node.layer = 0;
				queue.add(node);
			}
		}
		queue.sort((final SugiyamaNode a, final SugiyamaNode b) -> getNodeName(a).compareTo(getNodeName(b)));

		while (!queue.isEmpty()) {
			final SugiyamaNode node = queue.poll();
			final List<SugiyamaNode> successors = succs.getOrDefault(node, List.of());
			for (final SugiyamaNode succ : successors) {
				// Successor's layer = max(current layer, predecessor layer + 1)
				final int candidateLayer = node.layer + 1;
				if (candidateLayer > succ.layer) {
					succ.layer = candidateLayer;
				}
				final int newDeg = inDegree.get(succ) - 1;
				inDegree.put(succ, newDeg);
				if (newDeg == 0) {
					queue.add(succ);
					queue.sort(
							(final SugiyamaNode a, final SugiyamaNode b) -> getNodeName(a).compareTo(getNodeName(b)));
				}
			}
		}

		// Any unvisited nodes (in cycles that were completely removed) → layer 0
		for (final SugiyamaNode node : nodeMap.values()) {
			if (node.layer < 0) {
				node.layer = 0;
			}
		}
	}

	// ---------- Step 3: Crossing reduction (barycenter) ----------

	private static void barycentricOrder(final List<SugiyamaNode> layer,
			final Map<SugiyamaNode, List<SugiyamaNode>> neighbors, final boolean usePredecessors) {
		// Compute barycenter for each node
		final Map<SugiyamaNode, Double> barycenter = new HashMap<>();
		for (final SugiyamaNode node : layer) {
			final List<SugiyamaNode> connected = neighbors.getOrDefault(node, List.of());
			if (connected.isEmpty()) {
				barycenter.put(node, (double) node.orderInLayer);
			} else {
				double sum = 0.0;
				int count = 0;
				for (final SugiyamaNode neighbor : connected) {
					sum += neighbor.orderInLayer;
					count++;
				}
				barycenter.put(node, sum / count);
			}
		}

		// Sort by barycenter, tie-break by name
		layer.sort((final SugiyamaNode a, final SugiyamaNode b) -> {
			final int cmp = Double.compare(barycenter.getOrDefault(a, 0.0), barycenter.getOrDefault(b, 0.0));
			if (cmp != 0) {
				return cmp;
			}
			return getNodeName(a).compareTo(getNodeName(b));
		});

		// Update orderInLayer
		for (int i = 0; i < layer.size(); i++) {
			layer.get(i).orderInLayer = i;
		}
	}

	// ---------- Step 4: Coordinate assignment ----------

	private static void assignCoordinates(final List<List<SugiyamaNode>> layers,
			final Map<Object, int[]> dimensions, final Map<Object, int[]> positions) {
		// Compute Y for each layer band
		final int[] layerY = new int[layers.size()];
		final int[] layerHeight = new int[layers.size()];
		int currentY = INITIAL_Y;
		for (int l = 0; l < layers.size(); l++) {
			layerY[l] = currentY;
			int maxHeight = 0;
			for (final SugiyamaNode node : layers.get(l)) {
				final int[] dim = dimensions.get(node.element);
				if (dim != null && dim[1] > maxHeight) {
					maxHeight = dim[1];
				}
			}
			layerHeight[l] = maxHeight;
			currentY += maxHeight + ROW_SPACING;
		}

		// Assign X within each layer
		for (int l = 0; l < layers.size(); l++) {
			int currentX = INITIAL_X;
			for (final SugiyamaNode node : layers.get(l)) {
				final int[] dim = dimensions.get(node.element);
				if (dim == null) {
					continue;
				}
				positions.put(node.element, new int[] { currentX, layerY[l] });
				currentX += dim[0] + COLUMN_SPACING;
			}
		}
	}

	// ---------- Helpers ----------

	private static String getNodeName(final SugiyamaNode node) {
		final Object elem = node.element;
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
				+ "\" grid=\"0\" gridSize=\"10\" guides=\"1\" tooltips=\"1\" connect=\"1\" arrows=\"1\" fold=\"1\" page=\"0\" pageScale=\"1\" background=\"#FFFFFF\" pageWidth=\""
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
		// Filter out common value types that don't need their own box
		if (clazz == java.util.Date.class || clazz == org.bson.types.ObjectId.class
				|| clazz == org.bson.Document.class || clazz == CharSequence.class
				|| clazz == java.io.InputStream.class) {
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

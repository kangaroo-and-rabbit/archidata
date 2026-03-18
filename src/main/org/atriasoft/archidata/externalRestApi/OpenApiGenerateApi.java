package org.atriasoft.archidata.externalRestApi;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Generates an OpenAPI 3.0.3 specification from the {@link AnalyzeApi} introspection data.
 *
 * <p>This generator is independent of the Swagger library — it builds a plain
 * {@code Map<String, Object>} tree that is serialized to JSON via Jackson.
 */
public class OpenApiGenerateApi {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiGenerateApi.class);

	private OpenApiGenerateApi() {
		// Utility class
	}

	/**
	 * Generate an OpenAPI 3.0.3 specification as a JSON string.
	 *
	 * @param api   the analyzed API metadata
	 * @param title the API title
	 * @param version the API version
	 * @return the OpenAPI spec as a JSON string
	 * @throws Exception on generation error
	 */
	public static String generateJson(final AnalyzeApi api, final String title, final String version) throws Exception {
		final Map<String, Object> spec = generate(api, title, version);
		final ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper.writeValueAsString(spec);
	}

	/**
	 * Generate an OpenAPI 3.0.3 specification as a Map tree.
	 *
	 * @param api   the analyzed API metadata
	 * @param title the API title
	 * @param version the API version
	 * @return the OpenAPI spec as a nested Map
	 * @throws Exception on generation error
	 */
	public static Map<String, Object> generate(final AnalyzeApi api, final String title, final String version) throws Exception {
		final Map<String, Object> spec = new LinkedHashMap<>();
		spec.put("openapi", "3.0.3");

		// Info
		final Map<String, Object> info = new LinkedHashMap<>();
		info.put("title", title);
		info.put("version", version);
		spec.put("info", info);

		// Paths
		spec.put("paths", buildPaths(api));

		// Components
		final Map<String, Object> components = new LinkedHashMap<>();
		components.put("schemas", buildSchemas(api));
		// Security schemes (bearer auth for RolesAllowed endpoints)
		if (hasSecuredEndpoints(api)) {
			final Map<String, Object> securitySchemes = new LinkedHashMap<>();
			final Map<String, Object> bearerAuth = new LinkedHashMap<>();
			bearerAuth.put("type", "http");
			bearerAuth.put("scheme", "bearer");
			bearerAuth.put("bearerFormat", "JWT");
			securitySchemes.put("bearerAuth", bearerAuth);
			components.put("securitySchemes", securitySchemes);
		}
		spec.put("components", components);

		return spec;
	}

	// ========== PATHS ==========

	private static Map<String, Object> buildPaths(final AnalyzeApi api) {
		final Map<String, Object> paths = new LinkedHashMap<>();
		for (final ApiGroupModel group : api.getAllApi()) {
			for (final ApiModel endpoint : group.interfaces) {
				final String path = normalizePath(endpoint.restEndPoint);
				@SuppressWarnings("unchecked")
				Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
				if (pathItem == null) {
					pathItem = new LinkedHashMap<>();
					paths.put(path, pathItem);
				}
				final String httpMethod = toHttpMethod(endpoint.restTypeRequest);
				if (httpMethod == null) {
					LOGGER.warn("Skipping unsupported REST method: {} for {}", endpoint.restTypeRequest, path);
					continue;
				}
				pathItem.put(httpMethod, buildOperation(endpoint, group.name));
			}
		}
		return paths;
	}

	private static Map<String, Object> buildOperation(final ApiModel endpoint, final String groupName) {
		final Map<String, Object> operation = new LinkedHashMap<>();

		// Tags — use @ApiDoc(group=...) if available, otherwise class name
		final List<String> tags = new ArrayList<>();
		if (endpoint.group != null && !endpoint.group.isEmpty()) {
			tags.add(endpoint.group);
		} else {
			tags.add(groupName);
		}
		operation.put("tags", tags);

		// Operation ID
		operation.put("operationId", endpoint.name);

		// Summary / description
		if (endpoint.description != null && !endpoint.description.isEmpty()) {
			operation.put("summary", endpoint.description);
		}

		// Build description with security roles info
		final StringBuilder descriptionBuilder = new StringBuilder();
		if (endpoint.description != null && !endpoint.description.isEmpty()) {
			descriptionBuilder.append(endpoint.description);
		}
		if (endpoint.securityRoles != null) {
			if (descriptionBuilder.length() > 0) {
				descriptionBuilder.append("\n\n");
			}
			if (endpoint.securityRoles.isEmpty()) {
				descriptionBuilder.append("**Access:** Public (no authentication required)");
			} else {
				descriptionBuilder.append("**Access:** Requires role(s): ");
				descriptionBuilder.append(String.join(", ", endpoint.securityRoles));
			}
		}
		if (descriptionBuilder.length() > 0) {
			operation.put("description", descriptionBuilder.toString());
		}

		// Parameters (path, query, header)
		final List<Map<String, Object>> parameters = new ArrayList<>();
		for (final Map.Entry<String, ParameterClassModelList> entry : endpoint.parameters.entrySet()) {
			parameters.add(buildParameter(entry.getKey(), "path", entry.getValue(), true));
		}
		for (final Map.Entry<String, ParameterClassModelList> entry : endpoint.queries.entrySet()) {
			parameters.add(buildParameter(entry.getKey(), "query", entry.getValue(), !entry.getValue().optional()));
		}
		for (final Map.Entry<String, ParameterClassModelList> entry : endpoint.headers.entrySet()) {
			parameters.add(buildParameter(entry.getKey(), "header", entry.getValue(), !entry.getValue().optional()));
		}
		if (!parameters.isEmpty()) {
			operation.put("parameters", parameters);
		}

		// Request body
		if (!endpoint.unnamedElement.isEmpty()) {
			operation.put("requestBody", buildRequestBody(endpoint));
		} else if (!endpoint.multiPartParameters.isEmpty()) {
			operation.put("requestBody", buildMultiPartRequestBody(endpoint));
		}

		// Responses
		operation.put("responses", buildResponses(endpoint));

		// Security
		if (endpoint.securityRoles != null) {
			if (endpoint.securityRoles.isEmpty()) {
				// @PermitAll — no security required
				operation.put("security", new ArrayList<>());
			} else {
				// @RolesAllowed — require bearer auth with specific roles
				final List<Map<String, Object>> security = new ArrayList<>();
				final Map<String, Object> bearerAuth = new LinkedHashMap<>();
				bearerAuth.put("bearerAuth", endpoint.securityRoles);
				security.add(bearerAuth);
				operation.put("security", security);
			}
		}

		return operation;
	}

	private static Map<String, Object> buildParameter(
			final String name,
			final String in,
			final ParameterClassModelList paramList,
			final boolean required) {
		final Map<String, Object> param = new LinkedHashMap<>();
		param.put("name", name);
		param.put("in", in);
		param.put("required", required);
		if (!paramList.models().isEmpty()) {
			param.put("schema", buildSchemaRef(paramList.models().get(0)));
		}
		return param;
	}

	private static Map<String, Object> buildRequestBody(final ApiModel endpoint) {
		final Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("required", true);

		final Map<String, Object> content = new LinkedHashMap<>();
		if (endpoint.consumes != null) {
			for (final String mediaType : endpoint.consumes) {
				final Map<String, Object> mediaContent = new LinkedHashMap<>();
				if (!endpoint.unnamedElement.isEmpty()) {
					final ParameterClassModelList body = endpoint.unnamedElement.get(0);
					if (!body.models().isEmpty()) {
						mediaContent.put("schema", buildSchemaRefWithGroups(body));
					}
				}
				content.put(mediaType, mediaContent);
			}
		}
		if (content.isEmpty()) {
			final Map<String, Object> mediaContent = new LinkedHashMap<>();
			if (!endpoint.unnamedElement.isEmpty()) {
				final ParameterClassModelList body = endpoint.unnamedElement.get(0);
				if (!body.models().isEmpty()) {
					mediaContent.put("schema", buildSchemaRefWithGroups(body));
				}
			}
			content.put("application/json", mediaContent);
		}
		requestBody.put("content", content);
		return requestBody;
	}

	private static Map<String, Object> buildMultiPartRequestBody(final ApiModel endpoint) {
		final Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("required", true);

		final Map<String, Object> properties = new LinkedHashMap<>();
		final List<String> requiredFields = new ArrayList<>();
		for (final Map.Entry<String, ParameterClassModelList> entry : endpoint.multiPartParameters.entrySet()) {
			final String partName = entry.getKey();
			final ParameterClassModelList paramList = entry.getValue();
			if (!paramList.models().isEmpty()) {
				final ClassModel model = paramList.models().get(0);
				if (isFileType(model)) {
					final Map<String, Object> fileProp = new LinkedHashMap<>();
					fileProp.put("type", "string");
					fileProp.put("format", "binary");
					properties.put(partName, fileProp);
				} else {
					properties.put(partName, buildSchemaRef(model));
				}
			}
			if (!paramList.optional()) {
				requiredFields.add(partName);
			}
		}

		final Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		if (!requiredFields.isEmpty()) {
			schema.put("required", requiredFields);
		}

		final Map<String, Object> mediaContent = new LinkedHashMap<>();
		mediaContent.put("schema", schema);

		final Map<String, Object> content = new LinkedHashMap<>();
		content.put("multipart/form-data", mediaContent);
		requestBody.put("content", content);
		return requestBody;
	}

	private static Map<String, Object> buildResponses(final ApiModel endpoint) {
		final Map<String, Object> responses = new LinkedHashMap<>();
		final Map<String, Object> response200 = new LinkedHashMap<>();
		response200.put("description", "Successful operation");

		if (!endpoint.returnTypes.isEmpty()) {
			final ClassModel returnModel = endpoint.returnTypes.get(0);
			final Class<?> returnClass = returnModel.getOriginClasses();
			if (returnClass != Void.class && returnClass != void.class) {
				final Map<String, Object> content = new LinkedHashMap<>();
				if (endpoint.produces != null) {
					for (final String mediaType : endpoint.produces) {
						final Map<String, Object> mediaContent = new LinkedHashMap<>();
						mediaContent.put("schema", buildSchemaRef(returnModel));
						content.put(mediaType, mediaContent);
					}
				}
				if (content.isEmpty()) {
					final Map<String, Object> mediaContent = new LinkedHashMap<>();
					mediaContent.put("schema", buildSchemaRef(returnModel));
					content.put("application/json", mediaContent);
				}
				response200.put("content", content);
			}
		}
		responses.put("200", response200);
		return responses;
	}

	// ========== SCHEMAS ==========

	private static Map<String, Object> buildSchemas(final AnalyzeApi api) {
		final Map<String, Object> schemas = new LinkedHashMap<>();
		for (final ClassModel model : api.getAllModel()) {
			if (model instanceof ClassObjectModel) {
				final ClassObjectModel objModel = (ClassObjectModel) model;
				if (objModel.isPrimitive() || isBasicType(objModel.getOriginClasses())) {
					continue;
				}
				final String name = getSchemaName(objModel);
				if (name == null) {
					continue;
				}
				schemas.put(name, buildObjectSchema(objModel));
			} else if (model instanceof ClassEnumModel) {
				final ClassEnumModel enumModel = (ClassEnumModel) model;
				schemas.put(getSimpleName(enumModel), buildEnumSchema(enumModel));
			}
		}
		return schemas;
	}

	private static Map<String, Object> buildObjectSchema(final ClassObjectModel model) {
		final Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");

		if (model.getDescription() != null) {
			schema.put("description", model.getDescription());
		}
		if (model.getExample() != null) {
			schema.put("example", model.getExample());
		}

		final Map<String, Object> properties = new LinkedHashMap<>();
		final List<String> required = new ArrayList<>();
		final Map<String, Object> example = new LinkedHashMap<>();

		for (final FieldProperty field : model.getFields()) {
			final Map<String, Object> prop = buildFieldSchema(field);
			properties.put(field.name(), prop);

			// Required: fields with @NotNull and no @Null
			if (field.annotationNotNull() != null && field.annotationNull() == null) {
				required.add(field.name());
			} else if (field.apiNotNull() != null && field.apiNotNull().value()) {
				required.add(field.name());
			}

			// Build aggregated example
			if (field.example() != null) {
				example.put(field.name(), field.example());
			}
		}

		if (!properties.isEmpty()) {
			schema.put("properties", properties);
		}
		if (!required.isEmpty()) {
			schema.put("required", required);
		}
		if (!example.isEmpty()) {
			schema.put("example", example);
		}

		// Inheritance
		if (model.getExtendsClass() != null) {
			final String parentName = getSchemaName(model.getExtendsClass());
			if (parentName != null) {
				final List<Map<String, Object>> allOf = new ArrayList<>();
				final Map<String, Object> parentRef = new LinkedHashMap<>();
				parentRef.put("$ref", "#/components/schemas/" + parentName);
				allOf.add(parentRef);

				final Map<String, Object> ownProperties = new LinkedHashMap<>();
				ownProperties.put("type", "object");
				if (!properties.isEmpty()) {
					ownProperties.put("properties", properties);
				}
				if (!required.isEmpty()) {
					ownProperties.put("required", required);
				}
				allOf.add(ownProperties);

				schema.clear();
				if (model.getDescription() != null) {
					schema.put("description", model.getDescription());
				}
				schema.put("allOf", allOf);
				if (!example.isEmpty()) {
					schema.put("example", example);
				}
			}
		}

		return schema;
	}

	private static Map<String, Object> buildFieldSchema(final FieldProperty field) {
		final Map<String, Object> prop = buildSchemaForModel(field.model());

		if (field.comment() != null) {
			prop.put("description", field.comment());
		}
		if (field.example() != null) {
			prop.put("example", field.example());
		}
		if (field.nullable() != null && field.nullable()) {
			prop.put("nullable", true);
		}
		if (field.apiReadOnly() != null) {
			prop.put("readOnly", true);
		}

		// String constraints
		if (field.stringSize() != null) {
			if (field.stringSize().min() > 0) {
				prop.put("minLength", field.stringSize().min());
			}
			if (field.stringSize().max() < Integer.MAX_VALUE) {
				prop.put("maxLength", field.stringSize().max());
			}
		}
		if (field.pattern() != null) {
			prop.put("pattern", field.pattern().regexp());
		}
		if (field.email() != null) {
			prop.put("format", "email");
		}

		// Number constraints
		if (field.min() != null) {
			prop.put("minimum", field.min().value());
		}
		if (field.max() != null) {
			prop.put("maximum", field.max().value());
		}
		if (field.decimalMin() != null) {
			prop.put("minimum", Double.parseDouble(field.decimalMin().value()));
			if (!field.decimalMin().inclusive()) {
				prop.put("exclusiveMinimum", true);
			}
		}
		if (field.decimalMax() != null) {
			prop.put("maximum", Double.parseDouble(field.decimalMax().value()));
			if (!field.decimalMax().inclusive()) {
				prop.put("exclusiveMaximum", true);
			}
		}

		return prop;
	}

	private static Map<String, Object> buildEnumSchema(final ClassEnumModel model) {
		final Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "string");
		final List<Object> enumValues = new ArrayList<>(model.getListOfValues().values());
		schema.put("enum", enumValues);
		return schema;
	}

	// ========== SCHEMA REFERENCES ==========

	/**
	 * Build a schema ref that respects @ValidGroup annotations.
	 * If the body has valid=true and groups are set, generates an inline schema
	 * with only the fields compatible with the group (filtering out @Null fields).
	 */
	private static Map<String, Object> buildSchemaRefWithGroups(final ParameterClassModelList body) {
		final ClassModel model = body.models().get(0);
		if (!body.valid() || body.groups() == null || body.groups().length == 0) {
			return buildSchemaRef(model);
		}
		// For group-aware schemas, generate inline filtered schema
		if (model instanceof ClassObjectModel) {
			final ClassObjectModel objModel = (ClassObjectModel) model;
			if (objModel.isPrimitive() || isBasicType(objModel.getOriginClasses())) {
				return buildPrimitiveSchema(objModel.getOriginClasses());
			}
			return buildGroupAwareObjectSchema(objModel, body.valid(), body.groups());
		}
		return buildSchemaRef(model);
	}

	/**
	 * Build an object schema filtered by validation groups.
	 * Fields with @Null for the target group are excluded.
	 * Fields with @NotNull for the target group are marked required.
	 */
	private static Map<String, Object> buildGroupAwareObjectSchema(
			final ClassObjectModel model,
			final boolean valid,
			final Class<?>[] groups) {
		final Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");

		if (model.getDescription() != null) {
			schema.put("description", model.getDescription());
		}

		final Map<String, Object> properties = new LinkedHashMap<>();
		final List<String> required = new ArrayList<>();

		// Collect fields from this model and all parents
		collectGroupAwareFields(model, valid, groups, properties, required);

		if (!properties.isEmpty()) {
			schema.put("properties", properties);
		}
		if (!required.isEmpty()) {
			schema.put("required", required);
		}

		return schema;
	}

	/**
	 * Recursively collect fields from a model and its parent, filtered by validation group.
	 */
	private static void collectGroupAwareFields(
			final ClassObjectModel model,
			final boolean valid,
			final Class<?>[] groups,
			final Map<String, Object> properties,
			final List<String> required) {
		// First collect parent fields
		if (model.getExtendsClass() != null && model.getExtendsClass() instanceof ClassObjectModel) {
			collectGroupAwareFields((ClassObjectModel) model.getExtendsClass(), valid, groups, properties, required);
		}

		for (final FieldProperty field : model.getFields()) {
			if (!isFieldCompatibleWithGroup(field, valid, groups)) {
				continue;
			}
			final Map<String, Object> prop = buildFieldSchema(field);
			properties.put(field.name(), prop);

			if (isFieldRequiredForGroup(field, valid, groups)) {
				required.add(field.name());
			}
		}
	}

	/**
	 * Check if a field should be included for a specific validation group.
	 * A field with @Null(groups={targetGroup}) is excluded.
	 */
	private static boolean isFieldCompatibleWithGroup(
			final FieldProperty field,
			final boolean valid,
			final Class<?>[] groups) {
		if (field.annotationNotNull() != null) {
			if (hasMatchingGroup(field.annotationNotNull().groups(), groups)) {
				return true;
			}
			if ((field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0) && valid) {
				return true;
			}
		}
		if (field.annotationNull() != null) {
			if (hasMatchingGroup(field.annotationNull().groups(), groups)) {
				return false;
			}
			if ((field.annotationNull().groups() == null || field.annotationNull().groups().length == 0) && valid) {
				return false;
			}
		}
		return valid;
	}

	/**
	 * Check if a field is required for a specific validation group.
	 */
	private static boolean isFieldRequiredForGroup(
			final FieldProperty field,
			final boolean valid,
			final Class<?>[] groups) {
		if (field.apiNotNull() != null && field.apiNotNull().value()) {
			return true;
		}
		if (field.annotationNotNull() != null) {
			if (hasMatchingGroup(field.annotationNotNull().groups(), groups)) {
				return true;
			}
			if ((field.annotationNotNull().groups() == null || field.annotationNotNull().groups().length == 0) && valid) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if any group in groupsA matches any group in groupsB.
	 */
	private static boolean hasMatchingGroup(final Class<?>[] groupsA, final Class<?>[] groupsB) {
		if (groupsA == null || groupsB == null) {
			return false;
		}
		for (final Class<?> a : groupsA) {
			for (final Class<?> b : groupsB) {
				if (a == b) {
					return true;
				}
			}
		}
		return false;
	}

	private static Map<String, Object> buildSchemaRef(final ClassModel model) {
		if (model instanceof ClassObjectModel) {
			final ClassObjectModel objModel = (ClassObjectModel) model;
			if (objModel.isPrimitive() || isBasicType(objModel.getOriginClasses())) {
				return buildPrimitiveSchema(objModel.getOriginClasses());
			}
			final String name = getSchemaName(objModel);
			if (name != null) {
				final Map<String, Object> ref = new LinkedHashMap<>();
				ref.put("$ref", "#/components/schemas/" + name);
				return ref;
			}
			return buildPrimitiveSchema(objModel.getOriginClasses());
		}
		if (model instanceof ClassEnumModel) {
			final Map<String, Object> ref = new LinkedHashMap<>();
			ref.put("$ref", "#/components/schemas/" + getSimpleName(model));
			return ref;
		}
		if (model instanceof ClassListModel) {
			final ClassListModel listModel = (ClassListModel) model;
			final Map<String, Object> schema = new LinkedHashMap<>();
			schema.put("type", "array");
			schema.put("items", buildSchemaRef(listModel.valueModel));
			return schema;
		}
		if (model instanceof ClassMapModel) {
			final ClassMapModel mapModel = (ClassMapModel) model;
			final Map<String, Object> schema = new LinkedHashMap<>();
			schema.put("type", "object");
			schema.put("additionalProperties", buildSchemaRef(mapModel.valueModel));
			return schema;
		}
		return buildPrimitiveSchema(model.getOriginClasses());
	}

	private static Map<String, Object> buildSchemaForModel(final ClassModel model) {
		if (model instanceof ClassObjectModel) {
			final ClassObjectModel objModel = (ClassObjectModel) model;
			if (objModel.isPrimitive() || isBasicType(objModel.getOriginClasses())) {
				return buildPrimitiveSchema(objModel.getOriginClasses());
			}
			final String name = getSchemaName(objModel);
			if (name != null) {
				final Map<String, Object> ref = new LinkedHashMap<>();
				ref.put("$ref", "#/components/schemas/" + name);
				return ref;
			}
			return buildPrimitiveSchema(objModel.getOriginClasses());
		}
		if (model instanceof ClassEnumModel) {
			final Map<String, Object> ref = new LinkedHashMap<>();
			ref.put("$ref", "#/components/schemas/" + getSimpleName(model));
			return ref;
		}
		if (model instanceof ClassListModel) {
			final ClassListModel listModel = (ClassListModel) model;
			final Map<String, Object> schema = new LinkedHashMap<>();
			schema.put("type", "array");
			schema.put("items", buildSchemaRef(listModel.valueModel));
			return schema;
		}
		if (model instanceof ClassMapModel) {
			final ClassMapModel mapModel = (ClassMapModel) model;
			final Map<String, Object> schema = new LinkedHashMap<>();
			schema.put("type", "object");
			schema.put("additionalProperties", buildSchemaRef(mapModel.valueModel));
			return schema;
		}
		return buildPrimitiveSchema(model.getOriginClasses());
	}

	// ========== TYPE MAPPING ==========

	private static Map<String, Object> buildPrimitiveSchema(final Class<?> type) {
		final Map<String, Object> schema = new LinkedHashMap<>();
		if (type == null || type == Object.class) {
			schema.put("type", "object");
			return schema;
		}
		if (type == String.class) {
			schema.put("type", "string");
		} else if (type == Boolean.class || type == boolean.class) {
			schema.put("type", "boolean");
		} else if (type == Integer.class || type == int.class || type == Short.class || type == short.class) {
			schema.put("type", "integer");
			schema.put("format", "int32");
		} else if (type == Long.class || type == long.class) {
			schema.put("type", "integer");
			schema.put("format", "int64");
		} else if (type == Float.class || type == float.class) {
			schema.put("type", "number");
			schema.put("format", "float");
		} else if (type == Double.class || type == double.class) {
			schema.put("type", "number");
			schema.put("format", "double");
		} else if (type == UUID.class) {
			schema.put("type", "string");
			schema.put("format", "uuid");
			schema.put("example", "550e8400-e29b-41d4-a716-446655440000");
		} else if (type == ObjectId.class) {
			schema.put("type", "string");
			schema.put("format", "objectid");
			schema.put("description", "MongoDB ObjectId (24 hex characters)");
			schema.put("pattern", "^[a-fA-F0-9]{24}$");
			schema.put("example", "507f1f77bcf86cd799439011");
		} else if (type == Date.class || type == Instant.class) {
			schema.put("type", "string");
			schema.put("format", "date-time");
			schema.put("description", "ISO 8601 date-time (e.g. 2024-01-15T10:30:00.000Z)");
			schema.put("example", "2024-01-15T10:30:00.000Z");
		} else if (type == LocalDate.class) {
			schema.put("type", "string");
			schema.put("format", "date");
			schema.put("description", "ISO 8601 date (e.g. 2024-01-15)");
			schema.put("example", "2024-01-15");
		} else if (type == LocalTime.class) {
			schema.put("type", "string");
			schema.put("format", "time");
			schema.put("description", "ISO 8601 time (e.g. 10:30:00)");
			schema.put("example", "10:30:00");
		} else if (type == byte[].class) {
			schema.put("type", "string");
			schema.put("format", "byte");
			schema.put("description", "Base64-encoded binary data");
		} else if (type == InputStream.class) {
			schema.put("type", "string");
			schema.put("format", "binary");
		} else {
			schema.put("type", "object");
		}
		return schema;
	}

	// ========== HELPERS ==========

	private static String toHttpMethod(final RestTypeRequest restType) {
		switch (restType) {
			case GET:
				return "get";
			case POST:
				return "post";
			case PUT:
				return "put";
			case PATCH:
				return "patch";
			case DELETE:
				return "delete";
			case ARCHIVE:
				return "delete"; // ARCHIVE maps to DELETE in OpenAPI
			case RESTORE:
				return "patch"; // RESTORE maps to PATCH in OpenAPI
			case CALL:
				return "post"; // CALL maps to POST in OpenAPI
			default:
				return null;
		}
	}

	private static String getSchemaName(final ClassModel model) {
		if (model == null || model.getOriginClasses() == null) {
			return null;
		}
		return getSimpleName(model);
	}

	private static String getSimpleName(final ClassModel model) {
		final Class<?> clazz = model.getOriginClasses();
		if (clazz == null) {
			return null;
		}
		final String name = clazz.getSimpleName();
		if (name.isEmpty()) {
			return clazz.getName();
		}
		return name;
	}

	private static boolean isBasicType(final Class<?> type) {
		return type == Void.class || type == void.class || type == String.class
				|| type == Boolean.class || type == boolean.class
				|| type == Integer.class || type == int.class
				|| type == Long.class || type == long.class
				|| type == Float.class || type == float.class
				|| type == Double.class || type == double.class
				|| type == Short.class || type == short.class
				|| type == Character.class || type == char.class
				|| type == Date.class || type == LocalDate.class || type == LocalTime.class || type == Instant.class
				|| type == UUID.class || type == ObjectId.class
				|| type == byte[].class || type == Object.class;
	}

	/**
	 * Normalizes a REST path by collapsing double slashes and removing trailing slashes.
	 */
	private static String normalizePath(final String path) {
		String normalized = path.replaceAll("//+", "/");
		if (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	private static boolean hasSecuredEndpoints(final AnalyzeApi api) {
		for (final ApiGroupModel group : api.getAllApi()) {
			for (final ApiModel endpoint : group.interfaces) {
				if (endpoint.securityRoles != null && !endpoint.securityRoles.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isFileType(final ClassModel model) {
		final Class<?> type = model.getOriginClasses();
		return type == InputStream.class
				|| (type != null && type.getName().contains("FormDataContentDisposition"));
	}
}

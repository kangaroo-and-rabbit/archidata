package test.atriasoft.archidata.externalRestApi;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.annotation.apiGenerator.ApiInputOptional;
import org.atriasoft.archidata.annotation.method.PaginationContext;
import org.atriasoft.archidata.dataAccess.model.Pagination;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import test.atriasoft.archidata.externalRestApi.TestOpenApiGeneration.ItemStatus;

/**
 * Characterization of the whole OpenAPI document for the parts that
 * {@link TestOpenApiGeneration} only walks with {@code containsKey}: a multipart upload, a header
 * parameter, a query list, the collection and map fields of a schema, a paginated answer, a void
 * answer and a text answer.
 */
public class TestOpenApiGenerationWire {

	public static class Slot {
		public String code;
	}

	public static class Shelf {
		public String label;
		public ItemStatus status;
		public List<Slot> slots;
		public List<String> tags;
		public Map<String, Long> counters;
		public Map<String, Slot> slotByName;
	}

	@Path("/shelves")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class ShelfResource {
		@GET
		@Path("{oid}")
		@ApiDoc(description = "Read a shelf", group = "SHELF")
		public Shelf get(
				@PathParam("oid") final ObjectId oid,
				@ApiInputOptional @HeaderParam("X-Trace") final String trace,
				@ApiInputOptional @QueryParam("status") final List<ItemStatus> status) {
			return null;
		}

		@GET
		@Path("page")
		public Pagination<Shelf> page(
				@PaginationContext final org.atriasoft.archidata.dataAccess.model.PaginationContext page) {
			return null;
		}

		@POST
		@Path("{oid}/attachment")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@RolesAllowed("USER")
		public ObjectId attach(
				@PathParam("oid") final ObjectId oid,
				@FormDataParam("file") final InputStream file,
				@FormDataParam("file") final FormDataContentDisposition fileMeta,
				@FormDataParam("title") final String title) {
			return null;
		}

		@GET
		@Path("export")
		@Produces("text/csv")
		public String export() {
			return null;
		}

		@DELETE
		@Path("{oid}")
		public void remove(@PathParam("oid") final ObjectId oid) {}
	}

	private static Map<String, Object> generate() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ShelfResource.class));
		return OpenApiGenerateApi.generate(api, "Shelves", "2.0");
	}

	/** Walks the generated tree the way a JSON pointer would and prints the node as the generator does. */
	private static String node(final String... pointer) throws Exception {
		Object current = generate();
		for (final String key : pointer) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = (Map<String, Object>) current;
			Assertions.assertTrue(map.containsKey(key), "missing " + key + " in " + map.keySet());
			current = map.get(key);
		}
		final ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		final String text = mapper.writeValueAsString(current) + "\n";
		return text;
	}

	@Test
	public void testHeader() throws Exception {
		Assertions.assertEquals("3.0.3", generate().get("openapi"));
		Assertions.assertEquals("""
				{
				  "title" : "Shelves",
				  "version" : "2.0"
				}
				""", node("info"));
	}

	@Test
	public void testReadOperationWithHeaderAndQueryList() throws Exception {
		Assertions.assertEquals("""
				{
				  "tags" : [ "SHELF" ],
				  "operationId" : "ShelfResource_get",
				  "summary" : "Read a shelf",
				  "description" : "Read a shelf",
				  "parameters" : [ {
				    "name" : "oid",
				    "in" : "path",
				    "required" : true,
				    "schema" : {
				      "type" : "string",
				      "format" : "objectid",
				      "description" : "MongoDB ObjectId (24 hex characters)",
				      "pattern" : "^[a-fA-F0-9]{24}$",
				      "example" : "507f1f77bcf86cd799439011"
				    }
				  }, {
				    "name" : "status",
				    "in" : "query",
				    "required" : false,
				    "schema" : {
				      "type" : "array",
				      "items" : {
				        "$ref" : "#/components/schemas/ItemStatus"
				      }
				    }
				  }, {
				    "name" : "X-Trace",
				    "in" : "header",
				    "required" : false,
				    "schema" : {
				      "type" : "string"
				    }
				  } ],
				  "responses" : {
				    "200" : {
				      "description" : "Successful operation",
				      "content" : {
				        "application/json" : {
				          "schema" : {
				            "$ref" : "#/components/schemas/Shelf"
				          }
				        }
				      }
				    }
				  }
				}
				""", node("paths", "/shelves/{oid}", "get"));
	}

	@Test
	public void testMultipartOperation() throws Exception {
		Assertions.assertEquals("""
				{
				  "tags" : [ "ShelfResource" ],
				  "operationId" : "ShelfResource_attach",
				  "description" : "**Access:** Requires role(s): USER",
				  "parameters" : [ {
				    "name" : "oid",
				    "in" : "path",
				    "required" : true,
				    "schema" : {
				      "type" : "string",
				      "format" : "objectid",
				      "description" : "MongoDB ObjectId (24 hex characters)",
				      "pattern" : "^[a-fA-F0-9]{24}$",
				      "example" : "507f1f77bcf86cd799439011"
				    }
				  } ],
				  "requestBody" : {
				    "required" : true,
				    "content" : {
				      "multipart/form-data" : {
				        "schema" : {
				          "type" : "object",
				          "properties" : {
				            "file" : {
				              "type" : "string",
				              "format" : "binary"
				            },
				            "title" : {
				              "type" : "string"
				            }
				          },
				          "required" : [ "file", "title" ]
				        }
				      }
				    }
				  },
				  "responses" : {
				    "200" : {
				      "description" : "Successful operation",
				      "content" : {
				        "application/json" : {
				          "schema" : {
				            "type" : "string",
				            "format" : "objectid",
				            "description" : "MongoDB ObjectId (24 hex characters)",
				            "pattern" : "^[a-fA-F0-9]{24}$",
				            "example" : "507f1f77bcf86cd799439011"
				          }
				        }
				      }
				    }
				  },
				  "security" : [ {
				    "bearerAuth" : [ "USER" ]
				  } ]
				}
				""", node("paths", "/shelves/{oid}/attachment", "post"));
	}

	@Test
	public void testPaginatedOperation() throws Exception {
		// compared as a tree: the schemas of the pagination headers come from Map.of, whose key order changes
		// from one JVM run to the next (see OpenApiGenerateApi.buildPaginationResponseHeaders)
		final com.fasterxml.jackson.databind.ObjectMapper reader = new com.fasterxml.jackson.databind.ObjectMapper();
		final String expected = """
				{
				  "tags" : [ "ShelfResource" ],
				  "operationId" : "ShelfResource_page",
				  "parameters" : [ {
				    "name" : "X-Pagination-Offset",
				    "in" : "header",
				    "required" : false,
				    "description" : "Index of the first item of the page (default 0). Also accepted as a query parameter of the same name, which is how the Link header carries it.",
				    "schema" : {
				      "format" : "int64",
				      "type" : "integer"
				    }
				  }, {
				    "name" : "X-Pagination-Limit",
				    "in" : "header",
				    "required" : false,
				    "description" : "Maximum number of items in the page (server default when absent). Also accepted as a query parameter of the same name.",
				    "schema" : {
				      "format" : "int64",
				      "type" : "integer"
				    }
				  } ],
				  "responses" : {
				    "200" : {
				      "description" : "Successful operation",
				      "content" : {
				        "application/json" : {
				          "schema" : {
				            "type" : "array",
				            "items" : {
				              "$ref" : "#/components/schemas/Shelf"
				            }
				          }
				        }
				      },
				      "headers" : {
				        "X-Total-Count" : {
				          "description" : "Total number of items matching the query, across every page",
				          "schema" : {
				            "format" : "int64",
				            "type" : "integer"
				          }
				        },
				        "Link" : {
				          "description" : "RFC 5988 navigation links: first, prev, next, last",
				          "schema" : {
				            "type" : "string"
				          }
				        }
				      }
				    }
				  }
				}
				""";
		Assertions.assertEquals(reader.readTree(expected), reader.readTree(node("paths", "/shelves/page", "get")));
	}

	@Test
	public void testTextAndVoidOperations() throws Exception {
		Assertions.assertEquals("""
				{
				  "tags" : [ "ShelfResource" ],
				  "operationId" : "ShelfResource_export",
				  "responses" : {
				    "200" : {
				      "description" : "Successful operation",
				      "content" : {
				        "text/csv" : {
				          "schema" : {
				            "type" : "string"
				          }
				        }
				      }
				    }
				  }
				}
				""", node("paths", "/shelves/export", "get"));
		Assertions.assertEquals("""
				{
				  "tags" : [ "ShelfResource" ],
				  "operationId" : "ShelfResource_remove",
				  "parameters" : [ {
				    "name" : "oid",
				    "in" : "path",
				    "required" : true,
				    "schema" : {
				      "type" : "string",
				      "format" : "objectid",
				      "description" : "MongoDB ObjectId (24 hex characters)",
				      "pattern" : "^[a-fA-F0-9]{24}$",
				      "example" : "507f1f77bcf86cd799439011"
				    }
				  } ],
				  "responses" : {
				    "200" : {
				      "description" : "Successful operation"
				    }
				  }
				}
				""", node("paths", "/shelves/{oid}", "delete"));
	}

	@Test
	public void testSchemasOfCollectionsMapsAndEnums() throws Exception {
		Assertions.assertEquals("""
				{
				  "type" : "object",
				  "properties" : {
				    "label" : {
				      "type" : "string",
				      "example" : "Example Title"
				    },
				    "status" : {
				      "$ref" : "#/components/schemas/ItemStatus"
				    },
				    "slots" : {
				      "type" : "array",
				      "items" : {
				        "$ref" : "#/components/schemas/Slot"
				      }
				    },
				    "tags" : {
				      "type" : "array",
				      "items" : {
				        "type" : "string"
				      }
				    },
				    "counters" : {
				      "type" : "object",
				      "additionalProperties" : {
				        "type" : "integer",
				        "format" : "int64"
				      }
				    },
				    "slotByName" : {
				      "type" : "object",
				      "additionalProperties" : {
				        "$ref" : "#/components/schemas/Slot"
				      }
				    }
				  },
				  "example" : {
				    "label" : "Example Title"
				  }
				}
				""", node("components", "schemas", "Shelf"));
		Assertions.assertEquals("""
				{
				  "type" : "object",
				  "properties" : {
				    "code" : {
				      "type" : "string",
				      "example" : "string"
				    }
				  },
				  "example" : {
				    "code" : "string"
				  }
				}
				""", node("components", "schemas", "Slot"));
		Assertions.assertEquals("""
				{
				  "type" : "string",
				  "enum" : [ "ACTIVE", "ARCHIVED", "INACTIVE" ]
				}
				""", node("components", "schemas", "ItemStatus"));
		Assertions.assertEquals("""
				{
				  "bearerAuth" : {
				    "type" : "http",
				    "scheme" : "bearer",
				    "bearerFormat" : "JWT"
				  }
				}
				""", node("components", "securitySchemes"));
	}
}

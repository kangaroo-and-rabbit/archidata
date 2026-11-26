package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.dataAccess.model.Pagination;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.DotGenerateApi;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import test.atriasoft.archidata.externalRestApi.TestDrawioGeneration.CategoryEnum;
import test.atriasoft.archidata.externalRestApi.TestDrawioGeneration.ExtendedEntity;
import test.atriasoft.archidata.externalRestApi.TestDrawioGeneration.Product;
import test.atriasoft.archidata.externalRestApi.TestDrawioGeneration.RelParent;

/**
 * Characterization of the whole text produced by {@link DotGenerateApi}: a resource and models
 * covering a plain field, a list of models, a map, an enum, a parent class, a relation, a path
 * parameter and a query parameter.
 *
 * <p>The body of an endpoint and the {@code List} of enums are pinned by {@link TestDotEnumListAndBody}.
 */
public class TestDotGeneration {

	@ApiDoc(description = "A catalog entry")
	public static class CatalogEntry extends ExtendedEntity {
		@ApiDoc(description = "Title shown to the user")
		public String title;
		public Long count;
		public CategoryEnum category;
		public List<Product> products;
		public List<String> labels;
		public Map<String, Product> productByName;
		public Map<String, Long> counters;
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@jakarta.ws.rs.Path("/catalog/{shop}")
	public static class CatalogResource {
		@GET
		@jakarta.ws.rs.Path("{id}")
		public CatalogEntry get(
				@PathParam("shop") final String shop,
				@PathParam("id") final Long id,
				@QueryParam("lang") final String lang) {
			return null;
		}

		@GET
		public List<Product> list(
				@PathParam("shop") final String shop,
				@QueryParam("category") final CategoryEnum category) {
			return null;
		}

		@GET
		@jakarta.ws.rs.Path("relations")
		public RelParent relations(@PathParam("shop") final String shop) {
			return null;
		}

		@POST
		@jakarta.ws.rs.Path("{id}/touch")
		public void touch(@PathParam("shop") final String shop, @PathParam("id") final Long id) {}
	}

	public static class EmptyModel {}

	public static class NestedHolder {
		public List<List<Product>> matrix;
		public Map<String, List<Product>> groups;
		public EmptyModel empty;
		public ObjectId owner;
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@jakarta.ws.rs.Path("/nested")
	public static class NestedResource {
		@GET
		public NestedHolder get() {
			return null;
		}

		@GET
		@jakarta.ws.rs.Path("index")
		public Map<String, Product> index() {
			return null;
		}

		@GET
		@jakarta.ws.rs.Path("page")
		public Pagination<Product> page(
				@QueryParam("offset") final Long offset,
				@QueryParam("limit") final Long limit) {
			return null;
		}
	}

	private static String generate(final Path dir, final List<Class<?>> resources) throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(resources);
		final Path target = dir.resolve("architecture.dot");
		DotGenerateApi.generateApi(api, target.toString());
		return Files.readString(target);
	}

	@Test
	public void testWholeGraph(@TempDir final Path dir) throws Exception {
		final String graph = generate(dir, List.of(CatalogResource.class));
		Assertions.assertEquals(
				"""
						# Architecture auto-generated file
						digraph UML_Class_diagram {
						    #rankdir=NS;
						\tgraph [
						\t\tpad="0.5"
						\t\tnodesep="1"
						\t\t#ranksep="2"
						\t\tlabel="Rest API server Model"
						\t\tlabelloc="t"
						\t\tfontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"
						\t]
						\tnode [
						\t\tfontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"
						\t\tshape=record
						\t\tstyle=filled
						\t\tfillcolor=gray95
						\t]
						\tedge [fontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"]
						\tCatalogResource [
						\t\tshape=plain
						\t\tlabel=<<table color="#FF3333" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t<tr>
						\t\t\t<td><b>CatalogResource</b><br/>(REST)</td>
						\t\t</tr>
						\t\t<tr>
						\t\t\t<td>
						\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t<tr><td  align="left" port="get"><b> + get(): CatalogEntry</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /catalog/{shop}/{id}</td></tr>
						\t\t\t\t\t<tr><td  align="left" port="list"><b> + list(): List&lt;Product&gt;</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /catalog/{shop}/</td></tr>
						\t\t\t\t\t<tr><td  align="left" port="relations"><b> + relations(): RelParent</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /catalog/{shop}/relations</td></tr>
						\t\t\t\t\t<tr><td  align="left" port="touch"><b> + touch(): void</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /catalog/{shop}/{id}/touch</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tCatalogResource:get:e -> CatalogEntry:NAME:w
						\tCatalogResource:list:e -> Product:NAME:w
						\tCatalogResource:relations:e -> RelParent:NAME:w







						\tCatalogEntry [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>CatalogEntry</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left"><i> // Title shown to the user</i></td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="title"><b> + title</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="count"><b> + count</b>: Long</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="category"><b> + category</b>: CategoryEnum</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="products"><b> + products</b>: List&lt;Product&gt;</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="labels"><b> + labels</b>: List&lt;string&gt;</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="productByName"><b> + productByName</b>: Map&lt;string, Product&gt;</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="counters"><b> + counters</b>: Map&lt;string, Long&gt;</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=empty arrowsize=2]
						\tExtendedEntity:s -> CatalogEntry:n
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tCatalogEntry:category:e -> CategoryEnum:NAME:w
						\tCatalogEntry:products:e -> Product:NAME:w
						\tCatalogEntry:productByName:e -> Product:NAME:w

						\tProduct [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>Product</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="id"><b> + id</b>: Long</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="name"><b> + name</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="price"><b> + price</b>: Double</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="category"><b> + category</b>: CategoryEnum</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tProduct:category:e -> CategoryEnum:NAME:w

						\tCategoryEnum [
						\t\tshape=plain
						\t\tlabel=<<table color="#33FF33" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>CategoryEnum</b><br/>(ENUM)</td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left"><b> + CLOTHING</b> = 'CLOTHING'</td></tr>
						\t\t\t\t\t\t<tr><td  align="left"><b> + ELECTRONICS</b> = 'ELECTRONICS'</td></tr>
						\t\t\t\t\t\t<tr><td  align="left"><b> + FOOD</b> = 'FOOD'</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						\tRelParent [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>RelParent</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="oid"><b> + oid</b>: ObjectId</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="childIds"><b> + childIds</b>: List&lt;ObjectId&gt;</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tRelParent:oid:e -> ObjectId:NAME:w
						\tRelParent:childIds:e -> ObjectId:NAME:w

						\tExtendedEntity [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>ExtendedEntity</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="label"><b> + label</b>: string</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=empty arrowsize=2]
						\tBaseEntity:s -> ExtendedEntity:n

						\tObjectId [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>ObjectId</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="timestamp"><b> + timestamp</b>: Integer</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="nonce"><b> + nonce</b>: Long</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						\tRelChild [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>RelChild</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="oid"><b> + oid</b>: ObjectId</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="parentId"><b> + parentId</b>: ObjectId</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tRelChild:oid:e -> ObjectId:NAME:w
						\tRelChild:parentId:e -> ObjectId:NAME:w

						\tBaseEntity [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>BaseEntity</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="id"><b> + id</b>: Long</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						\tRestErrorResponse [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>RestErrorResponse</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="oid"><b> + oid</b>: ObjectId</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="name"><b> + name</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="message"><b> + message</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="time"><b> + time</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="status"><b> + status</b>: Integer</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="statusMessage"><b> + statusMessage</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="inputError"><b> + inputError</b>: List&lt;RestInputError&gt;</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tRestErrorResponse:oid:e -> ObjectId:NAME:w
						\tRestErrorResponse:inputError:e -> RestInputError:NAME:w

						\tRestInputError [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>RestInputError</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="argument"><b> + argument</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="path"><b> + path</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="message"><b> + message</b>: string</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						}
						""",
				graph);
	}

	@Test
	public void testNestedContainersPaginationAndEmptyModel(@TempDir final Path dir) throws Exception {
		final String graph = generate(dir, List.of(NestedResource.class));
		Assertions.assertEquals(
				"""
						# Architecture auto-generated file
						digraph UML_Class_diagram {
						    #rankdir=NS;
						\tgraph [
						\t\tpad="0.5"
						\t\tnodesep="1"
						\t\t#ranksep="2"
						\t\tlabel="Rest API server Model"
						\t\tlabelloc="t"
						\t\tfontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"
						\t]
						\tnode [
						\t\tfontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"
						\t\tshape=record
						\t\tstyle=filled
						\t\tfillcolor=gray95
						\t]
						\tedge [fontname="FreeMono,Sans-Mono,Helvetica,Arial,sans-serif"]
						\tNestedResource [
						\t\tshape=plain
						\t\tlabel=<<table color="#FF3333" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t<tr>
						\t\t\t<td><b>NestedResource</b><br/>(REST)</td>
						\t\t</tr>
						\t\t<tr>
						\t\t\t<td>
						\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t<tr><td  align="left" port="get"><b> + get(): NestedHolder</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /nested/</td></tr>
						\t\t\t\t\t<tr><td  align="left" port="index"><b> + index(): Map&lt;string, Product&gt;</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /nested/index</td></tr>
						\t\t\t\t\t<tr><td  align="left" port="page"><b> + page(): Pagination&lt;Product&gt;</b></td></tr>
						\t\t\t\t\t\t\t<tr><td  align="left">    /nested/page</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tNestedResource:get:e -> NestedHolder:NAME:w
						\tNestedResource:index:e -> Product:NAME:w
						\tNestedResource:page:e -> Product:NAME:w






						\tNestedHolder [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>NestedHolder</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="matrix"><b> + matrix</b>: List&lt;List&lt;Product&gt;&gt;</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="groups"><b> + groups</b>: Map&lt;string, List&lt;Product&gt;&gt;</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="empty"><b> + empty</b>: EmptyModel</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="owner"><b> + owner</b>: ObjectId</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tNestedHolder:matrix:e -> Product:NAME:w
						\tNestedHolder:groups:e -> Product:NAME:w
						\tNestedHolder:empty:e -> EmptyModel:NAME:w
						\tNestedHolder:owner:e -> ObjectId:NAME:w

						\tProduct [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>Product</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="id"><b> + id</b>: Long</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="name"><b> + name</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="price"><b> + price</b>: Double</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="category"><b> + category</b>: CategoryEnum</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tProduct:category:e -> CategoryEnum:NAME:w

						\tEmptyModel [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>EmptyModel</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td> <i>(empty)</i> </td></tr>\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						\tObjectId [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>ObjectId</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="timestamp"><b> + timestamp</b>: Integer</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="nonce"><b> + nonce</b>: Long</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						\tCategoryEnum [
						\t\tshape=plain
						\t\tlabel=<<table color="#33FF33" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>CategoryEnum</b><br/>(ENUM)</td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left"><b> + CLOTHING</b> = 'CLOTHING'</td></tr>
						\t\t\t\t\t\t<tr><td  align="left"><b> + ELECTRONICS</b> = 'ELECTRONICS'</td></tr>
						\t\t\t\t\t\t<tr><td  align="left"><b> + FOOD</b> = 'FOOD'</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						\tRestErrorResponse [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>RestErrorResponse</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="oid"><b> + oid</b>: ObjectId</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="name"><b> + name</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="message"><b> + message</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="time"><b> + time</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="status"><b> + status</b>: Integer</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="statusMessage"><b> + statusMessage</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="inputError"><b> + inputError</b>: List&lt;RestInputError&gt;</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]
						\tedge [dir=back arrowtail=diamond arrowsize=2]
						\tRestErrorResponse:oid:e -> ObjectId:NAME:w
						\tRestErrorResponse:inputError:e -> RestInputError:NAME:w

						\tRestInputError [
						\t\tshape=plain
						\t\tranksep="2"
						\t\tlabel=<<table color="#000000" border="2" cellborder="1" cellspacing="0" cellpadding="4">
						\t\t\t<tr>
						\t\t\t\t<td port="NAME"><b>RestInputError</b></td>
						\t\t\t</tr>
						\t\t\t<tr>
						\t\t\t\t<td>
						\t\t\t\t\t<table border="0" cellborder="0" cellspacing="0" >
						\t\t\t\t\t\t<tr><td  align="left" port="argument"><b> + argument</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="path"><b> + path</b>: string</td></tr>
						\t\t\t\t\t\t<tr><td  align="left" port="message"><b> + message</b>: string</td></tr>
						\t\t\t\t\t</table>
						\t\t\t\t</td>
						\t\t\t</tr>
						\t\t</table>>
						\t]

						}
						""",
				graph);
	}

	@Test
	public void testGraphIsStable(@TempDir final Path dir) throws Exception {
		Assertions.assertEquals(generate(dir, List.of(CatalogResource.class)),
				generate(dir, List.of(CatalogResource.class)));
	}
}

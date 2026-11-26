package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 * A variant of a child model ({@code ChildCreate}) extends the same variant of its parent: the file of the parent
 * must export it, whatever the order the models are met.
 */
public class TestTypeScriptParentVariant {

	public static class Parent {
		public String name;
	}

	public static class Child extends Parent {
		public Long age;
	}

	/** The parent is met first (read resource, analyzed first)... */
	@Path("animals")
	public class ReadResource {
		@GET
		public Parent get() {
			return null;
		}
	}

	/** ... then the child, as a create body. */
	@Path("animals")
	public class WriteResource {
		@POST
		public Child create(@Valid @ValidGroup(GroupCreate.class) final Child data) {
			return null;
		}
	}

	@Test
	public void testParentFileExportsTheVariantUsedByItsChild() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ReadResource.class, WriteResource.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		final String child = generation.get(Paths.get("model").resolve("child.ts"));
		Assertions.assertTrue(child.contains("ZodParentCreate.extend({"), child);
		final String parent = generation.get(Paths.get("model").resolve("parent.ts"));
		Assertions.assertTrue(parent.contains("export const ZodParentCreate = "), parent);
		Assertions.assertTrue(parent.contains("export const ZodParent = "), parent);
	}
}

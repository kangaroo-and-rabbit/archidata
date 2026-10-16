package test.atriasoft.archidata.api;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.api.FrontGeneric;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;

/** {@link FrontGeneric} serves only files inside its folder, a sibling folder with the same prefix included. */
class TestFrontGeneric {
	private static Path root;

	private static final class Front extends FrontGeneric {
		Front(final Path folder) {
			this.baseFrontFolder = folder.toString();
		}
	}

	private static List<PathSegment> segments(final String... names) {
		final List<PathSegment> out = new ArrayList<>();
		for (final String name : names) {
			out.add(new PathSegment() {
				@Override
				public String getPath() {
					return name;
				}

				@Override
				public MultivaluedMap<String, String> getMatrixParameters() {
					return new MultivaluedHashMap<>();
				}
			});
		}
		return out;
	}

	@BeforeAll
	static void createFolders() throws Exception {
		root = Files.createTempDirectory("archidata-front");
		Files.createDirectories(root.resolve("front"));
		Files.createDirectories(root.resolve("front-old"));
		Files.writeString(root.resolve("front/index.html"), "<html></html>", StandardCharsets.UTF_8);
		Files.writeString(root.resolve("front/app.js"), "console.log(1);", StandardCharsets.UTF_8);
		Files.writeString(root.resolve("front-old/config.json"), "{\"secret\":1}", StandardCharsets.UTF_8);
	}

	@Test
	void fileInsideTheFolderIsServed() throws Exception {
		final Response response = new Front(root.resolve("front")).retrive1(segments("app.js"));
		Assertions.assertEquals(200, response.getStatus());
	}

	@Test
	void siblingFolderWithSamePrefixIsRefused() {
		final NotFoundException ex = Assertions.assertThrows(NotFoundException.class,
				() -> new Front(root.resolve("front")).retrive1(segments("..", "front-old", "config.json")));
		Assertions.assertFalse(ex.getMessage().contains("config"));
	}

	@Test
	void parentFolderIsRefused() {
		Assertions.assertThrows(NotFoundException.class,
				() -> new Front(root.resolve("front")).retrive1(segments("..", "..", "etc", "passwd.json")));
	}

	@Test
	void symbolicLinkOutOfTheFolderIsRefused() throws Exception {
		final Path link = root.resolve("front/link.json");
		if (!Files.exists(link)) {
			Files.createSymbolicLink(link, root.resolve("front-old/config.json"));
		}
		Assertions.assertThrows(NotFoundException.class,
				() -> new Front(root.resolve("front")).retrive1(segments("link.json")));
	}

	@Test
	void missingFileMessageDoesNotEchoTheName() {
		final NotFoundException ex = Assertions.assertThrows(NotFoundException.class,
				() -> new Front(root.resolve("front")).retrive1(segments("missing-name.js")));
		Assertions.assertFalse(ex.getMessage().contains("missing-name"));
	}
}

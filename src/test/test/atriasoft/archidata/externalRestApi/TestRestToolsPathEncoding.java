package test.atriasoft.archidata.externalRestApi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The path parameters of the generated clients are percent-encoded, and the values {@code .} and
 * {@code ..} are refused (CS-03). Runs the real runtimes ({@code rest-tools.ts} with node,
 * {@code rest_tools.py} with python3); a test is skipped when its interpreter (or the Python
 * dependencies of the runtime) is missing.
 */
public class TestRestToolsPathEncoding {

	private static String run(final Path dir, final String... command) throws IOException, InterruptedException {
		final Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
		final String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		Assertions.assertTrue(process.waitFor(60, TimeUnit.SECONDS), "timeout: " + out);
		Assertions.assertEquals(0, process.exitValue(), out);
		return out;
	}

	private static boolean available(final String... command) {
		try {
			final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			process.getInputStream().readAllBytes();
			return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
		} catch (final IOException | InterruptedException e) {
			return false;
		}
	}

	/** {@code --experimental-strip-types} runs TypeScript from node 22.7 on. */
	private static boolean nodeSupportsTypeScript() {
		try {
			final Process process = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			final String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
				return false;
			}
			final String[] parts = out.replaceFirst("^v", "").split("\\.");
			final int major = Integer.parseInt(parts[0]);
			final int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			return major > 22 || (major == 22 && minor >= 7);
		} catch (final IOException | InterruptedException | NumberFormatException e) {
			return false;
		}
	}

	@Test
	public void typeScriptEncodesPathParameters(@TempDir final Path dir) throws Exception {
		Assumptions.assumeTrue(nodeSupportsTypeScript(), "node >= 22.7 not available");
		// node resolves ES modules with their extension: point the runtime to the stub model.
		Files.writeString(dir.resolve("rest-tools.ts"),
				Files.readString(Paths.get("src/resources/rest-tools.ts")).replace("'./model'", "'./model.ts'"));
		Files.writeString(dir.resolve("model.ts"),
				"export class RestErrorResponse {}\nexport function isRestErrorResponse(a: any): boolean { return false; }\n");
		Files.writeString(dir.resolve("t.ts"),
				"""
						import { HTTPMimeType, HTTPRequestModel, RESTRequestJson, RESTUrl, fetchProgress } from './rest-tools.ts';
						console.log(HTTPMimeType.JSON, HTTPRequestModel.GET);
						const m: any = { endPoint: 'data/{oid}/{name}' };
						const cfg: any = { server: 'http://h/api' };
						console.log(RESTUrl({ restModel: m, restConfig: cfg, params: { oid: '1', name: '../../user/x?y#z' } } as any));
						for (const bad of ['.', '..']) {
							try {
								RESTUrl({ restModel: m, restConfig: cfg, params: { oid: '1', name: bad } } as any);
								console.log('NOT-REFUSED');
							} catch (e) {
								console.log('REFUSED ' + (e as Error).message);
							}
						}
						const plain: any = { endPoint: 'x' };
						console.log('QUERY ' + RESTUrl({ restModel: plain, restConfig: cfg, queries: { a: undefined, b: [1, null], c: null } } as any));
						console.log('EMPTY ' + RESTUrl({ restModel: plain, restConfig: cfg, queries: { a: undefined } } as any));
						// The token travels in the URL, and must not reach the console through it.
						(globalThis as any).fetch = async () =>
							new Response('[]', { status: 200, headers: { 'Content-Type': 'application/json' } });
						await RESTRequestJson({ restModel: { endPoint: 'x', tokenInUrl: true }, restConfig: { server: 'http://h/api', token: 'secret-token' } } as any);
						// What XMLHttpRequest gives for a 204: an empty body, and no header to read.
						class FakeXhr {
							static DONE = 4;
							readyState = 4;
							status = 204;
							statusText = '';
							response = '';
							upload = { addEventListener() {} };
							loadend: any;
							addEventListener(name: string, callback: any) {
								if (name === 'loadend') {
									this.loadend = callback;
								}
							}
							open() {}
							setRequestHeader() {}
							getAllResponseHeaders() {
								return '';
							}
							send() {
								this.loadend();
							}
						}
						(globalThis as any).XMLHttpRequest = FakeXhr;
						const empty = await fetchProgress('http://h/api/x', { method: 'DELETE', headers: {}, body: null } as any, {});
						console.log('NO-CONTENT ' + empty.status);
						""");
		final List<String> command = new ArrayList<>(List.of("node", "--no-warnings"));
		// On by default in recent versions, where this option may disappear as the transform one did.
		if (available("node", "--experimental-strip-types", "--version")) {
			command.add("--experimental-strip-types");
		}
		command.add("t.ts");
		final String out = run(dir, command.toArray(String[]::new));
		Assertions.assertTrue(out.contains("http://h/api/data/1/..%2F..%2Fuser%2Fx%3Fy%23z"), out);
		Assertions.assertTrue(out.contains("application/json GET"), out);
		Assertions.assertTrue(out.contains("QUERY http://h/api/x?b=1\n"), out);
		Assertions.assertTrue(out.contains("EMPTY http://h/api/x\n"), out);
		Assertions.assertTrue(out.contains("Have no check of MODEL in API: x"), out);
		Assertions.assertFalse(out.contains("secret-token"), out);
		Assertions.assertTrue(out.contains("NO-CONTENT 204"), out);
		Assertions.assertFalse(out.contains("NOT-REFUSED"), out);
		Assertions.assertEquals(2, out.split("REFUSED ", -1).length - 1, out);
	}

	@Test
	public void pythonEncodesAndRefusesDots(@TempDir final Path dir) throws Exception {
		Assumptions.assumeTrue(available("python3", "-I", "-c", "import pydantic, urllib3"),
				"python3 with pydantic and urllib3 not available");
		final Path pkg = Files.createDirectories(dir.resolve("pkg"));
		Files.writeString(pkg.resolve("__init__.py"), "");
		Files.copy(Paths.get("src/resources/python/rest_tools.py"), pkg.resolve("rest_tools.py"));
		Files.writeString(pkg.resolve("model.py"),
				"from pydantic import BaseModel\n\n\nclass RestErrorResponse(BaseModel):\n    pass\n");
		Files.writeString(dir.resolve("t.py"),
				"""
						from pkg.rest_tools import RESTConfig, RESTError, RESTModel, RESTRequestType, RESTUrl
						cfg = RESTConfig(server="http://h/api")
						model = RESTModel(end_point="data/{oid}/{name}")
						def url(name):
						    return RESTUrl(RESTRequestType(rest_model=model, rest_config=cfg, params={"oid": "1", "name": name}))
						print(url("../../user/x?y#z"))
						for bad in (".", ".."):
						    try:
						        url(bad)
						        print("NOT-REFUSED")
						    except ValueError as e:
						        print("REFUSED " + str(e))
						print("ERROR " + str(RESTError("failed", status=999, url="http://h/api/x?Authorization=Bearer+secret-token")))
						""");
		final String out = run(dir, "python3", "t.py");
		Assertions.assertTrue(out.contains("http://h/api/data/1/..%2F..%2Fuser%2Fx%3Fy%23z"), out);
		Assertions.assertFalse(out.contains("NOT-REFUSED"), out);
		Assertions.assertEquals(2, out.split("REFUSED ", -1).length - 1, out);
		Assertions.assertTrue(out.contains("ERROR "), out);
		Assertions.assertFalse(out.contains("secret-token"), out);
	}
}

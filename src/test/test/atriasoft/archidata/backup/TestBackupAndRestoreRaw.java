package test.atriasoft.archidata.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.backup.model.DataStoreWithUpdate;
import test.atriasoft.archidata.backup.model.DataStoreWithoutUpdate;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@EnabledIfEnvironmentVariable(named = "INCLUDE_MONGO_SPECIFIC", matches = "true")
public class TestBackupAndRestoreRaw {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestBackupAndRestoreRaw.class);

	/**
	 * Extracts all files from a .tar.gz archive into a Map where keys are file
	 * names and values are their textual content.
	 *
	 * @param inputPath Path to the .tar.gz archive
	 * @return Map of filename to file content as String
	 * @throws IOException if reading the archive fails
	 */
	public Map<String, String> extractTarGzToMap(Path inputPath) throws IOException {
		Map<String, String> result = new HashMap<>();

		try (InputStream fileIn = Files.newInputStream(inputPath);
				BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
				GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(bufferedIn);
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

			TarArchiveEntry entry;
			while ((entry = tarIn.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				tarIn.transferTo(out);
				String content = out.toString(StandardCharsets.UTF_8); // ou autre charset si nécessaire
				result.put(entry.getName(), content);
			}
		}

		return result;
	}

	/**
	 * Write a Map<String, String> into a .tar.gz archive. Each entry becomes a file
	 * with its content.
	 *
	 * @param data   Map with file name as key and content as value
	 * @param output Path to the .tar.gz file to create
	 * @throws IOException if writing fails
	 */
	public void writeMapToTarGz(Map<String, String> data, Path output) throws IOException {
		try (OutputStream fileOut = Files.newOutputStream(output);
				BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
				GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(bufferedOut);
				TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {

			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

			for (Map.Entry<String, String> entry : data.entrySet()) {
				byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
				TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
				tarEntry.setSize(content.length);
				tarOut.putArchiveEntry(tarEntry);
				tarOut.write(content);
				tarOut.closeArchiveEntry();
			}
		}
	}

	String revoveDateAndObjectId(String data, boolean ignoreDate) {
		String tmp = data //
				.replaceAll("\"_id\"\\s*:\\s*\"[a-f0-9]{24}\"", "\"_id\":\"IGNORE\""); // objectID
		if (ignoreDate) {
			return tmp;
		}
		return tmp.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z", "IGNORE"); // timestamp ISO 8601
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Test
	public void testBackupCollectionRawData() throws Exception {
		// Clean the collection:
		DataAccess.drop(DataStoreWithoutUpdate.class);
		DataAccess.drop(DataStoreWithUpdate.class);
		// Insert the data in the collection:
		DataStoreWithUpdate dataInsert = new DataStoreWithUpdate();
		dataInsert.dataLong = 9953L;
		dataInsert.dataDoubles = List.of(1.25, -1.66);
		DataStoreWithUpdate data11 = DataAccess.insert(dataInsert);
		dataInsert.dataLong = null;
		dataInsert.dataDoubles = List.of(152.8, 5213546345.0);
		DataStoreWithUpdate data12 = DataAccess.insert(dataInsert);
		DataStoreWithoutUpdate dataInsert2 = new DataStoreWithoutUpdate();
		dataInsert2.dataString = "my test String";
		dataInsert2.dataTime = Date
				.from(LocalDateTime.of(2523, 12, 22, 15, 32, 0, 254_000_000).atZone(ZoneOffset.UTC).toInstant());
		DataStoreWithoutUpdate data21 = DataAccess.insert(dataInsert2);
		dataInsert2.dataString = "my second test string";
		dataInsert2.dataTime = Date
				.from(LocalDateTime.of(2523, 05, 1, 05, 59, 24, 241_000_000).atZone(ZoneOffset.UTC).toInstant());
		DataStoreWithoutUpdate data22 = DataAccess.insert(dataInsert2);
		BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base");
		engine.addClass(DataStoreWithUpdate.class);
		engine.addClass(DataStoreWithoutUpdate.class);
		engine.store(null);
		Map<String, String> dataExtract = extractTarGzToMap(Paths.get("./").resolve("test_store_data_base.tar.gz"));
		Assertions.assertNotNull(dataExtract);
		Assertions.assertEquals(2, dataExtract.size());
		Assertions.assertEquals("""
				{"_id":"IGNORE","dataLong":9953,"dataDoubles":[1.25,-1.66],"createdAt":"IGNORE","updatedAt":"IGNORE"}
				{"_id":"IGNORE","dataDoubles":[152.8,5.213546345E9],"createdAt":"IGNORE","updatedAt":"IGNORE"}
				""", revoveDateAndObjectId(dataExtract.get("DataStoreWithUpdate.json"), false));
		Assertions.assertEquals("""
				{"_id":"IGNORE","dataString":"my test String","dataTime":"2523-12-22T15:32:00.254Z"}
				{"_id":"IGNORE","dataString":"my second test string","dataTime":"2523-05-01T05:59:24.241Z"}
				""", revoveDateAndObjectId(dataExtract.get("DataStoreWithoutUpdate.json"), true));

	}

	@Test
	public void testRestoreCollectionRawData() throws Exception {
		// Clean the collection
		DataAccess.drop(DataStoreWithoutUpdate.class);
		DataAccess.drop(DataStoreWithUpdate.class);
		// Generate the Tag.gz file for test:
		Map<String, String> data = Map.of( //
				"DataStoreWithUpdate.json",
				"""
						{"_id":"68548c414d3d545cebdf4289","dataLong":9953,"dataDoubles":[55.0,-65.84],"createdAt":"2025-06-19T22:16:33.420Z","updatedAt":"2025-06-19T22:16:33.420Z"}
						{"_id":"68548c414d3d545cebdf428b","dataDoubles":[-9235,5.25],"createdAt":"2025-06-19T22:16:33.494Z","updatedAt":"2025-06-19T22:16:33.494Z"}
						""", //
				"DataStoreWithoutUpdate.json",
				"""
						{"_id":"68548c414d3d545cebdf428d","dataString":"a string data test","dataTime":"2523-12-22T15:32:00.254Z"}
						{"_id":"68548c414d3d545cebdf428f","dataString":"other string data test","dataTime":"2523-05-01T05:59:24.241Z"}
						""");
		Path fileTestPath = Paths.get("./").resolve("test_store_data_base.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base");
		engine.restore(null);
		{
			List<DataStoreWithUpdate> testData = DataAccess.gets(DataStoreWithUpdate.class);
			Assertions.assertNotNull(testData);
			Assertions.assertEquals(2, testData.size());
		}
		{
			List<DataStoreWithoutUpdate> testData = DataAccess.gets(DataStoreWithoutUpdate.class);
			Assertions.assertNotNull(testData);
			Assertions.assertEquals(2, testData.size());
		}
	}
}

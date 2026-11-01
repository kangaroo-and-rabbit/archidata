package test.atriasoft.archidata.tools;

import org.atriasoft.archidata.tools.DataTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestDataTools {

	// ===== bytesToHex tests =====

	@Test
	void testBytesToHexEmpty() {
		final String result = DataTools.bytesToHex(new byte[0]);
		Assertions.assertEquals("", result);
	}

	@Test
	void testBytesToHexSingleByte() {
		final String result = DataTools.bytesToHex(new byte[] { (byte) 0xFF });
		Assertions.assertEquals("ff", result);
	}

	@Test
	void testBytesToHexZeroByte() {
		final String result = DataTools.bytesToHex(new byte[] { 0x00 });
		Assertions.assertEquals("00", result);
	}

	@Test
	void testBytesToHexMultipleBytes() {
		final String result = DataTools
				.bytesToHex(new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF });
		Assertions.assertEquals("0123456789abcdef", result);
	}

	@Test
	void testBytesToHexLeadingZeros() {
		final String result = DataTools.bytesToHex(new byte[] { 0x00, 0x0A, 0x0B });
		Assertions.assertEquals("000a0b", result);
	}

	// ===== multipartCorrection tests =====

	@Test
	void testMultipartCorrectionNull() {
		Assertions.assertNull(DataTools.multipartCorrection(null));
	}

	@Test
	void testMultipartCorrectionEmpty() {
		Assertions.assertNull(DataTools.multipartCorrection(""));
	}

	@Test
	void testMultipartCorrectionNullString() {
		Assertions.assertNull(DataTools.multipartCorrection("null"));
	}

	@Test
	void testMultipartCorrectionUndefined() {
		Assertions.assertNull(DataTools.multipartCorrection("undefined"));
	}

	@Test
	void testMultipartCorrectionValidValue() {
		Assertions.assertEquals("hello", DataTools.multipartCorrection("hello"));
	}

	@Test
	void testMultipartCorrectionWhitespace() {
		// Whitespace-only string is NOT treated as empty
		Assertions.assertEquals(" ", DataTools.multipartCorrection(" "));
	}

	@Test
	void testMultipartCorrectionNullUpperCase() {
		// "NULL" is not "null", should be kept as-is
		Assertions.assertEquals("NULL", DataTools.multipartCorrection("NULL"));
	}

	// ===== saveFile + bytesToHex integration test =====

	@Test
	void testSaveFileByteArrayAndReadBack() throws Exception {
		final String tmpDir = System.getProperty("java.io.tmpdir");
		final String filePath = tmpDir + "/test_data_tools_" + System.currentTimeMillis();
		final byte[] data = "Hello, World!".getBytes();

		final String sha512 = DataTools.saveFile(data, filePath);

		// SHA-512 of "Hello, World!" is known
		Assertions.assertFalse(sha512.isEmpty());
		Assertions.assertEquals(128, sha512.length()); // SHA-512 = 64 bytes = 128 hex chars

		// Verify the file was written correctly
		final byte[] readBack = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
		Assertions.assertArrayEquals(data, readBack);

		// Cleanup
		java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filePath));
	}

	@Test
	void testSaveFileInputStreamAndReadBack() throws Exception {
		final String tmpDir = System.getProperty("java.io.tmpdir");
		final String filePath = tmpDir + "/test_data_tools_is_" + System.currentTimeMillis();
		final byte[] data = "Stream test data".getBytes();
		final java.io.InputStream inputStream = new java.io.ByteArrayInputStream(data);

		final String sha512 = DataTools.saveFile(inputStream, filePath);

		Assertions.assertFalse(sha512.isEmpty());
		Assertions.assertEquals(128, sha512.length());

		final byte[] readBack = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
		Assertions.assertArrayEquals(data, readBack);

		// Cleanup
		java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filePath));
	}

	@Test
	void testSaveFileBothMethodsProduceSameHash() throws Exception {
		final String tmpDir = System.getProperty("java.io.tmpdir");
		final String filePath1 = tmpDir + "/test_dt_hash1_" + System.currentTimeMillis();
		final String filePath2 = tmpDir + "/test_dt_hash2_" + System.currentTimeMillis();
		final byte[] data = "Consistent hash test".getBytes();

		final String sha512FromBytes = DataTools.saveFile(data, filePath1);
		final String sha512FromStream = DataTools.saveFile(new java.io.ByteArrayInputStream(data), filePath2);

		Assertions.assertEquals(sha512FromBytes, sha512FromStream);

		// Cleanup
		java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filePath1));
		java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filePath2));
	}

	// ===== active content types (W-02) =====

	@Test
	void testActiveMimeTypes() {
		for (final String type : new String[] { "image/svg+xml", "text/html", "TEXT/HTML; charset=utf-8",
				"application/xhtml+xml", "text/xml", "application/xml", "application/javascript", "text/javascript",
				"application/rss+xml" }) {
			Assertions.assertTrue(DataTools.isActiveMimeType(type), type);
		}
		for (final String type : new String[] { "image/png", "image/jpeg", "image/webp", "video/webm", "audio/webm",
				"text/vtt", "application/octet-stream" }) {
			Assertions.assertFalse(DataTools.isActiveMimeType(type), type);
		}
		Assertions.assertFalse(DataTools.isActiveMimeType(null));
	}

	// ===== remote data size limit (W-04) =====

	@Test
	void testMaxRemoteDataSize() {
		Assertions.assertEquals(DataTools.DEFAULT_MAX_REMOTE_DATA_SIZE, DataTools.getMaxRemoteDataSize());
		try {
			DataTools.setMaxRemoteDataSize(1024);
			Assertions.assertEquals(1024, DataTools.getMaxRemoteDataSize());
			Assertions.assertThrows(IllegalArgumentException.class, () -> DataTools.setMaxRemoteDataSize(0));
			Assertions.assertThrows(IllegalArgumentException.class, () -> DataTools.setMaxRemoteDataSize(-1));
			Assertions.assertEquals(1024, DataTools.getMaxRemoteDataSize());
		} finally {
			DataTools.setMaxRemoteDataSize(DataTools.DEFAULT_MAX_REMOTE_DATA_SIZE);
		}
	}

	// ===== temporary identifiers (W-10) =====

	@Test
	void testTmpDataIdsAreUniqueAndReserved() throws Exception {
		final java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("archidata-tmp-id");
		org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(dir.toString());
		try {
			final java.util.Set<Long> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();
			final java.util.List<Thread> threads = new java.util.ArrayList<>();
			for (int iii = 0; iii < 8; iii++) {
				final boolean useResource = iii % 2 == 0;
				final Thread thread = new Thread(() -> {
					for (int jjj = 0; jjj < 100; jjj++) {
						ids.add(useResource ? org.atriasoft.archidata.api.DataResource.getTmpDataId()
								: DataTools.getTmpDataId());
					}
				});
				threads.add(thread);
				thread.start();
			}
			for (final Thread thread : threads) {
				thread.join();
			}
			Assertions.assertEquals(800, ids.size());
			for (final long id : ids) {
				Assertions.assertTrue(id > (1L << 40), "id not random enough: " + id);
				Assertions.assertTrue(
						java.nio.file.Files.exists(java.nio.file.Paths.get(DataTools.getTmpFileInData(id))));
				DataTools.removeTemporaryFile(id);
			}
			try (java.util.stream.Stream<java.nio.file.Path> left = java.nio.file.Files.list(dir)) {
				Assertions.assertEquals(0, left.count());
			}
		} finally {
			org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(null);
		}
	}

	@Test
	void testTmpDataIdNeverReturnedWithoutItsFile() throws Exception {
		// The temporary folder is below a regular file: the reservation can not succeed.
		final java.nio.file.Path blocker = java.nio.file.Files.createTempFile("archidata-tmp-blocker", ".bin");
		org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(blocker.resolve("sub").toString());
		try {
			Assertions.assertThrows(java.io.UncheckedIOException.class, DataTools::getTmpDataId);
			Assertions.assertThrows(java.io.UncheckedIOException.class, DataTools::getTmpFolder);
		} finally {
			org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(null);
			java.nio.file.Files.deleteIfExists(blocker);
		}
	}

	@Test
	void testTmpFoldersAreUniqueAndReserved() throws Exception {
		final java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("archidata-tmp-folder");
		org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(dir.toString());
		try {
			final java.util.Set<String> folders = new java.util.HashSet<>();
			for (int iii = 0; iii < 50; iii++) {
				final String folder = DataTools.getTmpFolder();
				Assertions.assertTrue(folders.add(folder), "folder given twice: " + folder);
				Assertions.assertTrue(java.nio.file.Files.isDirectory(java.nio.file.Paths.get(folder)));
				java.nio.file.Files.delete(java.nio.file.Paths.get(folder));
			}
		} finally {
			org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(null);
			java.nio.file.Files.deleteIfExists(dir);
		}
	}

	@Test
	void testDataResourceSaveTemporaryFileIsBounded() throws Exception {
		final java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("archidata-tmp-bounded");
		org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(dir.toString());
		DataTools.setMaxUploadSize(16);
		try {
			final long id = org.atriasoft.archidata.api.DataResource.getTmpDataId();
			final org.atriasoft.archidata.exception.FailException ex = Assertions.assertThrows(
					org.atriasoft.archidata.exception.FailException.class,
					() -> org.atriasoft.archidata.api.DataResource
							.saveTemporaryFile(new java.io.ByteArrayInputStream(new byte[17]), id));
			Assertions.assertEquals(jakarta.ws.rs.core.Response.Status.REQUEST_ENTITY_TOO_LARGE, ex.status);
			Assertions.assertFalse(java.nio.file.Files.exists(java.nio.file.Paths.get(DataTools.getTmpFileInData(id))));
			final long id2 = org.atriasoft.archidata.api.DataResource.getTmpDataId();
			Assertions.assertEquals(128, org.atriasoft.archidata.api.DataResource
					.saveTemporaryFile(new java.io.ByteArrayInputStream(new byte[16]), id2).length());
			org.atriasoft.archidata.api.DataResource.removeTemporaryFile(id2);
			Assertions
					.assertFalse(java.nio.file.Files.exists(java.nio.file.Paths.get(DataTools.getTmpFileInData(id2))));
		} finally {
			DataTools.setMaxUploadSize(DataTools.DEFAULT_MAX_UPLOAD_SIZE);
			org.atriasoft.archidata.tools.ConfigBaseVariable.setTmpDataFolder(null);
			java.nio.file.Files.deleteIfExists(dir);
		}
	}

	// ===== content type detection of the uploads (W-17) =====

	@Test
	void testDetectAcceptedMimeType() throws Exception {
		final java.nio.file.Path picture = java.nio.file.Files.createTempFile("cover", ".txt");
		final java.nio.file.Path text = java.nio.file.Files.createTempFile("cover", ".png");
		try {
			final java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(4, 4,
					java.awt.image.BufferedImage.TYPE_INT_RGB);
			javax.imageio.ImageIO.write(image, "png", picture.toFile());
			java.nio.file.Files.writeString(text, "not a picture at all");
			// the name says .txt, the content is a PNG: the content wins
			Assertions.assertEquals("image/png",
					DataTools.detectAcceptedMimeType(picture.toString(), DataTools.SUPPORTED_IMAGE_MIME_TYPE));
			final org.atriasoft.archidata.exception.FailException ex = Assertions.assertThrows(
					org.atriasoft.archidata.exception.FailException.class,
					() -> DataTools.detectAcceptedMimeType(text.toString(), DataTools.SUPPORTED_IMAGE_MIME_TYPE));
			Assertions.assertEquals(jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE, ex.status);
		} finally {
			java.nio.file.Files.deleteIfExists(picture);
			java.nio.file.Files.deleteIfExists(text);
		}
	}

	@Test
	void theAcceptedTypesCannotBeWidened() {
		// The arrays they replace could be changed by any code, and they decide what an upload may contain.
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> DataTools.SUPPORTED_UPLOAD_MIME_TYPES.add("text/html"));
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> DataTools.SUPPORTED_IMAGE_MIME_TYPE.set(0, "text/html"));
	}
}

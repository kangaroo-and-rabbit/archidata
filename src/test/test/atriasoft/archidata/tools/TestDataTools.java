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
		final String result = DataTools.bytesToHex(new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB,
				(byte) 0xCD, (byte) 0xEF });
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
}

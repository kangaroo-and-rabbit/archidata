package test.atriasoft.archidata.tools;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestConfigBaseVariable {

	@BeforeEach
	void setup() {
		// Reset all values to env defaults before each test
		ConfigBaseVariable.clearAllValue();
	}

	@AfterEach
	void teardown() {
		// Reset after each test to avoid side effects
		ConfigBaseVariable.clearAllValue();
	}

	// ===== Default values when fields are null =====

	@Test
	void testGetTmpDataFolderDefault() {
		ConfigBaseVariable.tmpDataFolder = null;
		Assertions.assertEquals("./data/tmp", ConfigBaseVariable.getTmpDataFolder());
	}

	@Test
	void testGetTmpDataFolderCustom() {
		ConfigBaseVariable.tmpDataFolder = "/custom/tmp";
		Assertions.assertEquals("/custom/tmp", ConfigBaseVariable.getTmpDataFolder());
	}

	@Test
	void testGetMediaDataFolderDefault() {
		ConfigBaseVariable.dataFolder = null;
		Assertions.assertEquals("./data/media", ConfigBaseVariable.getMediaDataFolder());
	}

	@Test
	void testGetMediaDataFolderCustom() {
		ConfigBaseVariable.dataFolder = "/custom/media";
		Assertions.assertEquals("/custom/media", ConfigBaseVariable.getMediaDataFolder());
	}

	@Test
	void testGetDBHostDefault() {
		ConfigBaseVariable.dbHost = null;
		Assertions.assertEquals("localhost", ConfigBaseVariable.getDBHost());
	}

	@Test
	void testGetDBHostCustom() {
		ConfigBaseVariable.dbHost = "db.example.com";
		Assertions.assertEquals("db.example.com", ConfigBaseVariable.getDBHost());
	}

	@Test
	void testGetDBPortDefault() {
		ConfigBaseVariable.dbPort = null;
		Assertions.assertEquals((short) 27017, ConfigBaseVariable.getDBPort());
	}

	@Test
	void testGetDBPortCustom() {
		ConfigBaseVariable.dbPort = "5432";
		Assertions.assertEquals((short) 5432, ConfigBaseVariable.getDBPort());
	}

	@Test
	void testGetDBLoginDefault() {
		ConfigBaseVariable.dbUser = null;
		Assertions.assertEquals("root", ConfigBaseVariable.getDBLogin());
	}

	@Test
	void testGetDBPasswordDefault() {
		ConfigBaseVariable.dbPassword = null;
		Assertions.assertEquals("base_db_password", ConfigBaseVariable.getDBPassword());
	}

	@Test
	void testGetDBNameReturnsField() {
		ConfigBaseVariable.bdDatabase = "mydb";
		Assertions.assertEquals("mydb", ConfigBaseVariable.getDBName());
	}

	@Test
	void testGetDBNameNull() {
		ConfigBaseVariable.bdDatabase = null;
		Assertions.assertNull(ConfigBaseVariable.getDBName());
	}

	@Test
	void testGetDBKeepConnectedDefault() {
		ConfigBaseVariable.dbKeepConnected = null;
		Assertions.assertFalse(ConfigBaseVariable.getDBKeepConnected());
	}

	@Test
	void testGetDBKeepConnectedTrue() {
		ConfigBaseVariable.dbKeepConnected = "true";
		Assertions.assertTrue(ConfigBaseVariable.getDBKeepConnected());
	}

	@Test
	void testGetTestModeDefault() {
		ConfigBaseVariable.testMode = null;
		Assertions.assertFalse(ConfigBaseVariable.getTestMode());
	}

	@Test
	void testGetTestModeTrue() {
		ConfigBaseVariable.testMode = "true";
		Assertions.assertTrue(ConfigBaseVariable.getTestMode());
	}

	@Test
	void testGetLocalAddressDefault() {
		ConfigBaseVariable.apiAdress = null;
		Assertions.assertEquals("http://0.0.0.0:80/api/", ConfigBaseVariable.getlocalAddress());
	}

	@Test
	void testGetLocalAddressCustom() {
		ConfigBaseVariable.apiAdress = "http://api.example.com/v1/";
		Assertions.assertEquals("http://api.example.com/v1/", ConfigBaseVariable.getlocalAddress());
	}

	@Test
	void testGetSSOAddressNull() {
		ConfigBaseVariable.ssoAdress = null;
		Assertions.assertNull(ConfigBaseVariable.getSSOAddress());
	}

	@Test
	void testGetThumbnailFormatDefault() {
		ConfigBaseVariable.thumbnailFormat = null;
		Assertions.assertEquals("png", ConfigBaseVariable.getThumbnailFormat());
	}

	@Test
	void testGetThumbnailFormatEmpty() {
		ConfigBaseVariable.thumbnailFormat = "";
		Assertions.assertEquals("png", ConfigBaseVariable.getThumbnailFormat());
	}

	@Test
	void testGetThumbnailFormatCustom() {
		ConfigBaseVariable.thumbnailFormat = "webp";
		Assertions.assertEquals("webp", ConfigBaseVariable.getThumbnailFormat());
	}

	@Test
	void testGetThumbnailWidthDefault() {
		ConfigBaseVariable.thumbnailWidth = null;
		Assertions.assertEquals(256, ConfigBaseVariable.getThumbnailWidth());
	}

	@Test
	void testGetThumbnailWidthEmpty() {
		ConfigBaseVariable.thumbnailWidth = "";
		Assertions.assertEquals(256, ConfigBaseVariable.getThumbnailWidth());
	}

	@Test
	void testGetThumbnailWidthCustom() {
		ConfigBaseVariable.thumbnailWidth = "512";
		Assertions.assertEquals(512, ConfigBaseVariable.getThumbnailWidth());
	}

	@Test
	void testGetEMailConfigNull() {
		ConfigBaseVariable.eMailFrom = null;
		ConfigBaseVariable.eMailLogin = null;
		ConfigBaseVariable.eMailPassword = null;
		Assertions.assertNull(ConfigBaseVariable.getEMailConfig());
	}

	@Test
	void testGetEMailConfigPartiallyNull() {
		ConfigBaseVariable.eMailFrom = "test@example.com";
		ConfigBaseVariable.eMailLogin = null;
		ConfigBaseVariable.eMailPassword = "pass";
		Assertions.assertNull(ConfigBaseVariable.getEMailConfig());
	}

	@Test
	void testGetEMailConfigComplete() {
		ConfigBaseVariable.eMailFrom = "test@example.com";
		ConfigBaseVariable.eMailLogin = "login";
		ConfigBaseVariable.eMailPassword = "pass";
		final ConfigBaseVariable.EMailConfig config = ConfigBaseVariable.getEMailConfig();
		Assertions.assertNotNull(config);
		Assertions.assertEquals("test@example.com", config.from());
		Assertions.assertEquals("login", config.login());
		Assertions.assertEquals("pass", config.password());
	}
}

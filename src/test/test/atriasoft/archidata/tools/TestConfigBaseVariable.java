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
		ConfigBaseVariable.setTmpDataFolder(null);
		Assertions.assertEquals("./data/tmp", ConfigBaseVariable.getTmpDataFolder());
	}

	@Test
	void testGetTmpDataFolderCustom() {
		ConfigBaseVariable.setTmpDataFolder("/custom/tmp");
		Assertions.assertEquals("/custom/tmp", ConfigBaseVariable.getTmpDataFolder());
	}

	@Test
	void testGetMediaDataFolderDefault() {
		ConfigBaseVariable.setDataFolder(null);
		Assertions.assertEquals("./data/media", ConfigBaseVariable.getMediaDataFolder());
	}

	@Test
	void testGetMediaDataFolderCustom() {
		ConfigBaseVariable.setDataFolder("/custom/media");
		Assertions.assertEquals("/custom/media", ConfigBaseVariable.getMediaDataFolder());
	}

	@Test
	void testGetDBHostDefault() {
		ConfigBaseVariable.setDbHost(null);
		Assertions.assertEquals("localhost", ConfigBaseVariable.getDBHost());
	}

	@Test
	void testGetDBHostCustom() {
		ConfigBaseVariable.setDbHost("db.example.com");
		Assertions.assertEquals("db.example.com", ConfigBaseVariable.getDBHost());
	}

	@Test
	void testGetDBPortDefault() {
		ConfigBaseVariable.setDbPort(null);
		Assertions.assertEquals(27017, ConfigBaseVariable.getDBPort());
	}

	@Test
	void testGetDBPortCustom() {
		ConfigBaseVariable.setDbPort("5432");
		Assertions.assertEquals(5432, ConfigBaseVariable.getDBPort());
	}

	@Test
	void testGetDBPortAboveShortRange() {
		ConfigBaseVariable.setDbPort("40000");
		Assertions.assertEquals(40000, ConfigBaseVariable.getDBPort());
		ConfigBaseVariable.setDbPort("65535");
		Assertions.assertEquals(65535, ConfigBaseVariable.getDBPort());
	}

	@Test
	void testGetDBPortInvalid() {
		for (final String value : new String[] { "0", "65536", "-1", "abc", "" }) {
			ConfigBaseVariable.setDbPort(value);
			Assertions.assertThrows(IllegalArgumentException.class, ConfigBaseVariable::getDBPort, value);
		}
	}

	@Test
	void testGetDBLoginDefault() {
		ConfigBaseVariable.setDbUser(null);
		Assertions.assertEquals("root", ConfigBaseVariable.getDBLogin());
	}

	@Test
	void testGetDBPasswordDefault() {
		ConfigBaseVariable.setDbPassword(null);
		Assertions.assertThrows(IllegalStateException.class, ConfigBaseVariable::getDBPassword);
	}

	@Test
	void testGetDBNameReturnsField() {
		ConfigBaseVariable.setBdDatabase("mydb");
		Assertions.assertEquals("mydb", ConfigBaseVariable.getDBName());
	}

	@Test
	void testGetDBNameNull() {
		ConfigBaseVariable.setBdDatabase(null);
		Assertions.assertNull(ConfigBaseVariable.getDBName());
	}

	@Test
	void testGetDBAbleToCreateParsesValue() {
		ConfigBaseVariable.setDbAbleToCreate(null);
		Assertions.assertTrue(ConfigBaseVariable.getDBAbleToCreate());
		ConfigBaseVariable.setDbAbleToCreate("true");
		Assertions.assertTrue(ConfigBaseVariable.getDBAbleToCreate());
		ConfigBaseVariable.setDbAbleToCreate("false");
		Assertions.assertFalse(ConfigBaseVariable.getDBAbleToCreate());
	}

	@Test
	void testGetDBKeepConnectedDefault() {
		ConfigBaseVariable.setDbKeepConnected(null);
		Assertions.assertFalse(ConfigBaseVariable.getDBKeepConnected());
	}

	@Test
	void testGetDBKeepConnectedTrue() {
		ConfigBaseVariable.setDbKeepConnected("true");
		Assertions.assertTrue(ConfigBaseVariable.getDBKeepConnected());
	}

	@Test
	void testGetTestModeDefault() {
		ConfigBaseVariable.setTestMode(null);
		Assertions.assertFalse(ConfigBaseVariable.getTestMode());
	}

	@Test
	void testGetTestModeTrue() {
		ConfigBaseVariable.setTestMode("true");
		Assertions.assertTrue(ConfigBaseVariable.getTestMode());
	}

	@Test
	void testgetLocalAddressDefault() {
		ConfigBaseVariable.setApiAddress(null);
		Assertions.assertEquals("http://localhost:80/api/", ConfigBaseVariable.getLocalAddress());
	}

	@Test
	void testgetLocalAddressCustom() {
		ConfigBaseVariable.setApiAddress("http://api.example.com/v1/");
		Assertions.assertEquals("http://api.example.com/v1/", ConfigBaseVariable.getLocalAddress());
	}

	@Test
	void testGetSSOAddressNull() {
		ConfigBaseVariable.setSsoAddress(null);
		Assertions.assertNull(ConfigBaseVariable.getSSOAddress());
	}

	@Test
	void testGetThumbnailFormatDefault() {
		ConfigBaseVariable.setThumbnailFormat(null);
		Assertions.assertEquals("png", ConfigBaseVariable.getThumbnailFormat());
	}

	@Test
	void testGetThumbnailFormatEmpty() {
		ConfigBaseVariable.setThumbnailFormat("");
		Assertions.assertEquals("png", ConfigBaseVariable.getThumbnailFormat());
	}

	@Test
	void testGetThumbnailFormatCustom() {
		ConfigBaseVariable.setThumbnailFormat("webp");
		Assertions.assertEquals("webp", ConfigBaseVariable.getThumbnailFormat());
	}

	@Test
	void testGetThumbnailWidthDefault() {
		ConfigBaseVariable.setThumbnailWidth(null);
		Assertions.assertEquals(256, ConfigBaseVariable.getThumbnailWidth());
	}

	@Test
	void testGetThumbnailWidthEmpty() {
		ConfigBaseVariable.setThumbnailWidth("");
		Assertions.assertEquals(256, ConfigBaseVariable.getThumbnailWidth());
	}

	@Test
	void testGetThumbnailWidthCustom() {
		ConfigBaseVariable.setThumbnailWidth("512");
		Assertions.assertEquals(512, ConfigBaseVariable.getThumbnailWidth());
	}

	@Test
	void testGetEMailConfigNull() {
		ConfigBaseVariable.setEMailFrom(null);
		ConfigBaseVariable.setEMailLogin(null);
		ConfigBaseVariable.setEMailPassword(null);
		Assertions.assertNull(ConfigBaseVariable.getEMailConfig());
	}

	@Test
	void testGetEMailConfigPartiallyNull() {
		ConfigBaseVariable.setEMailFrom("test@example.com");
		ConfigBaseVariable.setEMailLogin(null);
		ConfigBaseVariable.setEMailPassword("pass");
		Assertions.assertNull(ConfigBaseVariable.getEMailConfig());
	}

	@Test
	void testGetEMailConfigComplete() {
		ConfigBaseVariable.setEMailFrom("test@example.com");
		ConfigBaseVariable.setEMailLogin("login");
		ConfigBaseVariable.setEMailPassword("pass");
		final ConfigBaseVariable.EMailConfig config = ConfigBaseVariable.getEMailConfig();
		Assertions.assertNotNull(config);
		Assertions.assertEquals("test@example.com", config.from());
		Assertions.assertEquals("login", config.login());
		Assertions.assertEquals("pass", config.password());
	}

	@Test
	void testCorsAllowedOrigins() {
		ConfigBaseVariable.setCorsAllowedOrigins(null);
		Assertions.assertNull(ConfigBaseVariable.getCorsAllowedOrigins());
		ConfigBaseVariable.setCorsAllowedOrigins("  ");
		Assertions.assertNull(ConfigBaseVariable.getCorsAllowedOrigins());
		ConfigBaseVariable.setCorsAllowedOrigins(" https://a.example.com , http://localhost:4200,,");
		Assertions.assertEquals(java.util.List.of("https://a.example.com", "http://localhost:4200"),
				ConfigBaseVariable.getCorsAllowedOrigins());
		ConfigBaseVariable.setCorsAllowedOrigins(null);
	}
}

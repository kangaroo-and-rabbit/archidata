package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Table;

/**
 * Invalid entity used by tests: {@code @DataEncrypt} is placed on a non-String field, which must be
 * rejected when the class model (and its codecs) is built.
 */
@Table(name = "badEncryptedTable")
public class BadEncryptedTable extends GenericData {

	@DataEncrypt
	public Integer secret;
}

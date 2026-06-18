package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/**
 * Raw view over the same collection as {@link SecuredTable}, with no {@code @DataEncrypt} mapping.
 * Used by tests to read the stored (encrypted) value verbatim and assert it is not stored in clear.
 */
@Table(name = "securedTable")
public class SecuredTableRaw extends GenericData {

	@Column(length = 0)
	public String clearData;

	@Column(length = 0)
	public String symmetricData;

	@Column(length = 0)
	public String asymX25519;

	@Column(length = 0)
	public String asymRsa;
}

package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.model.GenericData;

/**
 * Raw view over the same collection as {@link SecuredTable}, with no {@code @DataEncrypt} mapping.
 * Used by tests to read the stored (encrypted) value verbatim and assert it is not stored in clear.
 */
@Collection("securedTable")
public class SecuredTableRaw extends GenericData {

	public String clearData;

	public String symmetricData;

	public String asymX25519;

	public String asymRsa;
}

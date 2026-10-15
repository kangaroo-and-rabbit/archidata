package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.model.GenericData;

/**
 * Test entity holding fields encrypted with the various supported schemes.
 */
@Collection("securedTable")
public class SecuredTable extends GenericData {

	/** Clear field, used as a control. */
	public String clearData;

	/** Symmetric encryption with the base key. */
	@DataEncrypt
	public String symmetricData;

	/** Asymmetric X25519 sealed box (public key encrypts, private key decrypts). */
	@DataEncrypt(encryptKey = "x-pub", decryptKey = "x-priv")
	public String asymX25519;

	/** Asymmetric RSA. */
	@DataEncrypt(encryptKey = "rsa-pub", decryptKey = "rsa-priv")
	public String asymRsa;

	/** Encrypted with the X25519 public key but no private key available to read back. */
	@DataEncrypt(encryptKey = "x-pub", decryptKey = "x-pub")
	public String encryptOnly;
}

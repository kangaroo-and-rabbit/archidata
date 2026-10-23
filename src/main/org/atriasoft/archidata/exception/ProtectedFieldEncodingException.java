package org.atriasoft.archidata.exception;

/**
 * Thrown when the MongoDB driver is asked to encode an application object that holds a field marked
 * {@code @DataNotRead} or {@code @DataEncrypt}.
 *
 * <p>The codecs of the driver (automatic POJO codec, record codec) ignore these annotations: they would write a
 * {@code @DataNotRead} field like any other and a {@code @DataEncrypt} field in clear. The client opened by
 * archidata therefore refuses to encode such an object, wherever the driver meets it: a typed collection, a value
 * of a {@code Document} or of an {@code Updates} / {@code Filters} builder, the update of
 * {@code DataAccess.updateBsonDocuments}. The message names the class, the path of the protected field and the
 * annotation. Decoding is not affected.
 *
 * <p>Write the object through {@code DataAccess} ({@code insert}, {@code updateById}...), which applies the
 * annotations, or pass a {@code Document} of plain values.
 */
public class ProtectedFieldEncodingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs the exception.
	 *
	 * @param message the message naming the class, the protected field and the remedy
	 */
	public ProtectedFieldEncodingException(final String message) {
		super(message);
	}
}

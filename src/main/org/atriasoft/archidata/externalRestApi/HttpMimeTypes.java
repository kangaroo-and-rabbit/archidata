package org.atriasoft.archidata.externalRestApi;

import jakarta.ws.rs.core.MediaType;

/**
 * Media types known by the {@code HTTPMimeType} table of the generated clients (TypeScript and Python).
 */
public final class HttpMimeTypes {

	private HttpMimeTypes() {
		// utility class
	}

	/**
	 * Gets the member of {@code HTTPMimeType} that stands for a media type.
	 *
	 * @param mediaType the media type, as written in {@code @Produces} or {@code @Consumes}
	 * @return the member name ({@code JSON}, {@code MULTIPART}, {@code TEXT_PLAIN}, {@code OCTET_STREAM},
	 *         {@code CSV}, {@code IMAGE_JPEG}, {@code IMAGE_PNG} or {@code ALL}), or {@code null} when the
	 *         generated clients do not know this media type
	 */
	public static String memberName(final String mediaType) {
		if (MediaType.APPLICATION_JSON.equals(mediaType)) {
			return "JSON";
		}
		if (MediaType.MULTIPART_FORM_DATA.equals(mediaType)) {
			return "MULTIPART";
		}
		if (MediaType.TEXT_PLAIN.equals(mediaType)) {
			return "TEXT_PLAIN";
		}
		if (MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			return "OCTET_STREAM";
		}
		if ("text/csv".equals(mediaType)) {
			return "CSV";
		}
		if ("image/jpeg".equals(mediaType)) {
			return "IMAGE_JPEG";
		}
		if ("image/png".equals(mediaType)) {
			return "IMAGE_PNG";
		}
		if (MediaType.WILDCARD.equals(mediaType)) {
			return "ALL";
		}
		return null;
	}
}

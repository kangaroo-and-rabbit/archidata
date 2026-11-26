package org.atriasoft.archidata.dataAccess.options;

/** Reads the fields carrying {@code @DataNotRead} (such as {@code createdAt} and {@code updatedAt}, the
 * ones of embedded objects included), which a read leaves {@code null} by default. */
public class ReadAllColumn extends QueryOption {
	/** Constructs an option to read the {@code @DataNotRead} fields too. */
	public ReadAllColumn() {
		// default constructor
	}
}

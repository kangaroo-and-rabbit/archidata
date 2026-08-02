package test.atriasoft.archidata.journal.model;

import org.atriasoft.archidata.model.OIDGenericData;

/** Journalized model carrying the automatic {@code createdAt}/{@code updatedAt} timestamps. */
public class JournalDataWithUpdate extends OIDGenericData {

	public Long dataLong;

	public String dataString;
}

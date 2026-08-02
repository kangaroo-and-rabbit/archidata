package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.model.OIDGenericData;

/** Soft-deletable model whose 'deleted' field declares no default value. */
public class SoftDeleteNoDefault extends OIDGenericData {

	public String data;

	@DataNotRead
	@DataDeleted
	public Boolean deleted = null;
}

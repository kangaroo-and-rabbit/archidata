package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.annotation.Indexed;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

/** Entity used by the synchronization tests. */
@Collection("IndexEngineTable")
@Index({ "companyId", "-createdOn" })
public class IndexModelEngine extends OIDGenericData {

	public ObjectId companyId;

	public Date createdOn;

	@Indexed(unique = true)
	public String reference;

	public String label;
}

package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.annotation.Indexed;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Table;

/** Entity used by the synchronization tests. */
@Table(name = "IndexEngineTable")
@Index({ "companyId", "-createdOn" })
public class IndexModelEngine extends OIDGenericData {

	public ObjectId companyId;

	public Date createdOn;

	@Indexed(unique = true)
	public String reference;

	public String label;
}

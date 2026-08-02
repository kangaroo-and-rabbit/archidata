package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

/** No annotation at all: everything is declared programmatically. */
public class IndexModelPlain extends OIDGenericData {

	public ObjectId companyId;

	public Date createdOn;

	public String email;

	public ObjectId getCompanyId() {
		return this.companyId;
	}

	public Date getCreatedOn() {
		return this.createdOn;
	}

	public String getEmail() {
		return this.email;
	}
}

package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

/** Same collection as {@link IndexModelEngine}, with a different set of indexes. */
@Collection("IndexEngineTable")
@Index("label")
public class IndexModelEngineChanged extends OIDGenericData {

	public ObjectId companyId;

	public Date createdOn;

	public String reference;

	public String label;
}

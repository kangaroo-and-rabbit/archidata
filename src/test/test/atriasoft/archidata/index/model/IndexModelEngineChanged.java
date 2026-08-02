package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Table;

/** Same collection as {@link IndexModelEngine}, with a different set of indexes. */
@Table(name = "IndexEngineTable")
@Index("label")
public class IndexModelEngineChanged extends OIDGenericData {

	public ObjectId companyId;

	public Date createdOn;

	public String reference;

	public String label;
}

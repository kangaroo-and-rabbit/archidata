package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Column;

/** Indexes declared with the class-level annotation. */
@Index({ "companyId", "-publishedAt" })
@Index(value = "email", unique = true)
public class IndexModelClassAnnotation extends OIDGenericData {

	public ObjectId companyId;

	public Date publishedAt;

	@Column(name = "email")
	public String mailAddress;

	public SubAddress address;
}

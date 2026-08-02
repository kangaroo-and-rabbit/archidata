package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Indexed;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Column;

/** Indexes declared with the property-level annotation. */
public class IndexModelFieldAnnotation extends OIDGenericData {

	@Indexed(unique = true)
	public String login;

	@Indexed(ascending = false)
	public Date lastSeenAt;

	@Indexed(expireAfterSeconds = 3600)
	public Date sessionExpireAt;

	/** Declares an unicity archidata does not enforce: must raise a warning, not an index. */
	@Column(unique = true)
	public String legacyKey;
}

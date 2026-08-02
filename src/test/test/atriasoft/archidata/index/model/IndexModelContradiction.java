package test.atriasoft.archidata.index.model;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.annotation.Indexed;
import org.atriasoft.archidata.model.OIDGenericData;

/** Same field indexed twice with disagreeing options: must be rejected. */
@Index("email")
public class IndexModelContradiction extends OIDGenericData {

	@Indexed(unique = true)
	public String email;
}

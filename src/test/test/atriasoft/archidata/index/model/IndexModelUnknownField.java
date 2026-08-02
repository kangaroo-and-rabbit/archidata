package test.atriasoft.archidata.index.model;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.model.OIDGenericData;

/** Index on a field that does not exist: must be rejected. */
@Index("emailAdress")
public class IndexModelUnknownField extends OIDGenericData {

	public String emailAddress;
}

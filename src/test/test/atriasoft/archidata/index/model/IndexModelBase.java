package test.atriasoft.archidata.index.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.model.OIDGenericData;

/** Base model carrying an index every child must inherit. */
@Index("-archivedAt")
public class IndexModelBase extends OIDGenericData {

	public Date archivedAt;
}

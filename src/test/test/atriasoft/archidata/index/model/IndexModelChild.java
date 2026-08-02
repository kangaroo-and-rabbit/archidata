package test.atriasoft.archidata.index.model;

import org.atriasoft.archidata.annotation.Index;

/** Child of an annotated model: inherits its index and adds one. */
@Index("label")
public class IndexModelChild extends IndexModelBase {

	public String label;
}

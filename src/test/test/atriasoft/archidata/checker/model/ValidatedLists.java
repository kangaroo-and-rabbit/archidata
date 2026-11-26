package test.atriasoft.archidata.checker.model;

import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.CollectionItemNotNull;
import org.atriasoft.archidata.annotation.checker.CollectionItemUnique;
import org.atriasoft.archidata.annotation.checker.CollectionNotEmpty;
import org.atriasoft.archidata.annotation.checker.ValueInList;

/** A bean carrying every collection and list constraint of archidata, one per field. */
public class ValidatedLists {
	@CollectionItemNotNull
	public List<String> noNullItem;
	@CollectionItemUnique
	public List<String> uniqueItem;
	@CollectionNotEmpty
	public List<String> notEmpty;
	@CollectionNotEmpty
	public Set<Integer> notEmptySet;
	@ValueInList({ "red", "green" })
	public String color;
	public List<@ValueInList({ "small", "large" }) String> sizes;
	/** Not a collection: the three collection constraints let any value through. */
	@CollectionItemNotNull
	@CollectionItemUnique
	@CollectionNotEmpty
	public String notACollection;
}

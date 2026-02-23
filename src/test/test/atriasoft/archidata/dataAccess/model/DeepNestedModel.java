package test.atriasoft.archidata.dataAccess.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.model.OIDGenericData;

public class DeepNestedModel extends OIDGenericData {

	public ComplexSubObject simpleObject;
	public NestedSubObject nestedObject;
	public Map<String, ComplexSubObject> mapOfObjects;
	public Map<String, Map<String, ComplexSubObject>> mapOfMapOfObjects;
	public Map<String, List<ComplexSubObject>> mapOfListOfObjects;
	public List<ComplexSubObject> listOfObjects;
	public List<Map<String, Integer>> listOfMaps;
	public Set<String> setOfStrings;
	public Map<String, Set<String>> mapOfSets;

}

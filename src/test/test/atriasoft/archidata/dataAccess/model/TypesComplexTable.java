package test.atriasoft.archidata.dataAccess.model;

import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.DataJson;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class TypesComplexTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;

	@DataJson
	public Map<Integer, Integer> MapIntInt;
	@DataJson
	public List<Integer> ListInt;
}
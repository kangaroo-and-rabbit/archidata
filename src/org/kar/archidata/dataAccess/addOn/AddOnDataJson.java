package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;

public class AddOnDataJson implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnDataJson.class);
	
	@Override
	public Class<?> getAnnotationClass() {
		return DataJson.class;
	}
	
	@Override
	public String getSQLFieldType(final Field elem) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(elem);
		return DataFactory.convertTypeInSQL(String.class, fieldName);
	}
	
	@Override
	public boolean isCompatibleField(final Field elem) {
		final DataJson decorators = elem.getDeclaredAnnotation(DataJson.class);
		return decorators != null;
	}
	
	@Override
	public void insertData(final PreparedStatement ps, final Field field, final Object rootObject, final CountInOut iii) throws Exception {
		final Object data = field.get(rootObject);
		if (data == null) {
			ps.setNull(iii.value, Types.VARCHAR);
		}
		final ObjectMapper objectMapper = new ObjectMapper();
		final String dataString = objectMapper.writeValueAsString(data);
		ps.setString(iii.value, dataString);
		iii.inc();
	}
	
	@Override
	public boolean canInsert(final Field field) {
		return true;
	}
	
	@Override
	public boolean canRetrieve(final Field field) {
		return true;
	}
	
	@Override
	public void generateQuerry(@NotNull final String tableName, @NotNull final Field field, @NotNull final StringBuilder querrySelect, @NotNull final StringBuilder querry, @NotNull final String name,
			@NotNull final CountInOut elemCount, final QueryOptions options) throws Exception {
		querrySelect.append(" ");
		querrySelect.append(tableName);
		querrySelect.append(".");
		querrySelect.append(name);
		elemCount.inc();
		return;
	}
	
	@Override
	public void fillFromQuerry(final ResultSet rs, final Field field, final Object data, final CountInOut count, final QueryOptions options, final List<LazyGetter> lazyCall) throws Exception {
		final String jsonData = rs.getString(count.value);
		count.inc();
		if (!rs.wasNull()) {
			final ObjectMapper objectMapper = new ObjectMapper();
			final Object dataParsed = objectMapper.readValue(jsonData, field.getType());
			field.set(data, dataParsed);
		}
	}
	
	@Override
	public void createTables(final String tableName, final Field field, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		DataFactory.createTablesSpecificType(tableName, field, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, JsonValue.class);
	}
}

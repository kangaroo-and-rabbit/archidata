package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

public class AddOnOneToMany implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnOneToMany.class);
	static final String SEPARATOR_LONG = "-";

	/** Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated */
	protected static String getStringOfIds(final List<Long> ids) {
		final List<Long> tmp = new ArrayList<>(ids);
		return tmp.stream().map(String::valueOf).collect(Collectors.joining("-"));
	}

	/** extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list of Long value
	 * @throws SQLException if an error is generated in the sql request. */
	protected static List<Long> getListOfIds(final ResultSet rs, final int iii) throws SQLException {
		final String trackString = rs.getString(iii);
		if (rs.wasNull()) {
			return null;
		}
		final List<Long> out = new ArrayList<>();
		final String[] elements = trackString.split("-");
		for (final String elem : elements) {
			final Long tmp = Long.parseLong(elem);
			out.add(tmp);
		}
		return out;
	}

	@Override
	public Class<?> getAnnotationClass() {
		return OneToMany.class;
	}

	@Override
	public String getSQLFieldType(final Field field) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field);
		try {
			return DataFactory.convertTypeInSQL(Long.class, fieldName);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		return decorators != null;
	}

	@Override
	public void insertData(final PreparedStatement ps, final Field field, final Object rootObject, final CountInOut iii)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		final Object data = field.get(rootObject);
		iii.inc();
		if (data == null) {
			ps.setNull(iii.value, Types.BIGINT);
		} else {
			@SuppressWarnings("unchecked")
			final String dataTmp = getStringOfIds((List<Long>) data);
			ps.setString(iii.value, dataTmp);
		}
	}

	@Override
	public boolean canInsert(final Field field) {
		return false;
	}

	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		if (field.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class) {
			return true;
		}
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return false;
		}
		if (decorators.targetEntity() == objectClass) {
			return true;
		}
		return false;
	}

	@Override
	public void generateQuery(
			@NotNull final String tableName,
			@NotNull final String primaryKey,
			@NotNull final Field field,
			@NotNull final StringBuilder querySelect,
			@NotNull final StringBuilder query,
			@NotNull final String name,
			@NotNull final CountInOut count,
			final QueryOptions options) {
		if (field.getType() != List.class) {
			return;
		}
		// Force a copy of the primaryKey to permit the async retrieve of the data
		querySelect.append(" ");
		querySelect.append(tableName);
		querySelect.append(".");
		querySelect.append(primaryKey);
		querySelect.append(" AS tmp_");
		querySelect.append(Integer.toString(count.value));
	}

	@Override
	public void fillFromQuery(
			final ResultSet rs,
			final Field field,
			final Object data,
			final CountInOut count,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not OneToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}

		Long parentIdTmp = null;
		UUID parendUuidTmp = null;
		try {
			final String modelData = rs.getString(count.value);
			parentIdTmp = Long.valueOf(modelData);
			count.inc();
		} catch (final NumberFormatException ex) {
			final List<UUID> idList = DataAccess.getListOfRawUUIDs(rs, count.value);
			parendUuidTmp = idList.get(0);
			count.inc();
		}
		final Long parentId = parentIdTmp;
		final UUID parendUuid = parendUuidTmp;
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return;
		}
		final String mappingKey = decorators.mappedBy();
		// We get the parent ID ... ==> need to request the list of elements

		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class) {
			LOGGER.error("Need to retreive all primary key of all elements");
			//field.set(data, idList);
			return;
		} else if (objectClass == UUID.class) {
			LOGGER.error("Need to retreive all primary key of all elements");
			//field.set(data, idList);
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				throw new DataAccessException("EAGER is not supported for list of element...");
			} else if (parentId != null) {
				// In the lazy mode, the request is done in asynchronous mode, they will be done after...
				final LazyGetter lambda = () -> {
					@SuppressWarnings("unchecked")
					final Object foreignData = DataAccess.getsWhere(decorators.targetEntity(),
							new Condition(new QueryCondition(mappingKey, "=", parentId)));
					if (foreignData == null) {
						return;
					}
					field.set(data, foreignData);
				};
				lazyCall.add(lambda);
			} else if (parendUuid != null) {
				final LazyGetter lambda = () -> {
					@SuppressWarnings("unchecked")
					final Object foreignData = DataAccess.getsWhere(decorators.targetEntity(),
							new Condition(new QueryCondition(mappingKey, "=", parendUuid)));
					if (foreignData == null) {
						return;
					}
					field.set(data, foreignData);
				};
				lazyCall.add(lambda);
			}
		}
	}

	// TODO : refacto this table to manage a generic table with dynamic name to be serialize with the default system
	@Override
	public void createTables(
			final String tableName,
			final Field primaryField,
			final Field field,
			final StringBuilder mainTableBuilder,
			final List<String> preActionList,
			final List<String> postActionList,
			final boolean createIfNotExist,
			final boolean createDrop,
			final int fieldId) throws Exception {
		// This is a remote field ==> nothing to generate (it is stored in the remote object
	}
}

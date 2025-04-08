package org.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.dataAccess.exportTools.TableQuery;
import org.atriasoft.archidata.dataAccess.exportTools.TableQueryTypes;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.GroupBy;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.atriasoft.archidata.tools.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class DataExport {
	static final Logger LOGGER = LoggerFactory.getLogger(DataExport.class);
	public static final String CSV_TYPE = "text/csv";

	@SuppressWarnings("unchecked")
	protected static RetreiveFromDB createSetValueFromDbCallbackTable(
			final int count,
			final Class<?> type,
			final int id) throws Exception {
		if (type == UUID.class) {
			return (final ResultSet rs, final Object obj) -> {
				final UUID tmp = rs.getObject(count, UUID.class);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Long.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Long tmp = rs.getLong(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == long.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Long tmp = rs.getLong(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Integer.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Integer tmp = rs.getInt(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == int.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Integer tmp = rs.getInt(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Float.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Float tmp = rs.getFloat(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == float.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Float tmp = rs.getFloat(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Double.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Double tmp = rs.getDouble(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == double.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Double tmp = rs.getDouble(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Boolean.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Boolean tmp = rs.getBoolean(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == boolean.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Boolean tmp = rs.getBoolean(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Timestamp.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Timestamp tmp = rs.getTimestamp(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type == Date.class) {
			return (final ResultSet rs, final Object obj) -> {
				try {
					final Timestamp tmp = rs.getTimestamp(count);
					if (!rs.wasNull()) {
						final List<Object> data = (List<Object>) (obj);
						data.set(id, Date.from(tmp.toInstant()));
					}
				} catch (final SQLException ex) {
					final String tmp = rs.getString(count);
					LOGGER.error("Fail to parse the SQL time !!! {}", tmp);
					if (!rs.wasNull()) {
						final Date date = DateTools.parseDate(tmp);
						LOGGER.error("Fail to parse the SQL time !!! {}", date);
						final List<Object> data = (List<Object>) (obj);
						data.set(id, date);
					}
				}
			};
		}
		if (type == Instant.class) {
			return (final ResultSet rs, final Object obj) -> {
				final String tmp = rs.getString(count);
				if (!rs.wasNull()) {
					final Instant date = Instant.parse(tmp);
					LOGGER.error("Fail to parse the SQL time !!! {}", date);
					final List<Object> data = (List<Object>) (obj);
					data.set(id, date);
				}
			};
		}
		if (type == LocalDate.class) {
			return (final ResultSet rs, final Object obj) -> {
				final java.sql.Date tmp = rs.getDate(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp.toLocalDate());
				}
			};
		}
		if (type == LocalTime.class) {
			return (final ResultSet rs, final Object obj) -> {
				final java.sql.Time tmp = rs.getTime(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp.toLocalTime());
				}
			};
		}
		if (type == String.class) {
			return (final ResultSet rs, final Object obj) -> {
				final String tmp = rs.getString(count);
				if (!rs.wasNull()) {
					final List<Object> data = (List<Object>) (obj);
					data.set(id, tmp);
				}
			};
		}
		if (type.isEnum()) {
			return (final ResultSet rs, final Object obj) -> {
				final String tmp = rs.getString(count);
				if (!rs.wasNull()) {
					boolean find = false;
					final Object[] arr = type.getEnumConstants();
					for (final Object elem : arr) {
						if (elem.toString().equals(tmp)) {
							final List<Object> data = (List<Object>) (obj);
							data.set(id, elem);
							find = true;
							break;
						}
					}
					if (!find) {
						throw new DataAccessException("Enum value does not exist in the Model: '" + tmp + "'");
					}
				}
			};
		}
		throw new DataAccessException("Unknown Field Type");

	}

	private static int getQueryPropertyId(final List<TableQueryTypes> properties, final String name)
			throws DataAccessException {
		for (int iii = 0; iii < properties.size(); iii++) {
			if (properties.get(iii).name.equals(name)) {
				return iii;
			}
		}
		throw new DataAccessException("Query with unknown field: '" + name + "'");
	}

	public static TableQuery queryTable(
			final DBAccessSQL ioDb,
			final List<TableQueryTypes> headers,
			final String query,
			final List<Object> parameters,
			final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return queryTable(ioDb, headers, query, parameters, options);
	}

	public static TableQuery queryTable(
			final DBAccessSQL ioDb,
			final List<TableQueryTypes> headers,
			final String queryBase,
			final List<Object> parameters,
			final QueryOptions options) throws Exception {
		final List<LazyGetter> lazyCall = new ArrayList<>();

		final Condition condition = ioDb.conditionFusionOrEmpty(options, false);
		final StringBuilder query = new StringBuilder(queryBase);
		final TableQuery out = new TableQuery(headers);
		// real add in the BDD:
		try {
			final CountInOut count = new CountInOut();
			condition.whereAppendQuery(query, null, options, null);
			final List<GroupBy> groups = options.get(GroupBy.class);
			for (final GroupBy group : groups) {
				group.generateQuery(query, null);
			}
			final List<OrderBy> orders = options.get(OrderBy.class);
			for (final OrderBy order : orders) {
				order.generateQuery(query, null);
			}
			final List<Limit> limits = options.get(Limit.class);
			if (limits.size() == 1) {
				limits.get(0).generateQuery(query, null);
			} else if (limits.size() > 1) {
				throw new DataAccessException("Request with multiple 'limit'...");
			}
			LOGGER.warn("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = ioDb.getConnection().prepareStatement(query.toString(),
					Statement.RETURN_GENERATED_KEYS);
			final CountInOut iii = new CountInOut(1);
			if (parameters != null) {
				for (final Object elem : parameters) {
					ioDb.addElement(ps, elem, iii);
				}
				iii.inc();
			}
			condition.injectQuery(ioDb, ps, iii);
			if (limits.size() == 1) {
				limits.get(0).injectQuery(ioDb, ps, iii);
			}
			// execute the request
			final ResultSet rs = ps.executeQuery();
			final ResultSetMetaData rsmd = rs.getMetaData();
			final List<RetreiveFromDB> actionToRetreive = new ArrayList<>();
			LOGGER.info("Field:");
			for (int jjj = 0; jjj < rsmd.getColumnCount(); jjj++) {
				final String label = rsmd.getColumnLabel(jjj + 1);
				final String typeName = rsmd.getColumnTypeName(jjj + 1);
				final int typeId = rsmd.getColumnType(jjj + 1);
				final int id = getQueryPropertyId(headers, label);
				LOGGER.info("    - {}:{} type=[{}] {}  REQUEST={}", jjj, label, typeId, typeName,
						headers.get(id).type.getCanonicalName());
				// create the callback...
				final RetreiveFromDB element = createSetValueFromDbCallbackTable(jjj + 1, headers.get(id).type, id);
				actionToRetreive.add(element);
			}
			while (rs.next()) {
				count.value = 1;
				final List<Object> data = Arrays.asList(new Object[headers.size()]);
				for (final RetreiveFromDB action : actionToRetreive) {
					action.doRequest(rs, data);
				}
				out.values.add(data);
			}
			LOGGER.info("Async calls: {}", lazyCall.size());
			for (final LazyGetter elem : lazyCall) {
				elem.doRequest();
			}
		} catch (final SQLException ex) {
			ex.printStackTrace();
			throw ex;
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return out;
	}

	public static String CSVReplaceSeparator(final String data, final String separator) {
		if (data == null) {
			return "";
		}
		final String separatorLine = "\n";
		final String separatorLineReplace = "\\n";
		return data.replaceAll(separator, "__SEP__").replaceAll(separatorLine, separatorLineReplace);
	}

	public static String tableToCSV(final TableQuery data) {
		final String separator = ";";
		final StringBuilder out = new StringBuilder();
		// generate header:
		boolean first = true;
		for (final TableQueryTypes elem : data.headers) {
			if (!first) {
				out.append(separator);
			} else {
				first = false;
			}
			out.append(CSVReplaceSeparator(elem.title, separator));
		}
		out.append("\n");
		// generate body:
		first = true;
		for (final List<Object> line : data.values) {
			for (final Object elem : line) {
				if (!first) {
					out.append(separator);
				} else {
					first = false;
				}
				if (elem != null) {
					out.append(CSVReplaceSeparator(elem.toString(), separator));
				}
			}
			out.append("\n");
		}
		return out.toString();
	}

	public static Response convertInResponse(final TableQuery dataOut, final String accept)
			throws DataAccessException, IOException {
		if (CSV_TYPE.equals(accept)) {
			// CSV serialization
			String out = null;
			try {
				out = DataExport.tableToCSV(dataOut);
			} catch (final Exception e) {
				LOGGER.error("Fail to generate CSV....");
				e.printStackTrace();
				throw new DataAccessException("Fail in CSV convertion data");
			}
			return Response.ok(out).header("Content-Type", CSV_TYPE).build();
		}
		if (MediaType.APPLICATION_JSON.equals(accept)) {
			LOGGER.info("Start mapping josn");
			final ObjectMapper objectMapper = ContextGenericTools.createObjectMapper();
			LOGGER.info("Start find modules josn");
			objectMapper.findAndRegisterModules();
			LOGGER.info("Start map object");
			String out;
			try {
				out = objectMapper.writeValueAsString(dataOut);
			} catch (final JsonProcessingException e) {
				LOGGER.error("Fail to generate JSON....");
				e.printStackTrace();
				throw new DataAccessException("Fail in JSON convertion data");
			}
			LOGGER.info("generate response");
			return Response.ok(out).header("Content-Type", MediaType.APPLICATION_JSON).build();
		}
		throw new IOException("This type is not managed: '" + accept + "'");
	}

}

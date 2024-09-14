package org.kar.archidata.dataAccess;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataIfNotExists;
import org.kar.archidata.annotation.UpdateTimestamp;
import org.kar.archidata.dataAccess.options.CreateDropTable;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.GenerationType;

public class DataFactory {
	static final Logger LOGGER = LoggerFactory.getLogger(DataFactory.class);

	public static String convertTypeInSQL(final Class<?> type, final String fieldName) throws DataAccessException {
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			if (type == UUID.class) {
				return "binary(16)";
			}
			if (type == Long.class || type == long.class) {
				return "bigint";
			}
			if (type == Integer.class || type == int.class) {
				return "int";
			}
			if (type == Boolean.class || type == boolean.class) {
				return "tinyint(1)";
			}
			if (type == Float.class || type == float.class) {
				return "float";
			}
			if (type == Double.class || type == double.class) {
				return "double";
			}
			if (type == Instant.class) {
				return "varchar(33)";
			}
			if (type == Date.class || type == Timestamp.class) {
				return "timestamp(3)";
			}
			if (type == LocalDate.class) {
				return "date";
			}
			if (type == LocalTime.class) {
				return "time";
			}
			if (type == String.class) {
				return "text";
			}
			if (type == JsonValue.class) {
				return "json";
			}
			if (type.isEnum()) {
				final Object[] arr = type.getEnumConstants();
				final StringBuilder out = new StringBuilder();
				out.append("ENUM(");
				boolean first = true;
				for (final Object elem : arr) {
					if (!first) {
						out.append(",");
					}
					first = false;
					out.append("'");
					out.append(elem.toString());
					out.append("'");
				}
				out.append(")");
				return out.toString();
			}
		} else {
			if (type == UUID.class) {
				return "BINARY(16)";
			}
			if (type == Long.class || type == long.class) {
				return "INTEGER";
			}
			if (type == Integer.class || type == int.class) {
				return "INTEGER";
			}
			if (type == Boolean.class || type == boolean.class) {
				return "INTEGER";
			}
			if (type == Float.class || type == float.class) {
				return "REAL";
			}
			if (type == Double.class || type == double.class) {
				return "REAL";
			}
			if (type == Instant.class) {
				return "text";
			}
			if (type == Date.class || type == Timestamp.class) {
				return "DATETIME";
			}
			if (type == LocalDate.class) {
				return "DATE";
			}
			if (type == LocalTime.class) {
				return "TIME";
			}
			if (type == String.class) {
				return "text";
			}
			if (type == JsonValue.class) {
				return "text";
			}
			if (type.isEnum()) {
				final Object[] arr = type.getEnumConstants();
				final StringBuilder out = new StringBuilder();
				out.append("TEXT CHECK(");
				out.append(fieldName);
				out.append(" IN (");
				boolean first = true;
				for (final Object elem : arr) {
					if (!first) {
						out.append(",");
					}
					first = false;
					out.append("'");
					out.append(elem.toString());
					out.append("'");
				}
				out.append(" ) )");
				return out.toString();
			}
		}
		throw new DataAccessException("Imcompatible type of element in object for: " + type.getCanonicalName());
	}

	public static void createTablesSpecificType(
			final String tableName,
			final Field primaryField,
			final Field elem,
			final StringBuilder mainTableBuilder,
			final List<String> preOtherTables,
			final List<String> postOtherTables,
			final boolean createIfNotExist,
			final boolean createDrop,
			final int fieldId,
			final Class<?> classModel) throws Exception {
		final String name = AnnotationTools.getFieldName(elem);
		final int limitSize = AnnotationTools.getLimitSize(elem);
		final boolean notNull = AnnotationTools.getColumnNotNull(elem);

		final boolean primaryKey = AnnotationTools.isPrimaryKey(elem);
		final GenerationType strategy = AnnotationTools.getStrategy(elem);

		final boolean createTime = elem.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
		final boolean updateTime = elem.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
		final String comment = AnnotationTools.getComment(elem);
		final String defaultValue = AnnotationTools.getDefault(elem);

		if (mainTableBuilder.toString().length() == 0) {
			mainTableBuilder.append("\n\t\t`");
		} else {
			mainTableBuilder.append(",\n\t\t`");
		}
		mainTableBuilder.append(name);
		mainTableBuilder.append("` ");
		String typeValue = null;
		typeValue = convertTypeInSQL(classModel, name);
		if ("text".equals(typeValue) && !"sqlite".equals(ConfigBaseVariable.getDBType())) {
			if (limitSize > 0) {
				mainTableBuilder.append("varchar(");
				mainTableBuilder.append(limitSize);
				mainTableBuilder.append(")");
			} else {
				mainTableBuilder.append("text");
				if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
					mainTableBuilder.append(" CHARACTER SET utf8");
				}
			}
		} else {
			mainTableBuilder.append(typeValue);
		}
		mainTableBuilder.append(" ");
		if (notNull) {
			if (!primaryKey || !"sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) {
				mainTableBuilder.append("NOT NULL ");
			}
			if (defaultValue == null) {
				if (updateTime || createTime) {
					if ("varchar(33)".equals(typeValue)) {
						mainTableBuilder.append("DEFAULT DATE_FORMAT(now(6), '%Y-%m-%dT%H:%m:%s.%fZ')");
					} else {
						mainTableBuilder.append("DEFAULT CURRENT_TIMESTAMP");
						if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
							mainTableBuilder.append("(3)");
						}
					}
					mainTableBuilder.append(" ");
				}
				if (updateTime) {
					if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
						if ("varchar(33)".equals(typeValue)) {
							mainTableBuilder.append("ON UPDATE DATE_FORMAT(now(6), '%Y-%m-%dT%H:%m:%s.%fZ')");
						} else {
							mainTableBuilder.append("ON UPDATE CURRENT_TIMESTAMP");
							mainTableBuilder.append("(3)");
						}
					} else {
						// TODO: add trigger:
						/* CREATE TRIGGER your_table_trig AFTER UPDATE ON your_table BEGIN update your_table SET updated_on = datetime('now') WHERE user_id = NEW.user_id; END; */
						final StringBuilder triggerBuilder = new StringBuilder();
						triggerBuilder.append("CREATE TRIGGER ");
						triggerBuilder.append(tableName);
						triggerBuilder.append("_update_trigger AFTER UPDATE ON ");
						triggerBuilder.append(tableName);
						triggerBuilder.append(" \nBEGIN \n    update ");
						triggerBuilder.append(tableName);
						triggerBuilder.append(" SET ");
						triggerBuilder.append(name);
						// triggerBuilder.append(" = datetime('now') WHERE id = NEW.id; \n");
						final String tablePrimaryKey = primaryField.getName();
						if ("varchar(33)".equals(typeValue)) {
							triggerBuilder.append(" = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE " + tablePrimaryKey
									+ " = NEW." + tablePrimaryKey + "; \n");
						} else {
							triggerBuilder.append(" = strftime('%Y-%m-%d %H:%M:%f', 'now') WHERE " + tablePrimaryKey
									+ " = NEW." + tablePrimaryKey + "; \n");
						}
						triggerBuilder.append("END;");

						postOtherTables.add(triggerBuilder.toString());
					}

					mainTableBuilder.append(" ");
				}
			} else {
				mainTableBuilder.append("DEFAULT ");
				if ("CURRENT_TIMESTAMP(3)".equals(defaultValue) && "sqlite".equals(ConfigBaseVariable.getDBType())) {
					mainTableBuilder.append("CURRENT_TIMESTAMP");
				} else {
					mainTableBuilder.append(defaultValue);
				}
				mainTableBuilder.append(" ");
				if (updateTime) {
					if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
						mainTableBuilder.append("ON UPDATE CURRENT_TIMESTAMP");
						mainTableBuilder.append("(3)");
					}
					mainTableBuilder.append(" ");
				}
			}
		} else if (defaultValue == null) {
			if (updateTime || createTime) {
				if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
					mainTableBuilder.append("DEFAULT CURRENT_TIMESTAMP ");
				} else {
					mainTableBuilder.append("DEFAULT CURRENT_TIMESTAMP(3) ");
				}
			} else if (primaryKey) {
				mainTableBuilder.append("NOT NULL ");
			} else {
				mainTableBuilder.append("DEFAULT NULL ");
			}
		} else {
			mainTableBuilder.append("DEFAULT ");
			mainTableBuilder.append(defaultValue);
			mainTableBuilder.append(" ");

		}
		if (primaryKey && "sqlite".equals(ConfigBaseVariable.getDBType())) {
			mainTableBuilder.append("PRIMARY KEY ");

		}

		if (strategy == GenerationType.IDENTITY) {
			if ("binary(16)".equals(typeValue)) {

			} else if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
				mainTableBuilder.append("AUTO_INCREMENT ");
			} else {
				mainTableBuilder.append("AUTOINCREMENT ");
			}
		} else if (strategy != null) {
			throw new DataAccessException("Can not generate a stategy different of IDENTITY");
		}

		if (comment != null && !"sqlite".equals(ConfigBaseVariable.getDBType())) {
			mainTableBuilder.append("COMMENT '");
			mainTableBuilder.append(comment.replace('\'', '\''));
			mainTableBuilder.append("' ");
		}
	}

	private static boolean isFieldFromSuperClass(final Class<?> model, final String filedName) {
		final Class<?> superClass = model.getSuperclass();
		if (superClass == null) {
			return false;
		}
		for (final Field field : superClass.getFields()) {
			String name;
			try {
				name = AnnotationTools.getFieldName(field);
				if (filedName.equals(name)) {
					return true;
				}
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				LOGGER.trace("Catch error field name in parent create data table: {}", e.getMessage());
			}
		}
		return false;
	}

	public static List<String> createTable(final Class<?> clazz) throws Exception {
		return createTable(clazz, null);
	}

	public static List<String> createTable(final Class<?> clazz, final QueryOptions options) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz, options);

		boolean createDrop = false;
		if (options != null) {
			createDrop = options.exist(CreateDropTable.class);
		}

		final boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(DataIfNotExists.class).length != 0;
		final List<String> preActionList = new ArrayList<>();
		final List<String> postActionList = new ArrayList<>();
		final StringBuilder out = new StringBuilder();
		// Drop Table
		if (createIfNotExist && createDrop) {
			final StringBuilder tableTmp = new StringBuilder();
			tableTmp.append("DROP TABLE IF EXISTS `");
			tableTmp.append(tableName);
			tableTmp.append("`;");
			postActionList.add(tableTmp.toString());
		}
		// create Table:
		out.append("CREATE TABLE `");
		out.append(tableName);
		out.append("` (");
		int fieldId = 0;
		LOGGER.debug("===> TABLE `{}`", tableName);
		final List<String> primaryKeys = new ArrayList<>();

		final Field primaryField = AnnotationTools.getPrimaryKeyField(clazz);
		for (final Field elem : clazz.getFields()) {
			// DEtect the primary key (support only one primary key right now...
			if (AnnotationTools.isPrimaryKey(elem)) {
				primaryKeys.add(AnnotationTools.getFieldName(elem));
			}
		}
		// Here we insert the data in the reverse mode ==> the parent class add there parameter at the start (we reorder the field with the parenting).
		StringBuilder tmpOut = new StringBuilder();
		StringBuilder reverseOut = new StringBuilder();
		final List<String> alreadyAdded = new ArrayList<>();
		Class<?> currentClazz = clazz;
		final Field tablePrimaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
		while (currentClazz != null) {
			fieldId = 0;
			LOGGER.trace("parse class: '{}'", currentClazz.getCanonicalName());
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				final String dataName = AnnotationTools.getFieldName(elem);
				if (isFieldFromSuperClass(currentClazz, dataName)) {
					LOGGER.trace("        SKIP:  '{}'", elem.getName());
					continue;
				}
				if (alreadyAdded.contains(dataName)) {
					LOGGER.trace("        SKIP2: '{}'", elem.getName());
					continue;
				}
				alreadyAdded.add(dataName);
				LOGGER.trace("        + '{}'", elem.getName());
				if (DataAccess.isAddOnField(elem)) {
					final DataAccessAddOn addOn = DataAccess.findAddOnforField(elem);
					LOGGER.trace("Create type for: {} ==> {} (ADD-ON)", AnnotationTools.getFieldName(elem),
							elem.getType());
					if (addOn != null) {
						addOn.createTables(tableName, primaryField, elem, tmpOut, preActionList, postActionList,
								createIfNotExist, createDrop, fieldId);
					} else {
						throw new DataAccessException("Element matked as add-on but add-on does not loaded: table:"
								+ tableName + " field name=" + AnnotationTools.getFieldName(elem) + " type="
								+ elem.getType());
					}
				} else {
					LOGGER.trace("Create type for: {} ==> {}", AnnotationTools.getFieldName(elem), elem.getType());
					DataFactory.createTablesSpecificType(tableName, tablePrimaryKeyField, elem, tmpOut, preActionList,
							postActionList, createIfNotExist, createDrop, fieldId, elem.getType());
				}
				fieldId++;
			}
			final boolean dataInThisObject = tmpOut.toString().length() > 0;
			if (dataInThisObject) {
				LOGGER.info("Previous Object : '{}'", reverseOut.toString());
				final boolean dataInPreviousObject = reverseOut.toString().length() > 0;
				if (dataInPreviousObject) {
					tmpOut.append(", ");
					tmpOut.append(reverseOut.toString());
				}
				reverseOut = tmpOut;
				tmpOut = new StringBuilder();
			}
			currentClazz = currentClazz.getSuperclass();
			if (currentClazz == Object.class) {
				break;
			}
		}
		out.append(reverseOut.toString());
		if (primaryKeys.size() != 0 && !"sqlite".equals(ConfigBaseVariable.getDBType())) {
			out.append(",\n\tPRIMARY KEY (`");
			for (int iii = 0; iii < primaryKeys.size(); iii++) {
				if (iii != 0) {
					out.append(",");
				}
				out.append(primaryKeys.get(iii));
			}
			out.append("`)");
		}
		out.append("\n\t)");
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			out.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
		}
		out.append(";");
		preActionList.add(out.toString());
		preActionList.addAll(postActionList);
		return preActionList;
	}

}
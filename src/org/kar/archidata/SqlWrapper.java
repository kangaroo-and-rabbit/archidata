package org.kar.archidata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.kar.archidata.annotation.SQLAutoIncrement;
import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.SQLLimitSize;
import org.kar.archidata.annotation.SQLNotNull;
import org.kar.archidata.annotation.SQLNotRead;
import org.kar.archidata.annotation.SQLPrimaryKey;
import org.kar.archidata.annotation.SQLTableLinkGeneric;
import org.kar.archidata.annotation.SQLTableLinkGeneric.ModelLink;
import org.kar.archidata.annotation.SQLTableName;
import org.kar.archidata.annotation.SQLUpdateTime;
import org.kar.archidata.db.DBEntry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.kar.archidata.annotation.SQLCreateTime;
import org.kar.archidata.annotation.SQLDefault;


public class SqlWrapper {
	
	public static class ExceptionDBInterface extends Exception {
		private static final long serialVersionUID = 1L;
		public int errorID;
		ExceptionDBInterface(int errorId, String message) {
			super(message);
			this.errorID = errorId;
		}
	}
	
	public SqlWrapper() {
		
	}
	
	public static String convertTypeInSQL(Class<?> type) throws Exception {
		if (type == Long.class || type == long.class ) {
			return "bigint";
		}
		if (type == Integer.class || type == int.class ) {
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
		if (type == Timestamp.class) {
			return "timestamp(3)";
		}
		if (type == Date.class) {
			return "date";
		}
		if (type == String.class) {
			return "text";
		}
		throw new Exception("Imcompatible type of element in object for: " + type.getCanonicalName());
	}

	protected static <T> void setValuedb(Class<?> type, T data, int index, Field field, PreparedStatement ps) throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (type == Long.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.BIGINT);
            } else {
            	ps.setLong(index++, (Long)tmp);
            }
		} else if (type == long.class ) {
			ps.setLong(index++, field.getLong(data));
		} else if (type == Integer.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.INTEGER);
            } else {
            	ps.setInt(index++, (Integer)tmp);
            }
		} else if (type == int.class ) {
			ps.setInt(index++, field.getInt(data));
		} else if (type == Float.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.FLOAT);
            } else {
            	ps.setFloat(index++, (Float)tmp);
            }
		} else if (type == float.class) {
			ps.setFloat(index++, field.getFloat(data));
		} else if (type == Double.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.DOUBLE);
            } else {
            	ps.setDouble(index++, (Double)tmp);
            }
		} else if (type == Double.class) {
			ps.setDouble(index++, field.getDouble(data));
		} else if (type == Boolean.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.INTEGER);
            } else {
            	ps.setBoolean(index++, (Boolean)tmp);
            }
		} else if (type == boolean.class) {
			ps.setBoolean(index++, field.getBoolean(data));
		} else if (type == Timestamp.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.INTEGER);
            } else {
            	ps.setTimestamp(index++, (Timestamp)tmp);
            }
		} else if (type == Date.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.INTEGER);
            } else {
            	ps.setDate(index++, (Date)tmp);
            }
		} else if (type == String.class) {
			Object tmp = field.get(data);
            if (tmp == null) {
            	ps.setNull(index++, Types.VARCHAR);
            } else {
            	ps.setString(index++, (String)tmp);
            }
		}
	}
	protected static <T> void setValueFromDb(Class<?> type, T data, int index, Field field, ResultSet rs) throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (type == Long.class) {
			Long tmp = rs.getLong(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				//System.out.println("       ==> " + tmp);
				field.set(data, tmp);
			}
		} else if (type == long.class ) {
			Long tmp = rs.getLong(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setLong(data, tmp);
			}
		} else if (type == Integer.class) {
			Integer tmp = rs.getInt(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == int.class ) {
			Integer tmp = rs.getInt(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setInt(data, tmp);
			}
		} else if (type == Float.class) {
			Float tmp = rs.getFloat(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == float.class) {
			Float tmp = rs.getFloat(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setFloat(data, tmp);
			}
		} else if (type == Double.class) {
			Double tmp = rs.getDouble(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == double.class) {
			Double tmp = rs.getDouble(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setDouble(data, tmp);
			}
		} else if (type == Boolean.class) {
			Boolean tmp = rs.getBoolean(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == boolean.class) {
			Boolean tmp = rs.getBoolean(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setBoolean(data, tmp);
			}
		} else if (type == Timestamp.class) {
			Timestamp tmp = rs.getTimestamp(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == Date.class) {
			Date tmp = rs.getDate(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == String.class) {
			String tmp = rs.getString(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		}
	}
	public static ModelLink getLinkMode(Field elem) {
		SQLTableLinkGeneric[] decorators = elem.getDeclaredAnnotationsByType(SQLTableLinkGeneric.class);
		if (decorators == null || decorators.length == 0) {
			return SQLTableLinkGeneric.ModelLink.NONE;
		}
		return decorators[0].value();
	}
	
	public static <T> T insert(T data) throws Exception {
		Class<?> clazz = data.getClass();
		//public static NodeSmall createNode(String typeInNode, String name, String descrition, Long parentId) {
		
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        // real add in the BDD:
        try {
        	String tableName = getTableName(clazz);
        	boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
        	StringBuilder query = new StringBuilder();
        	query.append("INSERT INTO ");
        	query.append(tableName);
        	query.append(" (");

   		 	boolean firstField = true;
   		 	int count = 0;
   		 	for (Field elem : clazz.getFields()) {
   		 		boolean primaryKey = elem.getDeclaredAnnotationsByType(SQLPrimaryKey.class).length != 0;
				if (primaryKey) {
					continue;
				}
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric == ModelLink.EXTERNAL) {
					continue;
				}
				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
				if (createTime) {
					continue;
				}
				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
				if (updateTime) {
					continue;
				}
				if (!elem.getClass().isPrimitive()) {
					Object tmp = elem.get(data);
					if(tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
						continue;
					}
				}
				count++;
				String name = elem.getName();
				if (firstField) {
		        	firstField = false;
				} else {
		        	query.append(",");
				}
		        query.append(" `");
		        query.append(name);
		        query.append("`");
   		 	}
   		 	firstField = true;
	        query.append(") VALUES (");
   		 	for (int iii = 0; iii<count; iii++) {
				if (firstField) {
		        	firstField = false;
				} else {
		        	query.append(",");
				}
				query.append("?");
   		 	}
   		    query.append(")");
   		    System.out.println("generate the querry: '" + query.toString() + "'");
            // prepare the request:
            PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            Field primaryKeyField = null;
            int iii = 1;
   		 	for (Field elem : clazz.getFields()) {
   		 		boolean primaryKey = elem.getDeclaredAnnotationsByType(SQLPrimaryKey.class).length != 0;
				if (primaryKey) {
					primaryKeyField = elem;
					continue;
				}
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric == ModelLink.EXTERNAL) {
					continue;
				}
				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
				if (createTime) {
					continue;
				}
				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
				if (updateTime) {
					continue;
				}
				if (linkGeneric == ModelLink.NONE) {
					Class<?> type = elem.getType();
					if (!type.isPrimitive()) {
						Object tmp = elem.get(data);
						if(tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, iii++, elem, ps);
				} else {
					// transform the data in string to insert it ...
					Object tmp = elem.get(data);
		            if (tmp == null) {
		            	ps.setNull(iii++, Types.BIGINT);
		            } else {
		            	@SuppressWarnings("unchecked")
						String dataTmp = getStringOfIds((List<Long>)tmp);
		            	ps.setString(iii++, dataTmp);
		            }
				}
				count++;
   		 	}
            // execute the request
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating node failed, no rows affected.");
            }
            Long uniqueSQLID = null;
            // retreive uid inserted
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    uniqueSQLID = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating node failed, no ID obtained (1).");
                }
            } catch (Exception ex) {
                System.out.println("Can not get the UID key inserted ... ");
                ex.printStackTrace();
                throw new SQLException("Creating node failed, no ID obtained (2).");
            }
            if (primaryKeyField != null) {
            	if (primaryKeyField.getType() == Long.class) {
            		primaryKeyField.set(data, (Long)uniqueSQLID);
            	} else if (primaryKeyField.getType() == long.class) {
            		primaryKeyField.setLong(data, uniqueSQLID);
            	} else {
            		System.out.println("Can not manage the primary filed !!!");
            	}
            }
            //ps.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
	}

	public static <T> T insertWithJson(Class<T> clazz, String jsonData) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // parse the object to be sure the data are valid:
        T data = mapper.readValue(jsonData, clazz);
        return insert(data);
	}
    
	public static <T> void update(Class<T> clazz, long id, String jsonData) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // parse the object to be sure the data are valid:
        T data = mapper.readValue(jsonData, clazz);
        // Read the tree to filter injection of data:
        JsonNode root = mapper.readTree(jsonData);
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = root.fieldNames();
        iterator.forEachRemaining(e -> keys.add(e));
        update(data, id, keys);
	}
	
	public static <T> void update(T data, long id, List<String> filterValue) throws Exception {
		Class<?> clazz = data.getClass();
		//public static NodeSmall createNode(String typeInNode, String name, String description, Long parentId) {
		
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        // real add in the BDD:
        try {
        	String tableName = getTableName(clazz);
        	boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
        	StringBuilder query = new StringBuilder();
        	query.append("UPDATE ");
        	query.append(tableName);
        	query.append(" SET ");

   		 	boolean firstField = true;
            Field primaryKeyField = null;
   		 	int count = 0;
   		 	for (Field elem : clazz.getFields()) {
   		 		boolean primaryKey = elem.getDeclaredAnnotationsByType(SQLPrimaryKey.class).length != 0;
				if (primaryKey) {
					primaryKeyField = elem;
					continue;
				}
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric == ModelLink.EXTERNAL) {
					continue;
				}
				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
				if (createTime) {
					continue;
				}
				String name = elem.getName();
				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
				if (! updateTime && !filterValue.contains(name)) {
					continue;
				}
				if (!elem.getClass().isPrimitive()) {
					Object tmp = elem.get(data);
					if(tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
						continue;
					}
				}
				count++;
				if (firstField) {
		        	firstField = false;
				} else {
		        	query.append(",");
				}
		        query.append(" `");
		        query.append(name);
		        query.append("` = ");
		        if (updateTime) {
			        query.append(" now(3) ");
		        } else {
		        	query.append("? ");
		        }
   		 	}
   		 	query.append(" WHERE `");
   			query.append(primaryKeyField.getName());
   			query.append("` = ?");
   		 	firstField = true;
   		    System.out.println("generate the querry: '" + query.toString() + "'");
            // prepare the request:
            PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            int iii = 1;
   		 	for (Field elem : clazz.getFields()) {
   		 		boolean primaryKey = elem.getDeclaredAnnotationsByType(SQLPrimaryKey.class).length != 0;
				if (primaryKey) {
					continue;
				}
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric == ModelLink.EXTERNAL) {
					continue;
				}
				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
				if (createTime) {
					continue;
				}
				String name = elem.getName();
				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
				if (updateTime || !filterValue.contains(name)) {
					continue;
				}
				if (linkGeneric == ModelLink.NONE) {
					Class<?> type = elem.getType();
					if (!type.isPrimitive()) {
						Object tmp = elem.get(data);
						if(tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, iii++, elem, ps);
				} else {
					// transform the data in string to insert it ...
					Object tmp = elem.get(data);
		            if (tmp == null) {
		            	ps.setNull(iii++, Types.BIGINT);
		            } else {
		            	@SuppressWarnings("unchecked")
						String dataTmp = getStringOfIds((List<Long>)tmp);
		            	ps.setString(iii++, dataTmp);
		            }
				}
				count++;
   		 	}
   		 	ps.setLong(iii++, id);
            // execute the request
            int affectedRows = ps.executeUpdate();
            // todo manage the list of element is valid ...
            //ps.execute();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
	}

	static void addElement(PreparedStatement ps, Object value, int iii) throws Exception {
	    if (value.getClass() == Long.class) {
		 		ps.setLong(iii, (Long)value);
		} else if (value.getClass() == Integer.class) {
		 		ps.setInt(iii, (Integer)value);
		} else if (value.getClass() == String.class) {
		 		ps.setString(iii, (String)value);
		} else if (value.getClass() == Short.class) {
		 		ps.setShort(iii, (Short)value);
		} else if (value.getClass() == Byte.class) {
		 		ps.setByte(iii, (Byte)value);
		} else if (value.getClass() == Float.class) {
		 		ps.setFloat(iii, (Float)value);
		} else if (value.getClass() == Double.class) {
		 		ps.setDouble(iii, (Double)value);
		} else if (value.getClass() == Boolean.class) {
		 		ps.setBoolean(iii, (Boolean)value);
		} else if (value.getClass() == Boolean.class) {
		 		ps.setBoolean(iii, (Boolean)value);
		} else if (value.getClass() == Timestamp.class) {
		 		ps.setTimestamp(iii, (Timestamp)value);
		} else if (value.getClass() == Date.class) {
		 		ps.setDate(iii, (Date)value);
		} else {
			throw new Exception("Not manage type ==> need to add it ...");
		}
	}
	
	public static <T> T getWith(Class<T> clazz, String key, String value) throws Exception {
		return getWhere(clazz, key, "=", value);
	}

	public static <T> T getWhere(Class<T> clazz, String key, String operator, Object value ) throws Exception {
		return getWhere(clazz, key, operator, value, null, null, null);
	}
	public static <T> T getWhere(Class<T> clazz, String key, String operator, Object value, String key2, String operator2, Object value2 ) throws Exception {
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        T out = null;
        // real add in the BDD:
        try {
        	String tableName = getTableName(clazz);
        	boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
        	StringBuilder query = new StringBuilder();
        	query.append("SELECT ");
        	//query.append(tableName);
        	//query.append(" SET ");

   		 	boolean firstField = true;
   		 	int count = 0;
   		 	for (Field elem : clazz.getFields()) {
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric != ModelLink.NONE) {
					continue;
				}
				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
				if (createTime) {
					continue;
				}
				String name = elem.getName();
				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
				if (updateTime) {
					continue;
				}
				count++;
				if (firstField) {
		        	firstField = false;
				} else {
		        	query.append(",");
				}
		        query.append(" ");
		        query.append(tableName);
		        query.append(".");
		        
		        query.append(name);
   		 	}
   			query.append(" FROM `");
	        query.append(tableName);
   			query.append("` ");
   		 	query.append(" WHERE ");
	        query.append(tableName);
	        query.append(".");
   			query.append(key);
   			query.append(" ");
   			query.append(operator);
   			query.append(" ?");
   			if (key2 != null) {
   		        query.append(" AND ");
   		        query.append(tableName);
   		        query.append(".");
   	   			query.append(key2);
   	   			query.append(" ");
   	   			query.append(operator2);
   	   			query.append(" ?");
   			}
   			/*
   			query.append(" AND ");
	        query.append(tableName);
   			query.append(".deleted = false ");
   			*/
   		 	firstField = true;
   		    System.out.println("generate the querry: '" + query.toString() + "'");
            // prepare the request:
            PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            int iii = 1;
            addElement(ps, value, iii++);
   			if (key2 != null) {
                addElement(ps, value2, iii++);
   			}
            // execute the request
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
            	Object data = clazz.getConstructors()[0].newInstance();
            	count = 1;
       		 	for (Field elem : clazz.getFields()) {
    				ModelLink linkGeneric = getLinkMode(elem);
    				if (linkGeneric != ModelLink.NONE) {
    					continue;
    				}
    				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
    				if (createTime) {
    					continue;
    				}
    				String name = elem.getName();
    				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
    				if (updateTime) {
    					continue;
    				}
    	            //this.name = rs.getString(iii++);
    	            //this.description = rs.getString(iii++);
    	           	//this.covers = getListOfIds(rs, iii++);
    	           	setValueFromDb(elem.getType(), data, count, elem, rs);
    				count++;
       		 	}
				out = (T)data;
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        entry.disconnect();
        entry = null;
        return out;
	}
	public static <T> List<T> getsWhere(Class<T> clazz, String key, String operator, Object value ) throws Exception {
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        List<T> outs = new ArrayList<>();
        // real add in the BDD:
        try {
        	String tableName = getTableName(clazz);
        	boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
        	StringBuilder query = new StringBuilder();
        	query.append("SELECT ");
        	//query.append(tableName);
        	//query.append(" SET ");

   		 	boolean firstField = true;
   		 	int count = 0;
   		 	for (Field elem : clazz.getFields()) {
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric != ModelLink.NONE) {
					continue;
				}
				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
				if (createTime) {
					continue;
				}
				String name = elem.getName();
				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
				if (updateTime) {
					continue;
				}
				count++;
				if (firstField) {
		        	firstField = false;
				} else {
		        	query.append(",");
				}
		        query.append(" ");
		        query.append(tableName);
		        query.append(".");
		        
		        query.append(name);
   		 	}
   			query.append(" FROM `");
	        query.append(tableName);
   			query.append("` ");
   		 	query.append(" WHERE ");
	        query.append(tableName);
	        query.append(".");
   			query.append(key);
   			query.append(" ");
   			query.append(operator);
   			query.append(" ?");
   			/*
   			query.append(" AND ");
	        query.append(tableName);
   			query.append(".deleted = false ");
   			*/
   		 	firstField = true;
   		    System.out.println("generate the querry: '" + query.toString() + "'");
            // prepare the request:
            PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
            int iii = 1;
            addElement(ps, value, iii++);
            // execute the request
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
            	Object data = clazz.getConstructors()[0].newInstance();
            	count = 1;
       		 	for (Field elem : clazz.getFields()) {
    				ModelLink linkGeneric = getLinkMode(elem);
    				if (linkGeneric != ModelLink.NONE) {
    					continue;
    				}
    				boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
    				if (createTime) {
    					continue;
    				}
    				String name = elem.getName();
    				boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
    				if (updateTime) {
    					continue;
    				}
    	            //this.name = rs.getString(iii++);
    	            //this.description = rs.getString(iii++);
    	           	//this.covers = getListOfIds(rs, iii++);
    	           	setValueFromDb(elem.getType(), data, count, elem, rs);
    				count++;
       		 	}
       		 	T out = (T)data;
				outs.add(out);
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        entry.disconnect();
        entry = null;
        return outs;
	}
	
	public static <T> T get(Class<T> clazz, long id) throws Exception {
        Field primaryKeyField = null;
	 	for (Field elem : clazz.getFields()) {
	 		boolean primaryKey = elem.getDeclaredAnnotationsByType(SQLPrimaryKey.class).length != 0;
			if (primaryKey) {
				primaryKeyField = elem;
			}
	 	}
		if (primaryKeyField != null) {
			return getWhere(clazz, primaryKeyField.getName(), "=",  id);
		}
		throw new Exception("Missing primary Key...");
	}
	
	private enum StateLoad {
		DISABLE,
		NORMAL,
		ARRAY
	};
	
	public static String getCurrentTimeStamp() {
		 return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
	}
	
	public static <T> List<T> gets(Class<T> clazz, boolean fool) throws Exception {
		System.out.println("request get " + clazz.getCanonicalName() + " start @" + getCurrentTimeStamp());
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        List<T> out = new ArrayList<>();
        // real add in the BDD:
        try {
        	String tableName = getTableName(clazz);
        	boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
        	StringBuilder query = new StringBuilder();
        	query.append("SELECT ");
        	//query.append(tableName);
        	//query.append(" SET ");

   		 	boolean firstField = true;
   		 	int count = 0;
   		 	StateLoad[] autoClasify = new StateLoad[clazz.getFields().length];
   		 	int indexAutoClasify = 0; 
   		 	for (Field elem : clazz.getFields()) {
				
				boolean notRead = elem.getDeclaredAnnotationsByType(SQLNotRead.class).length != 0;
				if (!fool && notRead) {
					autoClasify[indexAutoClasify++] = StateLoad.DISABLE;
					continue;
				}
				String name = elem.getName();
				count++;
				if (firstField) {
		        	firstField = false;
				} else {
		        	query.append(",");
				}
				ModelLink linkGeneric = getLinkMode(elem);
				if (linkGeneric == ModelLink.EXTERNAL) {
					autoClasify[indexAutoClasify++] = StateLoad.ARRAY;
					String localName = name;
					if (name.endsWith("s")) {
						localName = name.substring(0, name.length()-1);
					}
					String tmpVariable = "tmp_" + Integer.toString(count);
					query.append(" (SELECT GROUP_CONCAT(");
					query.append(tmpVariable);
					query.append(".");
					query.append(localName);
					query.append("_id SEPARATOR '-') FROM ");
					query.append(tableName);
					query.append("_link_");
					query.append(localName);
					query.append(" ");
					query.append(tmpVariable);
					query.append(" WHERE ");
					query.append(tmpVariable);
					query.append(".deleted = false AND ");
					query.append(tableName);
					query.append(".id = ");
					query.append(tmpVariable);
					query.append(".");
					query.append(tableName);
					query.append("_id GROUP BY ");
					query.append(tmpVariable);
					query.append(".");
					query.append(tableName);
					query.append("_id ) AS ");
					query.append(name);
					query.append(" ");
					/*
	                "              (SELECT GROUP_CONCAT(tmp.data_id SEPARATOR '-')" +
	                "                      FROM cover_link_node tmp" +
	                "                      WHERE tmp.deleted = false" +
	                "                            AND node.id = tmp.node_id" +
	                "                      GROUP BY tmp.node_id) AS covers" +
	                */
				} else {
					if (linkGeneric == ModelLink.NONE) {
						autoClasify[indexAutoClasify++] = StateLoad.NORMAL;
					} else {
						autoClasify[indexAutoClasify++] = StateLoad.ARRAY;
					}
			        query.append(" ");
			        query.append(tableName);
			        query.append(".");
			        query.append(name);
				}
   		 	}
   			query.append(" FROM `");
	        query.append(tableName);
   			query.append("` ");
   		 	query.append(" WHERE ");
	        //query.append(tableName);
	        //query.append(".");
   			//query.append(primaryKeyField.getName());
   			//query.append(" = ?");
   			//query.append(" AND ");
	        query.append(tableName);
   			query.append(".deleted = false ");
   		 	firstField = true;
   		    System.out.println("generate the querry: '" + query.toString() + "'");
   			System.out.println("request get " + clazz.getCanonicalName() + " prepare @" + getCurrentTimeStamp());
            // prepare the request:
            PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);

    		System.out.println("request get " + clazz.getCanonicalName() + " query @" + getCurrentTimeStamp());
            // execute the request
            ResultSet rs = ps.executeQuery();
    		System.out.println("request get " + clazz.getCanonicalName() + " transform @" + getCurrentTimeStamp());
            
            while (rs.next()) {
                indexAutoClasify = 0;
            	Object data = clazz.getConstructors()[0].newInstance();
            	count = 1;
       		 	for (Field elem : clazz.getFields()) {
    				/*
    				boolean notRead = elem.getDeclaredAnnotationsByType(SQLNotRead.class).length != 0;
    				*/
       		 		boolean notRead = autoClasify[indexAutoClasify] == StateLoad.DISABLE;
    				if (!fool && notRead) {
    					indexAutoClasify++;
    					continue;
    				}
    				//String name = elem.getName();
    				//boolean linkGeneric = elem.getDeclaredAnnotationsByType(SQLTableLinkGeneric.class).length != 0;
    				boolean linkGeneric = autoClasify[indexAutoClasify] == StateLoad.ARRAY;
    				if (linkGeneric) {
    					List<Long> idList = getListOfIds(rs, count);
    					elem.set(data, idList);
    				} else {
    					setValueFromDb(elem.getType(), data, count, elem, rs);
    				}
    				indexAutoClasify++;
    				count++;
       		 	}
       		 	//System.out.println("Read: " + (T)data);
				out.add((T)data);
            }

    		System.out.println("request get " + clazz.getCanonicalName() + " ready @" + getCurrentTimeStamp());
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        entry.disconnect();
        entry = null;
        return out;
	}

	public static void addLink(Class<?> clazz, long localKey, String table, long remoteKey) throws Exception {
    	String tableName = getTableName(clazz);
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        long uniqueSQLID = -1;
        // real add in the BDD:
        try {
            // prepare the request:
            String query = "INSERT INTO " + tableName + "_link_" + table + " (create_date, modify_date, " + tableName + "_id, " + table + "_id)" +
                    " VALUES (now(3), now(3), ?, ?)";
            PreparedStatement ps = entry.connection.prepareStatement(query,
                    Statement.RETURN_GENERATED_KEYS);
            int iii = 1;
            ps.setLong(iii++, localKey);
            ps.setLong(iii++, remoteKey);
            // execute the request
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating data failed, no rows affected.");
            }
            // retreive uid inserted
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    uniqueSQLID = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained (1).");
                }
            } catch (Exception ex) {
                System.out.println("Can not get the UID key inserted ... ");
                ex.printStackTrace();
                throw new SQLException("Creating user failed, no ID obtained (2).");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new ExceptionDBInterface(500, "SQL error: " + ex.getMessage());
        } finally {
            entry.disconnect();
        }
	}
	public static void removeLink(Class<?> clazz, long localKey, String table, long remoteKey) throws Exception {
    	String tableName = getTableName(clazz);
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "UPDATE `" + tableName + "_link_" + table + "` SET `modify_date`=now(3), `deleted`=true WHERE `" + tableName + "_id` = ? AND `" + table + "_id` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            int iii = 1;
            ps.setLong(iii++, localKey);
            ps.setLong(iii++, remoteKey);
            ps.executeUpdate();
        } catch (SQLException ex) {
        	ex.printStackTrace();
            throw new ExceptionDBInterface(500, "SQL error: " + ex.getMessage());
        } finally {
            entry.disconnect();
        }
	}

	/**
	 * extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list  of Long value
	 * @throws SQLException if an error is generated in the sql request.
	 */
    protected static List<Long> getListOfIds(ResultSet rs, int iii) throws SQLException {
    	String trackString = rs.getString(iii);
    	if (rs.wasNull()) {
    		return null;
    	}
    	List<Long> out = new ArrayList<>();
        String[] elements = trackString.split("-");
        for (String elem : elements) {
            Long tmp = Long.parseLong(elem);
            out.add(tmp);
        }
        return out;
    }
    /**
     * Convert the list if external Ids in a string '-' separated
     * @param ids List of value (null are removed)
     * @return '-' string separated
     */
    protected static String getStringOfIds(List<Long> ids) {
    	List<Long> tmp = new ArrayList<>();
    	for (Long elem : ids) {
    		tmp.add(elem);
    	}
    	return tmp.stream().map(x->String.valueOf(x)).collect(Collectors.joining("-"));
    }
    
	
	public static void delete(Class<?> clazz, long id) throws Exception {
		// TODO: I am not sure this is a real good idea.
	}
	public static void setDelete(Class<?> clazz, long id) throws Exception {
    	String tableName = getTableName(clazz);
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "UPDATE `" + tableName + "` SET `modify_date`=now(3), `deleted`=true WHERE `id` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            int iii = 1;
            ps.setLong(iii++, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new ExceptionDBInterface(500, "SQL error: " + ex.getMessage());
        } finally {
            entry.disconnect();
        }
	}
	public static void unsetDelete(Class<?> clazz, long id) throws Exception {
    	String tableName = getTableName(clazz);
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "UPDATE `" + tableName + "` SET `modify_date`=now(3), `deleted`=false WHERE `id` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            int iii = 1;
            ps.setLong(iii++, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new ExceptionDBInterface(500, "SQL error: " + ex.getMessage());
        } finally {
            entry.disconnect();
        }
	}
	
	public static String createTable(Class<?> clazz) throws Exception {
		String tableName = getTableName(clazz);
		boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
		StringBuilder out = new StringBuilder();
		StringBuilder otherTable = new StringBuilder();
		// Drop Table
		if (createIfNotExist) {
			 out.append("DROP TABLE IF EXISTS `");
			 out.append(tableName);
			 out.append("`;\n");
		}
		// create Table:
		out.append("CREATE TABLE `");
		out.append(tableName);
		out.append("` (");
		boolean firstField = true;
		System.out.println("===> TABLE `" + tableName + "`");
		String primaryKeyValue = null;
		for (Field elem : clazz.getFields()) {
		
			String name = elem.getName();
			Integer limitSize = getLimitSize(elem);
			boolean notNull = elem.getDeclaredAnnotationsByType(SQLNotNull.class).length != 0;
			boolean autoIncrement = elem.getDeclaredAnnotationsByType(SQLAutoIncrement.class).length != 0;
			
			boolean primaryKey = elem.getDeclaredAnnotationsByType(SQLPrimaryKey.class).length != 0;
			//boolean sqlNotRead = elem.getDeclaredAnnotationsByType(SQLNotRead.class).length != 0;
			boolean createTime = elem.getDeclaredAnnotationsByType(SQLCreateTime.class).length != 0;
			boolean updateTime = elem.getDeclaredAnnotationsByType(SQLUpdateTime.class).length != 0;
			ModelLink linkGeneric = getLinkMode(elem);
			String comment = getComment(elem);
			String defaultValue = getDefault(elem);
			//System.out.println("      ==> elem `" + name + "`     primaryKey=" + primaryKey + "       linkGeneric=" + linkGeneric);
			 
			 
			if (primaryKey) {
				primaryKeyValue = name;
			}
			// special case with external link table:
			if (linkGeneric == ModelLink.EXTERNAL) {
				 String localName = name;
				 if (name.endsWith("s")) {
					 localName = name.substring(0, name.length()-1);
				 }
				 if (createIfNotExist) {
					 otherTable.append("DROP TABLE IF EXISTS `");
					 otherTable.append(tableName);
					 otherTable.append("_link_");
					 otherTable.append(localName);
					 otherTable.append("`;\n");
				 }
				 otherTable.append("CREATE TABLE `");
				 otherTable.append(tableName);
				 otherTable.append("_link_");
				 otherTable.append(localName);
				 otherTable.append("`(\n");
				 otherTable.append("\t\t`id` bigint NOT NULL AUTO_INCREMENT,\n");
				 otherTable.append("\t\t`deleted` tinyint(1) NOT NULL DEFAULT '0',\n");
				 otherTable.append("\t\t`create_date` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n");
				 otherTable.append("\t\t`modify_date` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n");
				 otherTable.append("\t\t`");
				 otherTable.append(tableName);
				 otherTable.append("_id` bigint NOT NULL,\n");
				 otherTable.append("\t\t`");
				 otherTable.append(localName);
				 otherTable.append("_id` bigint NOT NULL,\n");
				 otherTable.append("\tPRIMARY KEY (`id`)\n");
				 otherTable.append("\t) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\n\n");
			} else {
				if (firstField) {
					out.append("\n\t\t`");
					firstField = false;
				} else {
					out.append(",\n\t\t`");
				}
				out.append(name);
				out.append("` ");
				String typeValue = null;
				if (linkGeneric == ModelLink.INTERNAL) {
					typeValue = convertTypeInSQL(String.class);
					out.append(typeValue);
				} else {
					typeValue = convertTypeInSQL(elem.getType());
					if (typeValue.equals("text")) {
						if (limitSize != null) {
							out.append("varchar(");
							out.append(limitSize);
							out.append(")");
						} else {
							out.append("text CHARACTER SET utf8");
						}
					} else {
						out.append(typeValue);
					}
				}
				out.append(" ");
				if (notNull) {
					out.append("NOT NULL ");
					if (defaultValue == null) {
						if (updateTime || createTime) {
							out.append("DEFAULT CURRENT_TIMESTAMP(3) ");
						}
					} else {
						out.append("DEFAULT ");
						out.append(defaultValue);
						out.append(" ");
					}
				} else if (defaultValue == null) {
					if (updateTime || createTime) {
						out.append("DEFAULT CURRENT_TIMESTAMP(3) ");
					} else {
						out.append("DEFAULT NULL ");
					}
				} else {
					out.append("DEFAULT ");
					out.append(defaultValue);
					out.append(" ");
					 
				}
				if (autoIncrement) {
					out.append("AUTO_INCREMENT ");
				}
				 
				if (comment != null) {
					out.append("COMMENT '");
					out.append(comment.replaceAll("'", "\'"));
					out.append("' ");
				}
			 }
		 }
		 if (primaryKeyValue != null) {
			 out.append(",\n\tPRIMARY KEY (`");
			 out.append(primaryKeyValue);
			 out.append("`)");
		 }
		 out.append("\n\t) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\n");
		 return out.toString() + otherTable.toString();
	}
	

	public static String getTableName(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(SQLTableName.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @AknotDescription on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((SQLTableName) annotation[0]).value();
		if (tmp == null) {
			return null;
		}
		return tmp;
	}	
	public static String getComment(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(SQLComment.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @AknotDescription on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((SQLComment) annotation[0]).value();
		if (tmp == null) {
			return null;
		}
		return tmp;
	}	
	public static String getDefault(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(SQLDefault.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @AknotDescription on " + element.getClass().getCanonicalName());
		}
		final String tmp = ((SQLDefault) annotation[0]).value();
		if (tmp == null) {
			return null;
		}
		return tmp;
	}	
	public static Integer getLimitSize(final Field element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(SQLLimitSize.class);
		if (annotation.length == 0) {
			return null;
		}
		if (annotation.length > 1) {
			throw new Exception("Must not have more than 1 element @AknotDescription on " + element.getClass().getCanonicalName());
		}
		return ((SQLLimitSize) annotation[0]).value();
	}

}
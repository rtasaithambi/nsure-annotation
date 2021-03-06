package com.kgisl.nsure.data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.kgisl.nsure.data.annotation.AutoGenerated;
import com.kgisl.nsure.data.annotation.Column;
import com.kgisl.nsure.data.annotation.SQLDateFormat;
import com.kgisl.nsure.data.annotation.Table;

/**
 * This class process all the database related annotations and perform the DML
 * operations like JPA API.
 * 
 * @author asaithambi.r
 * @since 01-FEB-2019
 * @see com.kgisl.nsure.data.annotation.DataAnnotationProcessor
 */
class DataAnnotationProcessor {
	final static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

	public final static TableMeta processAnnotation(Object object) throws Exception {
		TableMeta metaData = null;
		try {
			Class<?> objectClass = object.getClass();

			if (objectClass.isAnnotationPresent(Table.class)) {
				metaData = new TableMeta();
				final MapSqlParameterSource params = new MapSqlParameterSource();
				final List<String> autoGeneratedCols = new ArrayList<>();
				final List<String> ignoreCols = new ArrayList<>();

				Annotation tableAnnotation = objectClass.getAnnotation(Table.class);
				Table table = (Table) tableAnnotation;

				Field[] declaredFields = objectClass.getDeclaredFields();
				if (declaredFields != null && declaredFields.length > 0) {
					for (Field field : declaredFields) {
						Column column = null;
						// Column Annotation Processor
						if (field.isAnnotationPresent(Column.class)) {
							boolean fieldAccessible = field.isAccessible();
							if (!fieldAccessible) {
								field.setAccessible(true);
							}

							column = (Column) field.getAnnotation(Column.class);
							String fieldType = field.getType().getSimpleName();

							if (fieldType.equals(FieldType.BYTE.toString())) {
								params.addValue(column.value(),
										column.nullable() && field.getByte(object) == 0 ? null : field.getByte(object));
							} else if (fieldType.equals(FieldType.SHORT.toString())) {
								params.addValue(column.value(), column.nullable() && field.getShort(object) == 0 ? null
										: field.getShort(object));
							} else if (fieldType.equals(FieldType.CHAR.toString())) {
								params.addValue(column.value(),
										column.nullable() && field.getChar(object) == 0 ? null : field.getChar(object));
							} else if (fieldType.equals(FieldType.INT.toString())) {
								params.addValue(column.value(),
										column.nullable() && field.getInt(object) == 0 ? null : field.getInt(object));
							} else if (fieldType.equals(FieldType.FLOAT.toString())) {
								params.addValue(column.value(), column.nullable() && field.getFloat(object) == 0 ? null
										: field.getFloat(object));
							} else if (fieldType.equals(FieldType.DOUBLE.toString())) {
								params.addValue(column.value(), column.nullable() && field.getDouble(object) == 0 ? null
										: field.getDouble(object));
							} else if (fieldType.equals(FieldType.LONG.toString())) {
								params.addValue(column.value(),
										column.nullable() && field.getLong(object) == 0 ? null : field.getLong(object));
							} else if (fieldType.equals(FieldType.STRING.toString())) {
								String strValue = (String) field.get(object);
								if (strValue != null && strValue.trim().length() > 0) {
									strValue = strValue.trim();
									strValue = column.lowercase() ? strValue.toLowerCase() : strValue;
									strValue = column.uppercase() ? strValue.toUpperCase() : strValue;
								}

								// PROCESS DATE TYPE FIELDS
								if (strValue != null && strValue.trim().length() > 0
										&& !column.dateformat().equals(SQLDateFormat.NONE)) {
									java.sql.Date sqlDate = null;
									if (column.dateformat().equals(SQLDateFormat.DATE)) {
										sqlDate = strtoSqlDate(strValue, SQLDateFormat.NONE);
										if (sqlDate == null) {
											throw new RuntimeException("Invalid date value passed @" + field.getName());
										}
									} else if (column.dateformat().equals(SQLDateFormat.TODAY)) {
										sqlDate = strtoSqlDate(strValue, column.dateformat());
									} else if (column.dateformat().equals(SQLDateFormat.NOW)) {
										sqlDate = strtoSqlDate(strValue, column.dateformat());
									}
									params.addValue(column.value(), sqlDate);
								}
								// PROCESS STRING TYPE FIELDS
								else {
									params.addValue(column.value(), strValue);
								}
							}
							// STRING TYPE REFLECTION ENDS.

							// GATHER ALL THE IGNORED COLUMNS AS TO RESTRICT FROM THE UPDATE OPERATIONS.
							if (column.ignoreupdate()) {
								ignoreCols.add(column.value());
							}

							field.setAccessible(fieldAccessible);
						}

						// AUTO GENERATED KEY COLUMN [ REQUIRED FOR UPDATE OPERATION AND WILL RETURN
						// UNIQUE KEY ON INSERT MODE ]
						if (column != null && field.isAnnotationPresent(AutoGenerated.class)) {
							autoGeneratedCols.add(column.value());
						}

					}
					// FIELD ITERATIONS ENDS.
				}
				// END-IF - @Column DECLARED FIELDS

				// SET THE META INFORMATION TO AN OBJECT WHICH WILL HELP FOR FURTHER PROCESSING.
				metaData.setIgnoreCols(ignoreCols);
				metaData.setAutoGeneratedCols(autoGeneratedCols);
				metaData.setParams(params);
				metaData.setTableName(table.value());
			}
		} catch (Exception e) {
			throw e;
		}
		return metaData;
	}
	
	/**
	 * Convert object fields to table columns
	 * @param aClass
	 * @param params
	 * @return
	 */
	public static MapSqlParameterSource toTableColumns(Class<?> aClass, MapSqlParameterSource params) {
		MapSqlParameterSource tableColumns = null;
		if(aClass.isAnnotationPresent(Table.class)) {
			tableColumns = new MapSqlParameterSource();
			Map<String,Object> paramMap = params.getValues();
			Field[] fields = aClass.getDeclaredFields();
			for(Field field : fields) {
				if(field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);
					if(paramMap.containsKey(field.getName())) {
						tableColumns.addValue(column.value(), paramMap.get(field.getName()));
					}
				}
			}
		}
		return tableColumns;
	}

	/**
	 * Read all annotated columns of a class to perform READ operation
	 * @param aClass
	 * @param tableMeta
	 * @return
	 */
	public static TableMeta getAnnotatedFields(Class<?> aClass, TableMeta tableMeta) {
		if(aClass.isAnnotationPresent(Table.class)) {
			Table table =  aClass.getAnnotation(Table.class);
			
			tableMeta.setTableName(table.value());
			List<String> cols = new ArrayList<>();
			Field[] fields = aClass.getDeclaredFields();
			for(Field field : fields) {
				if(field.isAnnotationPresent(Column.class)) {
					Column column = (Column)field.getAnnotation(Column.class);
					cols.add(column.value());
				}
			}
			tableMeta.setCols(cols);
		}
		return tableMeta;
	}

	/**
	 * Create new instance of a given class and populate given parameter values
	 * @param aClass
	 * @param params
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createNewInstanceWithParameterValues(Class<T> aClass, Map<String,Object> params) throws Exception {
		Object aClassObject = null;
		if(aClass.isAnnotationPresent(Table.class)) {
			
			Constructor<?> constructor = Class.forName(aClass.getName()).getConstructor();
			aClassObject = constructor.newInstance();
			
			Field[] fields = aClass.getDeclaredFields();
			
			for(Field field : fields) {
				
				if(field.isAnnotationPresent(Column.class)) {
					Column column = (Column) field.getAnnotation(Column.class);
					
					boolean isAccessible = field.isAccessible();
					field.setAccessible(true);
					
					String fieldType = field.getType().getSimpleName();
					Object valueObject = params.get(column.value());
					String value = null;
					
					if (fieldType.equals(FieldType.BYTE.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.setByte(aClassObject, Byte.parseByte(value));
						}
					} else if (fieldType.equals(FieldType.SHORT.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.setShort(aClassObject, Short.parseShort(value));
						}
					} else if (fieldType.equals(FieldType.INT.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.setInt(aClassObject, Integer.parseInt(value));
						}
					} else if (fieldType.equals(FieldType.FLOAT.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.setFloat(aClassObject, Float.parseFloat(value));
						}
					} else if (fieldType.equals(FieldType.DOUBLE.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.setDouble(aClassObject, Double.parseDouble(value));
						}
					} else if (fieldType.equals(FieldType.LONG.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.setLong(aClassObject, Long.parseLong(value));
						}
					} else if (fieldType.equals(FieldType.STRING.toString())) {
						if(valueObject != null) {
							value = String.valueOf(valueObject);
							field.set(aClassObject, value);
						}
					}
					field.setAccessible(isAccessible);
				}
			}
		}
		return aClassObject != null ? (T) aClassObject : null;
	}
	
	/*************** INNER TYPE DEFINITIONS STARTS ****************************/
	private enum FieldType {

		INT("int"), DOUBLE("double"), STRING("String"), CHAR("char"), FLOAT("float"), LONG("long"), SHORT("short"),
		BYTE("byte");

		private String value;

		@Override
		public String toString() {
			return this.value;
		}

		private FieldType(String value) {
			this.value = value;
		}
	}

	private static Date strtoSqlDate(String date, SQLDateFormat sqlDateType) {
		java.sql.Date sqlDate = null;
		try {
			if (sqlDateType != null && sqlDateType.equals(SQLDateFormat.NOW)) {
				sqlDate = new java.sql.Date(new java.util.Date().getTime());
			} else if (sqlDateType != null && sqlDateType.equals(SQLDateFormat.TODAY)) {
				sqlDate = new java.sql.Date(sdf.parse(sdf.format(new java.util.Date())).getTime());
			} else if (date != null && date.trim().length() == 10) {
				java.util.Date utildate = sdf.parse(date.trim());
				sqlDate = new java.sql.Date(utildate.getTime());
			}
		} catch (Exception e) {
			sqlDate = null;
		}
		return sqlDate;
	}

	/*************** INNER TYPE DEFINITIONS ENDS ****************************/
}

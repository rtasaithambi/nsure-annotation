package com.kgisl.nsure.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

public class NsureData {

	private NamedParameterJdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	@SuppressWarnings("unchecked")
	public int[] save(List<? extends Object> objectList, boolean...updateMode) throws Exception {
		int[] intResult = null;
		
		if(objectList == null || objectList.isEmpty()) {
			throw new RuntimeException("Save operation failed, Object or List<?> instance is Null.");
		}
		
		try {
			
			List<Map<String, Object>> batchValues = new ArrayList<>(objectList.size());
			
			boolean isUpdate = false;
			if(updateMode != null && updateMode.length > 0 && updateMode[0]) {
				isUpdate = true;
			}
			
			TableMeta metaData = null;
			for(Object object : objectList) {
				metaData = DataAnnotationProcessor.processAnnotation(object);
				if(metaData == null) {
					throw new RuntimeException("Operation failed, unable to save data.");
				}
				
				metaData.setUpdate(updateMode != null && updateMode.length > 0 ? updateMode[0] : false);
				metaData = buildMetaData(metaData);
				
				Map<String, Object> mapParams = metaData.getParams().getValues();
				
				if(mapParams != null && !mapParams.isEmpty() && isUpdate) {
					int autoGeneratedKeyValue = (Integer) mapParams.get(metaData.getReturnKey());
					if(autoGeneratedKeyValue <= 0) {
						throw new RuntimeException("Update Failed, No Values supplied for AutoGenerated Column.");
					}
				}
				
				batchValues.add(mapParams);
			}
			
			String query = null;
			
			if(isUpdate) {
				query = buildUpdateSQL(metaData.getTableName(), metaData.getCols(), metaData.getParams(), metaData.getReturnKey());
				isUpdate = true;
			} else {
				query = buildInsertSQL(metaData.getTableName(), metaData.getCols(), metaData.getPlaceHolders());
			}
			
			if(query != null && !batchValues.isEmpty()) {
				intResult = jdbcTemplate.batchUpdate(query,batchValues.toArray(new Map[objectList.size()]));
			}
			
		} catch (Exception e) {
			throw e;
		}
		return intResult;
	}
	
	public Number save(Object object, boolean... updateMode) throws Exception {
		Number number = null;
		
		if(object == null) {
			throw new RuntimeException("Save operation failed, Object or List<?> instance is Null.");
		}
		
		try {
			TableMeta metaData = DataAnnotationProcessor.processAnnotation(object);
			
			if(metaData == null) {
				throw new RuntimeException("Operation failed, unable to save data.");
			}
			
			metaData.setUpdate(updateMode != null && updateMode.length > 0 ? updateMode[0] : false);
			metaData = buildMetaData(metaData);
			
			number = save(metaData, updateMode);
		}catch (Exception e) {
			throw e;
		}
		return number;
	}
	
	private Number save(TableMeta metaData, boolean... updateMode) {
		Number number = null;
		String query = null;
		if(metaData.isUpdate()) {
			query = buildUpdateSQL(metaData.getTableName(), metaData.getCols(), metaData.getParams(), metaData.getReturnKey());
		} else {
			query = buildInsertSQL(metaData.getTableName(), metaData.getCols(), metaData.getPlaceHolders());
		}
		
		if(metaData.isUpdate()) {
			int autoGeneratedKeyValue = (Integer) metaData.getParams().getValue(metaData.getReturnKey());
			if(autoGeneratedKeyValue <= 0) {
				throw new RuntimeException("Update Failed, No Values supplied for AutoGenerated Column.");
			}
		}
		
		if(metaData.getReturnKey() != null && metaData.getReturnKey().trim().length() > 0) {
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(query, metaData.getParams(), keyHolder, new String[]{metaData.getReturnKey()});
			number = keyHolder.getKey();
			if(number == null && metaData.isUpdate()) {
				number = (Integer) metaData.getParams().getValue(metaData.getReturnKey());
			}
		} else {
			jdbcTemplate.update(query, metaData.getParams());				
		}
		return number;
	}
	
	private TableMeta buildMetaData(TableMeta metaData) {
		String returnKey = null;
		Set<String> keySet = new HashSet<>();
		if (metaData.getParams() != null && metaData.getParams().getValues() != null) {
			keySet.addAll(metaData.getParams().getValues().keySet());
		} else {
			throw new RuntimeException("No properties are annotated with @Column annotation.");
		}
		
		if (metaData.getAutoGeneratedCols() != null && !metaData.getAutoGeneratedCols().isEmpty()) {
			if(metaData.getAutoGeneratedCols().size() > 1) {
				throw new RuntimeException("@AutoGenerated annotation can be applied one column only.");
			}
			keySet.removeAll(metaData.getAutoGeneratedCols());
			returnKey = metaData.getAutoGeneratedCols().iterator().next();
		} else if(metaData.isUpdate()) {
			throw new RuntimeException("@AutoGenerated annotation is required to perform Update Operation.");
		}
		
		// RESTRICT COLUMNS UPDATE.
		if(metaData.getIgnoreCols() != null && !metaData.getIgnoreCols().isEmpty() && metaData.isUpdate()) {
			keySet.removeAll(metaData.getIgnoreCols());
		}

		List<String> cols = new ArrayList<>();
		List<String> placeHolders = new ArrayList<>();

		for (String key : keySet) {
			cols.add(key);
			placeHolders.add(":" + key);
		}
		
		metaData.setPlaceHolders(placeHolders);
		metaData.setCols(cols);
		metaData.setReturnKey(returnKey);
		return metaData;
	}
	
	private String buildInsertSQL(String tableName, List<String> cols, List<String> placeHolders) {
		return new StringBuilder()
				.append("INSERT INTO ")
				.append(tableName)
				.append(" (")
				.append(StringUtils.collectionToCommaDelimitedString(cols))
				.append(" )")
				.append(" VALUES")
				.append(" (")
				.append(StringUtils.collectionToCommaDelimitedString(placeHolders))
				.append(")")
				.toString();
	}
	
	private String buildUpdateSQL(String tableName, List<String> cols, MapSqlParameterSource params, String returnKey) {
		StringBuilder query = new StringBuilder()
				.append(" UPDATE ")
				.append(tableName)
				.append(" SET ")
				;
		
		Map<String,String> updateParams = new HashMap<>();
		for(String key : cols) {
			if(!key.equalsIgnoreCase(returnKey.trim())) {
				updateParams.put(key, ":"+key);
			}
		}
		query.append(updateParams.toString().replaceAll("[{|}]+", ""))
		.append(" WHERE ")
		.append(returnKey)
		.append("=:")
		.append(returnKey.trim());
		return query.toString();
				
	}
	
	/****************************  READ OPERATIONS STARTS ************************************/
	
	@SuppressWarnings("unchecked")
	public <T> T queryForObject(Class<T> clazz, MapSqlParameterSource params) throws Exception {
		List<Object> resultList = null;
		try {
			TableMeta tableMeta = new TableMeta();
			if(params != null && !params.getValues().isEmpty()) {
				tableMeta.setParams(DataAnnotationProcessor.toTableColumns(clazz, params));
			}
			DataAnnotationProcessor.getAnnotatedFields(clazz, tableMeta);
			String query = buildSelectQuery(tableMeta);
			List<Map<String,Object>> dbResult = fetchQueryResult(query, tableMeta);
			if(dbResult != null && !dbResult.isEmpty()) {
				resultList = new ArrayList<>();
				
				for(Map<String,Object> rowMap : dbResult) {
					Object object = DataAnnotationProcessor.createNewInstanceWithParameterValues(clazz, rowMap);
					resultList.add(object);
				}
			}
		}catch (Exception e) {
			throw e;
		}
		if(resultList != null && resultList.size() > 1) {
			throw new RuntimeException("Incorrect Result size, Expected 1 actual "+resultList.size());
		}
		return resultList != null ? (T) resultList.get(0) : null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> query(Class<T> clazz, MapSqlParameterSource params) throws Exception {
		List<Object> resultList = null;
		try {
			TableMeta tableMeta = new TableMeta();
			if(params != null && !params.getValues().isEmpty()) {
				tableMeta.setParams(DataAnnotationProcessor.toTableColumns(clazz, params));
			}
			DataAnnotationProcessor.getAnnotatedFields(clazz, tableMeta);
			String query = buildSelectQuery(tableMeta);
			List<Map<String,Object>> dbResult = fetchQueryResult(query, tableMeta);
			if(dbResult != null && !dbResult.isEmpty()) {
				resultList = new ArrayList<>();
				
				for(Map<String,Object> rowMap : dbResult) {
					Object object = DataAnnotationProcessor.createNewInstanceWithParameterValues(clazz, rowMap);
					resultList.add(object);
				}
			}
		}catch (Exception e) {
			throw e;
		}
		return resultList != null ? (List<T>) resultList : null;
	}

	// FETCHING DATA FROM DATABASE.
	private List<Map<String,Object>> fetchQueryResult(String query,final TableMeta tableMeta) {
		final List<Map<String,Object>> mapList = new ArrayList<>(); // SQL Result Container
		jdbcTemplate.query(query, tableMeta.getParams(), new RowMapper<Object>() {
			@Override
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				Map<String,Object> row = new HashMap<>();
				for(String key : tableMeta.getCols()) {
					row.put(key, rs.getObject(key));
				}
				mapList.add(row);
				return null;
			}
		});
		return mapList;
	}
	
	private static String buildSelectQuery(TableMeta tableMeta) {
		StringBuilder query = new StringBuilder("SELECT ");
		MapSqlParameterSource params = tableMeta.getParams();
		
		query.append(StringUtils.collectionToCommaDelimitedString(tableMeta.getCols()))
			 .append(" FROM ")
			 .append(tableMeta.getTableName());

		// BUILDING WHERE CLAUSES FROM THE SQL PARAMETER SOURCE RECEIVED IN INPUT PARAMETER
		if (params != null && params.getValues() != null && !params.getValues().isEmpty()) {
			Map<String, Object> whereCols = params.getValues();
			Set<String> conds = new HashSet<>();
			for (Map.Entry<String, Object> map : whereCols.entrySet()) {
				if (map.getValue() == null) {
					conds.add(map.getKey() + " IS NULL");
				} else {
					conds.add(map.getKey() + " = :" + map.getKey());
				}
			}
			if(!conds.isEmpty()) {
				query.append(" WHERE ")
				.append(StringUtils.collectionToDelimitedString(conds, " AND "));
			}
		}
		return query.toString();
	}
	
	/****************************  READ OPERATIONS ENDS ************************************/
	
}

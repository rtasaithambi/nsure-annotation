package com.kgisl.nsure.data;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

class TableMeta {
	private String tableName;
	private MapSqlParameterSource params;
	private List<String> autoGeneratedCols;
	private List<String> ignoreCols;
	private boolean isUpdate;

	private List<String> cols;
	private List<String> placeHolders;
	private String returnKey;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public MapSqlParameterSource getParams() {
		return params;
	}

	public void setParams(MapSqlParameterSource params) {
		this.params = params;
	}

	public List<String> getAutoGeneratedCols() {
		return autoGeneratedCols;
	}

	public void setAutoGeneratedCols(List<String> autoGeneratedCols) {
		this.autoGeneratedCols = autoGeneratedCols;
	}

	public boolean isUpdate() {
		return isUpdate;
	}

	public void setUpdate(boolean isUpdate) {
		this.isUpdate = isUpdate;
	}

	public List<String> getIgnoreCols() {
		return ignoreCols;
	}

	public void setIgnoreCols(List<String> ignoreCols) {
		this.ignoreCols = ignoreCols;
	}

	public List<String> getCols() {
		return cols;
	}

	public void setCols(List<String> cols) {
		this.cols = cols;
	}

	public List<String> getPlaceHolders() {
		return placeHolders;
	}

	public void setPlaceHolders(List<String> placeHolders) {
		this.placeHolders = placeHolders;
	}

	public String getReturnKey() {
		return returnKey;
	}

	public void setReturnKey(String returnKey) {
		this.returnKey = returnKey;
	}

}
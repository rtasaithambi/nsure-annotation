package com.kgisl.nsure.data.annotation;

public enum SQLDateFormat {

	NONE(""), DATE("DD-MM-YYYY"), NOW("DD-MM-YYYY HH:MI:SS AM"), TODAY("DD-MM-YYYY");

	private String value;

	@Override
	public String toString() {
		return this.value;
	}

	private SQLDateFormat(String value) {
		this.value = value;
	}

}

package com.kgisl.nsure.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	String value();

	boolean nullable() default false;

	boolean lowercase() default false;

	boolean uppercase() default false;
	
	boolean ignoreupdate() default false;

	SQLDateFormat dateformat() default SQLDateFormat.NONE;

}

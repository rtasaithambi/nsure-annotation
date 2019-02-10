package com.kgisl.nsure.data.annotation.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.kgisl.nsure.data.annotation.Table;

public class TableAnnotationProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		Set<? extends Element> annotationList = roundEnv.getElementsAnnotatedWith(Table.class);

		if (annotationList != null && !annotationList.isEmpty()) {
			// TODO:
		}

		return true;
	}

}

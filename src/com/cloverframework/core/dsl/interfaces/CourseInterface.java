package com.cloverframework.core.dsl.interfaces;

import com.cloverframework.core.data.interfaces.CourseValues;
import com.cloverframework.core.dsl.AbstractCourse;
import com.cloverframework.core.exception.ArgsCountNotMatch;

public interface CourseInterface {
	void destroy();
	public Object execute();
	CourseValues getValues();
	AbstractCourse setValues(Object ...values) throws ArgsCountNotMatch;
	String getType();
}
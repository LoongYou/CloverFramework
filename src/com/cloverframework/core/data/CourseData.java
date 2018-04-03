package com.cloverframework.core.data;

import com.cloverframework.core.course.AbstractCourse;

public interface CourseData<T> {
	AbstractCourse<T> setValues(Object ...val);
	CourseValues getValues();
	void setResult(CourseResult<?> result);
	CourseResult<?> getResult();
}

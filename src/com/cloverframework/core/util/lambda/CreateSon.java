package com.cloverframework.core.util.lambda;

import com.cloverframework.core.dsl.AbstractCourse;

@FunctionalInterface
public interface CreateSon<R> {
	R apply(AbstractCourse parent,String optype,boolean isSon, Object... obj);
}

package com.cloverframework.core.data.interfaces;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 仓储层通过该接口与数据接口层实现交互
 * @author yl
 *
 * @param <T>
 */
public interface DataSwap<T> extends Iterable<CourseWrapper>{
	CourseWrapper open();
	void setResult(DataSet<T> data);
	void setResult(List<T> list, List<Object> objectList, Map<String, Object> map, Object value);
	void setResult(CourseResult<T> result);
	void close();
}

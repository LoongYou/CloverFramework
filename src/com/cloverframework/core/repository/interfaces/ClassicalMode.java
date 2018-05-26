package com.cloverframework.core.repository.interfaces;
/**
 * 
 * @author yl
 *
 */
public interface ClassicalMode{
	<E> E get(Class<E> Class,Integer key);
	<E> int add(E entity);
	<E> int put(E entity);
	<E> int remove(Class<E> Class,Integer key);
}
package com.cloverframework.core.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.cloverframework.core.domain.DomainService;
import com.cloverframework.core.dsl.interfaces.CourseOperation;

/**
 * Action是一个支持多线程的course代理，在服务程序中建议使用该类来创建业务过程而非使用CourseProxy，
 * 该类还提供了批量提交并执行业务过程的特性。原则上，不建议方法中创建action对象，因为那样会消耗大量内存，
 * 而是让业务组件继承action。例如：UserService extends Action
 * @author yl
 *
 * @param <T>
 */
public class Action<T> extends CourseProxy<T> implements CourseOperation{
	{
		/** 用于计算产生字面值的方法栈长是否合法，
		 * 如果别的方法中调用该类中的START()或START(args)方法（仅开发过程中可设置，对外隐藏），需要相应的+1*/
		level +=1;		
	}
	
	/** 每个线程操作的course对象是相互独立的，对course操作前会先将course设置到local中，确保线程安全。*/
	private ThreadLocal<Course> newest = new ThreadLocal<Course>();
	
	/** share区，用于缓存每个线程产生的course*/
	private ConcurrentHashMap<String,Course> shareSpace = new ConcurrentHashMap<String,Course>();
	
	/** work区，每个线程持有的一个相互独立的work集合，并且会被批量的提交至仓储执行*/
	private ThreadLocal<List<Course>> workSpace = new ThreadLocal<List<Course>>();
	
	/** 每个work集合初始大小*/
	private static byte workSize = 10;
	
	/** 只有当workable为1的时候，course才会被填入work区*/
	private static ThreadLocal<Byte> workable = new  ThreadLocal<Byte>();
	

	
	public Action() {}
	
	public Action(DomainService domainService) {
		super(domainService);
	}
	
	
	@Override
	public Course getCurrCourse() {
		return newest.get();
	}
	
	@Override
	public Course removeCurrCourse() {
		Course old = getCurrCourse();
		newest.remove();
		return old;
	}
	
	@Override
	public void setCurrCourse(Course course) {
		newest.set(course);
	}
	
	@Override
	public Course getCourse(String id) {
		return shareSpace.get(id);
	}
	
	@Override
	public void setCourse(String id,Course course) {
		shareSpace.put(id, course);
	}
	
	@Override
	public Course removeCourse(String id) {
		return shareSpace.remove(id);
	}
	

	/**
	 * 获取当前线程的work区
	 * @return
	 */
	public List<Course> getWorkSpace() {
		return workSpace.get();
	}

	/**
	 * 获取work区大小值
	 * @return
	 */
	public static byte getWorkSize() {
		return workSize;
	}

	/**
	 * 设置work区大小
	 * @param workSize
	 */
	public static void setWorkSize(byte workSize) {
		Action.workSize = workSize;
	}

	/**
	 * 开启工作区
	 */
	public void startWork() {
		workSpace.remove();
		workSpace.set(new ArrayList<Course>(workSize));
		workable.remove();
		workable.set((byte)1);
	}
	
	/**
	 * 关闭工作区同时销毁其中的course
	 * @return
	 */
	public int endWork() {
		if(workSpace.get()!=null) {
			for(Course course:workSpace.get()) {
				course.destroy();
			}
			workSpace.get().clear();
		}
		workable.remove();
		workable.set((byte)0);
		return 0;
	}
	
<<<<<<< HEAD:src/com/cloverframework/core/dsl/Action.java
=======
	/**
	 * {@inheritDoc}
	 */
>>>>>>> treenode:src/com/cloverframework/core/dsl/Action.java
	public int push() {
		return repository.fromAction(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Course START() {
		//必需
		return super.START();
	}

	/**
	 * {@inheritDoc}，
	 * 当 {@link Action#startWork()}开启，匹配到的course会填入workspace
	 */
	@Override
	public Course START(String id) {
		//必需
		Course course = super.START(id);
<<<<<<< HEAD:src/com/cloverframework/core/dsl/Action.java
//		if(workable.get()!=null && workable.get()==1 && course.getStatus()==Course.END)
//			workSpace.get().add(course);
=======
>>>>>>> treenode:src/com/cloverframework/core/dsl/Action.java
		return course;
	}

	/**
	 * 根据sharespace中的一个course创建分支引用，如果对应id的course存在，
	 * 则进行分支，否则不进行分支，分支的course将存入workspace
	 */
	public Course FORK(String id) {
		//必需
		Course course = super.FORK(id);
<<<<<<< HEAD:src/com/cloverframework/core/dsl/Action.java
//		if(workable.get()!=null && workable.get()==1 && course.getStatus()==Course.END)
//			workSpace.get().add(course);
=======
>>>>>>> treenode:src/com/cloverframework/core/dsl/Action.java
		return course;
	}
	
	/**
	 * {@inheritDoc}，
	 * 当 {@link Action#startWork()}开启，将fork或无给定id的course加入工作区
	 */
	protected void END() {
		//必需
		super.END();
		Course course = getCurrCourse();
		if(workable.get()!=null && workable.get()==1 && course.getStatus()==Course.END)
			workSpace.get().add(course);
	}	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();	
		for(String key:shareSpace.keySet()) {
			Course course = shareSpace.get(key);
			sb.append(course.toString()+"\n");
		}
		return sb.toString();			
	}
}

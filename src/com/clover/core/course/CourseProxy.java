package com.clover.core.course;

import java.util.HashMap;

import com.clover.core.factory.EntityFactory;
import com.clover.core.repository.CourseRepository;
import com.domain.DomainService;
import com.infrastructure.util.lambda.Literal;
/**
 * course代理提供了面向用户的course操作和管理方法，通常使用该类创建业务过程而不是course，
 * 该类大部分方法是线程不安全的。
 * @author yl
 *
 */
public class CourseProxy implements CourseOperation{
	/** 用于计算产生字面值的方法栈长是否合法，
	 * 如果别的方法中调用该类中的START()或START(args)方法（仅开发过程中可设置，对外隐藏），需要相应的+1*/
	byte level = 1;
	
	/**最后产生的course对象*/
	Course newest;
	/**eden区，用于缓存course对象*/
	HashMap<String,Course> eden = new HashMap<String,Course>();
	
	protected DomainService service;
	
	CourseRepository repository;
	/*
	 * 下面重写接口的方法用于针对courseProxy不同eden和newest的操作实现，
	 * 在实际中，根据需要使用合适的集合和对应的操作，如并发、或者队列，
	 * 可以通过子类重写这些方法即可实现，无须对其他特性进行改动
	 */
	
	@Override
	public Course getCurrCourse() {
		return newest;
	}
	
	@Override
	public Course removeCurrCourse() {
		Course old = getCurrCourse();
		newest = null;
		return old;
	}
	
	@Override
	public void setCurrCourse(Course course) {
		newest = course;
	}
	
	@Override
	public Course getCourse(String id) {
		return eden.get(id);
	}
	
	@Override
	public void addCourse(String id,Course course) {
		eden.put(id, course);
	}
	
	@Override
	public Course removeCourse(String id) {
		return eden.remove(id);
	}
	
	/*----------------------private method-------------------- */
	/**
	 * 初始化一个course
	 */
	private Course initCourse(Course course,DomainService service,CourseProxy proxy,byte status) {
		course.domainService = service;
		course.proxy = proxy;
		course.status = status;
		return course;
	}
	
	/**
	 * 该方法会初始化一个course并发送到factory的course集合中，
	 * 并且会判断存入的course与刚刚创建的course是否引用相同，
	 * 如果不相同则抛出异常，为防止获取到快照或者被jvm优化。
	 * @return 返回一个根节点
	 */
	private Course begin() {
		Thread t = Thread.currentThread();
		//调整该方法的位置需要修改length的值，每多一个上级方法调用length-1
		Course course = EntityFactory.putCourse(getCurrCourse(), t, t.getStackTrace().length-level);
		if(course.status==Course.WAIT) {
			return course;
		}
		//如果集合返回的不是刚刚创建的对象
		return null;
	}

	/**
	 * 设置course内部的时间属性
	 * @param course
	 */
	private void setCourseTime(Course course) {
		long exe = System.currentTimeMillis()-course.createTime;
		course.max_exe = (exe>course.max_exe?exe:course.max_exe);
		course.min_exe = (exe<course.min_exe?exe:course.min_exe);
		if(course.min_exe==0)
			course.min_exe = exe;
		course.avg_exe = (exe+course.avg_exe)/(course.avg_exe==0?1:2);
	}

	
	/*----------------------public method-------------------- */
	
	
	
	public CourseProxy() {}

	public CourseProxy(DomainService service) {
		this.service = service;
	}
	
	public HashMap<String, Course> getEden() {
		return eden;
	}
	
	public void setRepository(CourseRepository repository) {
		this.repository = repository;
	}

	/**
	 * 获取course list的友好信息
	 * @see CourseProxy#getInfo()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();	
		for(String key:eden.keySet()) {
			Course course = eden.get(key);
			course.condition1 = true;
			course.condition2 = true;
			sb.append(course.toString()+"\n");
		}
		return sb.toString();			
	}

	/**
	 * 打印course list的详细，如没必要，使用toString()
	 * @return
	 */
	public String getInfo() {
		return getCurrCourse().toString()+"\n";
	}

	/**
	 * 将一个新的course替换上一个course，返回上一个course
	 * @param cover false：不覆盖集合中的上一个course，true：覆盖集合中的上一个course
	 * @return
	 */
	protected Course addCourse(String id,boolean cover) {
		Course old = removeCurrCourse();
		Course newc = new Course(id);
		initCourse(newc,service,this,Course.WAIT);
		if(cover)
			addCourse(old.id, newc);
		setCurrCourse(newc);
		return old;
	}
	
	
	/*------如果希望调用者只能以匿名类的形式使用，这些方法或重写应当为protected------*/
	
	
	/**
	 * 开始一个course的方法，通过该方法可以链式执行GET ADD PUT REMVE等方法
	 * @return 返回一个根节点
	 */
	public Course START() {
		addCourse(null,false);
		return begin();
	}
	
	/**
	 * 在开始course时提供一个id作为它的标识，同时该course在end之后会被缓存，
	 * 如果执行时该id的course存在缓存，则使用缓存的course
	 * @param id 这个course的标识，不能包含空格
	 * @return
	 */
	public Course START(String id) {
		Course cache = null;
		String reg = "^[\\S]*$";
		if(id!=null && id.matches(reg))
			cache = getCourse(id);
		if(cache!=null)
			return cache;
		addCourse(id,false);
		return begin();
	}
	
	/**
	 * 将当前course标记为end状态并缓存
	 * 1、如果course没有一个有效的id则不会缓存。
	 * 2、如果没有对当前course进行end，在下一次start新course时当前course会被取代。
	 */
	public void END() {
		Course course = getCurrCourse();
		setCourseTime(course);
		if(course.status==Course.END && course.id!=null)
			addCourse(course.id, course);
		removeCurrCourse();
	}

	/**
	 * 直接执行当前的一条course语句
	 * @return
	 */
	public Object executeOne() {
		return repository.query(newest);
	}
	
	
	/**
	 * 一系列的字面值参数的开头，例如：如果在GET($(),user.getName(),user.getId())之前，
	 * 在course方法以外的地方使用了User.getName()等方法，
	 * 则$()方法是必需出现在参数的第一位，以抹除之前无关的字面值。
	 * @return null
	 */
	public Object $() {
		getCurrCourse().literalList.clear();
		return null;
	}
	
	/**
	 * 方法引用，以lambda的方式提供方法字面参数，
	 * 如GET($(user::getName))相当于GET(user.getName()),当有多个方法字面值需要获取，最好用该方式。<p>
	 * 例如：GET($(user::getName,user::getId,user,user::getCode),首项为lambda的情况下不需要$()。
	 * 如果跟user.getName()这样的方法混用，那么lambda的任意一个表达式必需写在参数的第一位。
	 * 
	 * @param lt 实体类的lambda表达式，如user::getName，建议采用此种语法而非()->user.getName(),
	 * 因为这种写法会带来隐患，比如可在lambda中执行多个句柄，这样会令字面值参数跟期望的不一致，
	 * 尽管Course已经尽力避免这种情况，但是无法完全阻止通过这种手段输入，这是由于java语言机制限制了，因此在实际使用中
	 * 需要多加留意。并且，在其他层面（如domainMatch、EntityFactory）会对该问题进行原则上的处理。
	 * @return null
	 */
	public Object $(Literal ...lt) {
		Course course = getCurrCourse();
		if(course.status==Course.WAIT)
			course.literalList.clear();
		course.status = Course.LAMBDA;
		for(Literal li:lt) {
			li.literal();
		}
		course.status = Course.METHOD;
		return null;
	}
	
	/**
	 * 三元方法引用，可获取三元运算结果的lambda，建议作为在字面值参数列表的最后一项
	 * @param li @see {@link CourseProxy#$(Literal...)}
	 * @return
	 * @
	 */
	public Object $te(Literal li) {
		Course course = getCurrCourse();
		int c = course.status;
		if(course.status==Course.WAIT)
			course.literalList.clear();
		course.status = Course.LAMBDA;
		li.literal();
		course.status = Course.METHOD;
		if(c!=0)
			course.literalList.remove(course.literalList.size()-2);
		return null;
	}
	
	/**
	 * 可获取三元运算结果的字面值
	 * @param obj @see {@link CourseProxy#$(Literal...)}
	 * @return
	 */
	public Object te(Object obj) {
		Course course = getCurrCourse();
		course.literalList.remove(course.literalList.size()-2);
		return null;
	}
	
	
}

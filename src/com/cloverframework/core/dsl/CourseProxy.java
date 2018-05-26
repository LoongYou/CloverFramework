package com.cloverframework.core.dsl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.cloverframework.core.data.interfaces.CourseResult;
import com.cloverframework.core.domain.DomainService;
import com.cloverframework.core.dsl.Course.Condition;
import com.cloverframework.core.dsl.Course.Count;
import com.cloverframework.core.dsl.interfaces.CourseOperation;
import com.cloverframework.core.dsl.interfaces.CourseProxyInterface;
import com.cloverframework.core.factory.CourseFactory;
import com.cloverframework.core.factory.EntityFactory;
import com.cloverframework.core.repository.CourseRepository;
import com.cloverframework.core.util.interfaces.CourseType;
import com.cloverframework.core.util.lambda.Literal;
/**
 * 泛型C extends AbstractCourse，下面统称course
 * course代理提供了面向用户的course操作和管理方法，通常使用该类创建业务过程普适的course，
 * 该类大部分方法是线程不安全的。
 * @author yl
 *
 */
@SuppressWarnings("rawtypes")
public class CourseProxy<T,C extends AbstractCourse> implements CourseOperation<C>,CourseProxyInterface<T,C>{
	/** 用于计算产生字面值的方法栈长是否合法，
	 * 如果别的方法中调用该类中的START()或START(args)方法（仅开发过程中可设置，对外隐藏），需要相应的+1*/
	byte level = 1;
	
	volatile int courseCount;
	/**最后产生的course对象，无论什么方法，要求每次产生新的course都必须移除旧的course*/
	C newest;
	/**share区，用于缓存course对象*/
	HashMap<String,C> shareSpace = new HashMap<String,C>();
	
	protected DomainService domainService;

	CourseRepository<T,C> repository;
	
	
	/**并集 */
	public static final String U = "U";
	/**交集 */
	public static final String I = "I";
	/**补集*/
	public static final String C = "C";
	/**前置并集 */
	public static final String UB = "UB";
	/**后置并集 */
	public static final String UA = "UA";
	/**前置混合 */
	public static final String MB = "MB";
	/**后置混合 */
	public static final String MA = "MA";
	/**正交 */
	public static final String M = "M";
	/**反交 */
	public static final String RM = "RM";
	/**左补 */
	public static final String CB = "CB";
	/**右补 */
	public static final String CA = "CA";//
	
	
	public static final String[] Model = {U,I,C,UB,UA,MB,MA,M,RM,CB,CA};
	
	private static int getResultTimeout = 3;
	
	@Override
	public C getCurrCourse() {
		return newest;
	}
	
	@Override
	public C removeCurrCourse() {
		C old = getCurrCourse();
		newest = null;
		return old;
	}
	
	@Override
	public void setCurrCourse(C course) {
		newest = course;
	}
	
	@Override
	public C getCourse(String id) {
		return shareSpace.get(id);
	}
	
	@Override
	public void setCourse(String id,C course) {
		shareSpace.put(id, course);
	}
	
	@Override
	public C removeCourse(String id) {
		return shareSpace.remove(id);
	}
	
	/**
	 * 初始化一个course
	 */
	@Override
	public C initCourse(String id,C course,CourseProxyInterface<T,C> proxy,byte status) {
		//course.domainService = service;
		course.setId(id);
		course.proxy = proxy;
		course.setStatus(status);
		course.init(course);
		return course;
	}
	
	/**
	 * 初始化一个course并发送到factory的course集合中，
	 * 并且判断存入的course与刚刚创建的course是否引用相同，
	 * 如果不相同则抛出异常
	 * @return 返回一个根节点
	 */
	private C begin() {
		Thread t = Thread.currentThread();
		//调整该方法的位置需要修改length的值，每多一个上级方法调用length-1
		//System.out.println(t.getStackTrace().length-level);
		@SuppressWarnings("unchecked")
		C course = (C) EntityFactory.putCourse(getCurrCourse(), t, t.getStackTrace().length-level);
		if(course.getStatus()==Course.WAIT) {
			return course;
		}
		//如果集合返回的不是刚刚创建的对象
		return null;
	}
	
	
	public CourseProxy() {}

	public CourseProxy(DomainService domainService) {
		this.domainService = domainService;
	}
	
	public HashMap<String, C> getShareSpace() {
		return shareSpace;
	}
	
	@Override
	public void setRepository(CourseRepository<T,C> repository) {
		this.repository = repository;
	}

	@Override
	public DomainService getDomainService() {
		return domainService;
	}

	@Override
	public void setDomainService(DomainService domainService) {
		this.domainService = domainService;
	}
	
	public static int getGetResultTimeout() {
		return getResultTimeout;
	}

	public static void setGetResultTimeout(int getResultTimeout) {
		CourseProxy.getResultTimeout = getResultTimeout;
	}

	/**
	 * 获取course list的友好信息
	 * @see CourseProxy#getInfo()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();	
		for(String key:shareSpace.keySet()) {
			C course = shareSpace.get(key);
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
	@SuppressWarnings("unchecked")
	protected C addCourse(String id,boolean cover) {
		C old = removeCurrCourse();
		C newc = null;
		try {
			//获取父类泛型参数，默认第二个泛型参数为泛型course，没有参数或者没提供，使用默认的course
			Type type = this.getClass().getGenericSuperclass();
			if(type==Object.class) {
				newc = (C) CourseFactory.create(Course.class);
			}else {
				Type[] types = ((ParameterizedType)type).getActualTypeArguments();
				if(types.length<2)
					newc = (C) CourseFactory.create(Course.class);
				else
					newc = CourseFactory.create((Class<C>) types[1]);				
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initCourse(id,newc,this,Course.WAIT);
		if(cover)
			setCourse(old.id, newc);
		setCurrCourse(newc);
		return old;
	}
	
	
	/*------如果希望调用者只能以匿名类的形式使用，这些方法或重写应当为protected------*/
	
	
	/**
	 * 开始一个course的方法，通过该方法可以链式执行GET ADD PUT REMVE等方法
	 * @return 返回一个根节点
	 */
	public C START() {
		addCourse(String.valueOf(System.currentTimeMillis()),false);
		return begin();
	}
	
	/**
	 * 在开始course时提供一个id作为它的标识，同时该course在end之后会被缓存，
	 * 如果执行时该id的course存在缓存，则使用缓存的course。
	 * 如果取出的course已经被缓存，则后面\重复的修改不会生效
	 * @param id 这个course的标识，不能包含空格
	 * @return
	 */
	public C START(String id) {
		C old = null;
		old = getCourse(id);
		if(old!=null) {
			setCurrCourse(old);
			return old;
		}
		addCourse(id,false);
		return begin();
	}
	
	/**
	 * 带缓存的FORK，
	 * 根据sharespace中的一个course创建分支引用，如果对应id的course存在，
	 * 则进行分支，否则不进行分支，并且按照START(id)模式进行
	 * @param id
	 * @return
	 */
	public C FORKM(String id) {
		C course = getCourse(id);
		if(course!=null) {
			addCourse(id+"_FM_"+System.currentTimeMillis(),false);
			getCurrCourse().isForkm = true;
			cross(id,getCurrCourse());
		}else {
			addCourse(id+"_NFM_"+System.currentTimeMillis(),false);
		}
		return begin();
	}
	
	/**
	 * 根据sharespace中的一个course创建分支引用，如果对应id的course存在，
	 * 则进行分支，否则不进行分支，并且按照START()模式进行
	 * @param id
	 * @return
	 */
	public C FORK(String id) {
		C course = getCourse(id);
		if(course!=null) {
			addCourse(id+"_F_"+System.currentTimeMillis(),false);
			getCurrCourse().isFork = true;
			cross(id,getCurrCourse());
		}else {
			addCourse(id+"_NF_"+System.currentTimeMillis(),false);
		}		
		return begin();
	}
	
	/**
	 * 将fork和master关联
	 * @param id
	 * @param course
	 * @return
	 */
	public C cross(String id,C course) {
		course.origin = getCourse(id).next;
		return course;
	}
	
	/**
	 * 将当前course标记为end状态并放入sharespace
	 * 1、如果course没有一个有效的id则不会放入sharespace
	 * 2、如果没有对当前course进行end，在下一次start新course时当前course会被取代。
	 * 3、分支course不会放入sharespace
	 */
	@Override
	public void END() {
		C course = getCurrCourse();
		if(course.getStatus()==Course.END && course.id!=null && !course.isFork) {
			setCourse(course.id, course);			
		}else if(course.getStatus()==Course.END && course.id==null){
			course.id = String.valueOf(System.currentTimeMillis());
		}
	}

	/**
	 * 直接执行当前的一条course语句
	 * @return
	 */
	public T execute() {
		return execute(getCurrCourse());
	}
	
	@Override
	public T execute(C course) {
		return repository.query(course);
	}
	
	@Override
	public Object execute(String id) {
		C course = getCourse(id);
		return executeGeneral(course);
	}

	public Object executeGeneral(C course) {
		if(course!=null && course.next.type==CourseType.get) 
			return execute(course);
		else
			return commit(course);
	}
	
	/**
	 * 直接提交当前的一条course语句
	 * @return
	 */
	public int commit() {
		return commit(getCurrCourse());
	}

	@Override
	public int commit(C course) {
		return repository.commit(course);
	}

	private <K> CompletableFuture<CourseResult<T>> applyFutureResult(C course,K t){
		return CompletableFuture.supplyAsync(()->{
			setObject(t,(K)executeGeneral(course));
			return course.getResult();
		});
	}

	private <O, K> void setObject(O obj,K t) {
		obj = (O) t;
	}

	@Override
	public T executeFuture() {
		T t = null;
		C course = getCurrCourse();
		if(course.getFutureResult()==null) {
			course.setResult(applyFutureResult(course,t));
		}
		return t;
	}

	@Override
	public int commitFuture() {
		int t = 0;
		C course = getCurrCourse();
		if(course.getFutureResult()==null) {
			course.setResult(applyFutureResult(course,t));
		}
		return t;
	}

	/**
	 * 将当前proxy对象移交仓储，仓储根据不用的proxy实例和泛化执行对应的操作
	 * @return
	 */
	public int push() {
		return repository.fromProxy(this);
	}
	
	
	/**
	 * 一系列的字面值参数的开头，例如：如果在GET($(),user.getName(),user.getId())之前，
	 * 在course方法以外的地方使用了User.getName()等方法，
	 * 则$()方法是必需出现在参数的第一位，以抹除之前无关的字面值。
	 * @return null
	 */
	public Object $() {
		if(getCurrCourse().literal!=null && getCurrCourse().literal.size()>0)
			getCurrCourse().literal.clear();
		return null;
	}
	
	/**
	 * 方法引用，以lambda的方式提供方法字面参数，
	 * 如GET($(user::getName))相当于GET(user.getName()),当有多个方法字面值需要获取，最好用该方式。<p>
	 * 例如：GET($(user::getName,user::getId,user,user::getCode),首项为lambda的情况下不需要$()。
	 * 如果字面值中还有user.getName()这样的方法引用，那么lambda的任意一个表达式必需写在字面值参数的第一位。
	 * 
	 * @param lt 实体类的lambda表达式，如user::getName，建议采用此种语法而非()->user.getName(),
	 * 因为这种写法会带来隐患，比如可在lambda中执行多个句柄，这样会令字面值参数跟期望的不一致，
	 * 尽管Course已经尽力避免这种情况，但是无法完全阻止通过这种手段输入，这是由于java语言机制限制了，因此在实际使用中
	 * 需要多加留意。并且，在其他层面（如domainMatch、EntityFactory）会对该问题进行原则上的处理。
	 * @return null
	 */
	public Object $(Literal ...lt) {
		C course = getCurrCourse();
		if(course.getStatus()==Course.WAIT)
			if(course.literal!=null && course.literal.size()>0)
				course.literal.clear();
		course.setStatus(Course.LAMBDA);
		for(Literal li:lt) {
			li.literal();
		}
		course.setStatus(Course.METHOD);
		return null;
	}
	
	/**
	 * 可获取三元运算结果的字面值
	 * @param obj @see {@link CourseProxy#$(Literal...)}
	 * @return
	 */
	public Object te(Object obj) {
		C course = getCurrCourse();
		course.literal_te.add(course.literal.get(course.literal.size()-1));
		course.literal.remove(course.literal.size()-1);
		course.literal.remove(course.literal.size()-1);
		return Course.Te.te;
	}
	
	/**
	 * 创建子节点通用函数
	 * @param function
	 * @return
	 */
	protected <R> R createNode(Function<AbstractCourse,R> function) {
		AbstractCourse course = getCurrCourse();
		AbstractCourse last = null;
		//搜索最后主干节点
		while(course!=null) {
			last = course;
			course = course.next;
		}
		R r = function.apply(last);
		return r;
	}

	/**
	 * 创建condition类型子节点
	 * @param obj
	 * @return
	 */
	public Condition $(Object...obj){
		//TODO 当操作类型eq中作为子节点如何处置其中的entity等信息
		return createNode((last)->new Condition(last,CourseType.by,true,obj));
	}
	
	/**
	 * 创建count聚合函数
	 * @param obj
	 * @return
	 */
	public Count count(Object obj) {
		return createNode((last)->new Count(last,CourseType.count,true,obj));
	}
	
}

package com.cosog.thread.calculate;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
	private static Map<String, Object> map = new Hashtable<>();
	private ThreadPoolExecutor executor;
	/**
	* 阻塞任务队列数
	*/
	private int wattingCount;
	/**
	* 线程池的名字，e.g:子系统的包名(com.tool.me)
	*/
	@SuppressWarnings("unused")
	private String name;
	
	/**
	* 创建线程池
	*
	* @param name
	* 线程池的名字，eg:子系统的包名(com.tool.me)
	* @param corePoolSize
	* 核心线程池大小 the number of threads to keep in the pool, even if
	* they are idle, unless {@code allowCoreThreadTimeOut} is set
	* @param maximumPoolSize
	* 最大线程池大小 the maximum number of threads to allow in the pool
	* @param keepAliveTime
	* 线程池中超过corePoolSize数目的空闲线程最大存活时间 when the number of threads is
	* greater than the core, this is the maximum time that excess
	* idle threads will wait for new tasks before terminating.
	* @param unit
	* keepAliveTime时间单位 the time unit for the {@code keepAliveTime}
	* argument
	* @param workQueue
	* 阻塞任务队列 the queue to use for holding tasks before they are
	* executed. This queue will hold only the {@code Runnable} tasks
	* submitted by the {@code execute} method.
	*/
	@SuppressWarnings("rawtypes")
	public ThreadPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,BlockingQueue workQueue) {
		synchronized (map) {
			this.name = name;
			this.wattingCount = workQueue.size();
			String key=this.name;
//			String key = buildKey(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue.size(), "#");
			if (map.containsKey(key)) {
				executor = (ThreadPoolExecutor) map.get(key);
			} else {
				executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
				map.put(key, executor);
			}
		}
	}
	
	/**
	* 创建线程池
	*
	* @param name
	* 线程池的名字，eg:子系统的包名(com.tool.me)
	* @param corePoolSize
	* 核心线程池大小 the number of threads to keep in the pool, even if
	* they are idle, unless {@code allowCoreThreadTimeOut} is set
	* @param maximumPoolSize
	* 最大线程池大小 the maximum number of threads to allow in the pool
	* @param keepAliveTime
	* 线程池中超过corePoolSize数目的空闲线程最大存活时间 when the number of threads is
	* greater than the core, this is the maximum time that excess
	* idle threads will wait for new tasks before terminating.
	* @param unit
	* keepAliveTime时间单位 the time unit for the {@code keepAliveTime}
	* argument
	* @param wattingCount
	* 阻塞任务队列数
	*/
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ThreadPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,int wattingCount) {
		synchronized (map) {
			this.name = name;
			this.wattingCount = (int) (wattingCount * 1.5);
			if(this.wattingCount<=0){
				this.wattingCount=Integer.MAX_VALUE;
			}
			String key=this.name;
//			String key = buildKey(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, wattingCount, "#");
			if (map.containsKey(key)) {
				executor = (ThreadPoolExecutor) map.get(key);
			} else {
				LinkedBlockingQueue linkedBlockingQueue=new LinkedBlockingQueue<>(this.wattingCount);
				executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit,linkedBlockingQueue);
				map.put(key, executor);
			}
		}
	}
	
	/**
	* 组装map中的key
	* @param name
	* 线程池的名字，eg:子系统的包名(com.tool.me)
	* @param corePoolSize
	* 核心线程池大小 the number of threads to keep in the pool, even if
	* they are idle, unless {@code allowCoreThreadTimeOut} is set
	* @param maximumPoolSize
	* 最大线程池大小 the maximum number of threads to allow in the pool
	* @param keepAliveTime
	* 线程池中超过corePoolSize数目的空闲线程最大存活时间 when the number of threads is
	* greater than the core, this is the maximum time that excess
	* idle threads will wait for new tasks before terminating.
	* @param unit
	* keepAliveTime时间单位 the time unit for the {@code keepAliveTime}
	* argument
	* @param workQueue
	* 阻塞任务队列 the queue to use for holding tasks before they are
	* executed. This queue will hold only the {@code Runnable} tasks
	* submitted by the {@code execute} method.
	* @param delimiter
	* 分割符
	*/
	private String buildKey(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,int wattingCount, String delimiter) {
		StringBuilder result = new StringBuilder();
		result.append(name).append(delimiter);
		result.append(corePoolSize).append(delimiter);
		result.append(maximumPoolSize).append(delimiter);
		result.append(keepAliveTime).append(delimiter);
		result.append(unit.toString()).append(delimiter);
		result.append(wattingCount);
		return result.toString();
	}
	
	public void removeThreadPoolExecutor(String key){
		if (map.containsKey(key)) {
			map.remove(key);
			this.executor = null;
		}
	} 
	
	/**
	* 添加任务到线程池(execute)中
	* @param runnable the task to execute
	*/

	public void execute(Runnable runnable) {
		checkQueneSize();
		executor.execute(runnable);
//		executor.submit(runnable);
	}

	private void checkQueneSize() {
		while (getTaskSzie() >= wattingCount) {//如果线程池中的阻塞队列数 > wattingCount 则继续等待
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 判断线程池的所有任务是否执行完
	 */
	public boolean isCompletedByTaskCount() {
	    return executor.getTaskCount() == executor.getCompletedTaskCount();
	}
	
	/**
	* Returns the number of elements in this collection. If this collection
	* contains more than Integer.MAX_VALUE elements, returns
	* Integer.MAX_VALUE.
	*
	* @return the number of elements in this collection
	*/
	public int getTaskSzie(){
		return executor.getQueue().size();
	}

	public ThreadPoolExecutor getExecutor() {
		return executor;
	}

	public void setExecutor(ThreadPoolExecutor executor) {
		this.executor = executor;
	}
	
	public List<Runnable> shutdown() {
		return this.executor.shutdownNow();
	}
}

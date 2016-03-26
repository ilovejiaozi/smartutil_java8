package com.xiaohanlin.smartutil.eventbus;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import com.xiaohanlin.smartutil.log.SmartLogger;

/**
 * 通用的事件监听, 只有该类为外部调用<br>
 * 
 * @author jiaozi
 */
public class SmartEventBus {

	private final String identifier;

	private final SmartSubscriberRegistry subscribers = new SmartSubscriberRegistry(this);

	private final SmartEventDispatcher dispatcher;

	/**
	 * 默认是同步的分发执行事件<br>
	 * 默认是使用自带的异常处理SmartSubscriberExceptionHandler
	 */
	public SmartEventBus(String identifier, SmartLogger logger) {
		this(identifier, SmartEventDispatcher.Immediate(logger));
	}

	public SmartEventBus(String identifier, SmartEventDispatcher dispatcher) {
		this.identifier = requireNonNull(identifier);
		this.dispatcher = requireNonNull(dispatcher);
	}

	public final String identifier() {
		return identifier;
	}

	// ==========接口==========
	public void registerByInterface(SmartSubscriber subscriber, Class<?> eventClass, byte priority) {
		requireNonNull(subscriber);
		requireNonNull(eventClass);
		subscribers.registerByInterface(subscriber, eventClass, priority);
	}

	public void registerByInterface(SmartSubscriber subscriber, Class<?> eventClass) {
		registerByInterface(subscriber, eventClass, SmartSubscribe.DEFAULT_PRIORITY);
	}

	public void unregisterByInterface(SmartSubscriber SmartSubscriber, Class<?> eventClass) {
		subscribers.unregisterByInterface(SmartSubscriber, eventClass);
	}

	public void unregisterByInterface(SmartSubscriber SmartSubscriber, Class<?> eventClass, byte priority) {
		subscribers.unregisterByInterface(SmartSubscriber, eventClass, priority);
	}

	// ==========lambda==========

	public <S, E> void registerByLambda(SmartEventHandleFunction<S, E> fun, S subscriber, Class<E> eventClass, byte priority) {
		requireNonNull(subscriber);
		requireNonNull(eventClass);
		subscribers.registerByLambda(fun, subscriber, eventClass, priority);
	}

	public <S, E> void registerByLambda(SmartEventHandleFunction<S, E> fun, S subscriber, Class<E> eventClass) {
		registerByLambda(fun, subscriber, eventClass, SmartSubscribe.DEFAULT_PRIORITY);
	}

	// ==========反射==========
	public void registerByReflection(Object object) {
		requireNonNull(object);
		subscribers.registerByReflect(object);
	}

	public void unregisterByReflection(Object object) {
		requireNonNull(object);
		subscribers.unregisterByReflect(object);
	}

	// ==========触发事件==========

	public void post(Object event) {
		requireNonNull(event);
		post(event.getClass(), event);
	}

	public void post(int event) {
		post(int.class, event);
	}

	public void post(long event) {
		post(long.class, event);
	}

	public void post(short event) {
		post(short.class, event);
	}

	public void post(byte event) {
		post(byte.class, event);
	}

	public void post(double event) {
		post(double.class, event);
	}

	public void post(float event) {
		post(float.class, event);
	}

	public void post(boolean event) {
		post(boolean.class, event);
	}

	public void post(char event) {
		post(char.class, event);
	}

	private void post(Class<?> clazz, Object event) {
		Iterator<SmartSubscriber> eventSubscribers = subscribers.getSubscribers(clazz);
		if (eventSubscribers.hasNext()) {
			dispatcher.dispatch(event, eventSubscribers);
		}
	}
}

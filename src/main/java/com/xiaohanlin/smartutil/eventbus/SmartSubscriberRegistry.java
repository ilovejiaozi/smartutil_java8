package com.xiaohanlin.smartutil.eventbus;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.xiaohanlin.smartutil.SmartStringUtil;
import static com.xiaohanlin.smartutil.SmartPreconditionUtil.*;


/**
 * 管理所有订阅者,Subscribers<br>
 * 因为有优先级，所有使用TreeMap<br>
 * 
 * @author jiaozi
 *
 */
final class SmartSubscriberRegistry {
	private static final short MAX_PRORITY = Byte.MAX_VALUE + 1;
	private final ConcurrentHashMap<Class<?>, ConcurrentSkipListMap<Short, List<SmartSubscriber>>> eventTypeSubscribers = new ConcurrentHashMap<>();
	private final SmartEventBus bus;

	SmartSubscriberRegistry(SmartEventBus bus) {
		this.bus = requireNonNull(bus);
	}

	// ==========反射==========
	final synchronized void registerByReflect(Object listener) {
		HashMap<Short, List<Method>> prioritySubscribers = getAnnotatedMethods(listener);
		for (Map.Entry<Short, List<Method>> en : prioritySubscribers.entrySet()) {
			short priotity = en.getKey();
			for (Method m : en.getValue()) {
				Class<?> eventType = m.getParameters()[0].getType();
				List<SmartSubscriber> sortedMapSubscribers = getEventTypeSubscribers(eventType, priotity);
				sortedMapSubscribers.add(SmartReflectMethodSubscriber.create(bus, listener, m));
			}
		}
	}

	final synchronized void unregisterByReflect(Object listener) {
		HashMap<Short, List<Method>> prioritySubscribers = getAnnotatedMethods(listener);
		for (Map.Entry<Short, List<Method>> en : prioritySubscribers.entrySet()) {
			short priotity = en.getKey();
			for (Method m : en.getValue()) {
				Class<?> eventType = m.getParameterTypes()[0];
				List<SmartSubscriber> sortedMapSubscribers = getEventTypeSubscribers(eventType, priotity);
				sortedMapSubscribers.remove(SmartReflectMethodSubscriber.create(bus, listener, m));
			}
		}
	}

	// ==========lambda==========

	final synchronized <S, E> void registerByLambda(SmartEventHandleFunction<S, E> fun, S subscriber, Class<E> eventClass, byte priority) {
		short finalPriority = (short) (MAX_PRORITY - priority);
		List<SmartSubscriber> subscriberList = getEventTypeSubscribers(eventClass, finalPriority);
		subscriberList.add(new SmartEventHandleFunctionSubscriber<S, E>(this.bus, fun, subscriber));
	}

	// =========接口interface==========

	final synchronized void registerByInterface(SmartSubscriber subscriber, Class<?> eventClass, byte priority) {
		short finalPriority = (short) (MAX_PRORITY - priority);
		List<SmartSubscriber> subscriberList = getEventTypeSubscribers(eventClass, finalPriority);
		subscriberList.add(subscriber);
	}

	final synchronized void unregisterByInterface(SmartSubscriber subscriber, Class<?> eventClass) {
		unregisterWithoutPriorty(subscriber, eventClass);
	}

	final synchronized void unregisterByInterface(SmartSubscriber subscriber, Class<?> eventClass, byte priority) {
		short finalPriority = (short) (MAX_PRORITY - priority);
		unregisterWithPriorty(subscriber, eventClass, finalPriority);
	}

	// ==========内部其他方法==========

	private void unregisterWithoutPriorty(SmartSubscriber subscriber, Class<?> eventClass) {
		ConcurrentSkipListMap<Short, List<SmartSubscriber>> sortedMap = eventTypeSubscribers.get(eventClass);
		if (sortedMap == null) {
			return;
		}
		for (List<SmartSubscriber> subscribersList : sortedMap.values()) {
			if (subscribersList.remove(subscriber)) {
				return;
			}
		}
	}

	private void unregisterWithPriorty(SmartSubscriber subscriber, Class<?> eventClass, short proprity) {
		ConcurrentSkipListMap<Short, List<SmartSubscriber>> sortedMap = eventTypeSubscribers.get(eventClass);
		if (sortedMap == null) {
			return;
		}

		List<SmartSubscriber> list = sortedMap.get(proprity);
		if (list == null) {
			return;
		}
		list.remove(subscriber);
	}

	private List<SmartSubscriber> getEventTypeSubscribers(Class<?> eventType, short priority) {
		ConcurrentSkipListMap<Short, List<SmartSubscriber>> sortedMap = eventTypeSubscribers.get(eventType);
		if (sortedMap == null) {
			sortedMap = new ConcurrentSkipListMap<>();
			eventTypeSubscribers.put(eventType, sortedMap);
		}
		List<SmartSubscriber> list = sortedMap.get(priority);
		if (list == null) {
			list = new ArrayList<>();
			sortedMap.put(priority, list);
		}
		return list;
	}

	private HashMap<Short, List<Method>> getAnnotatedMethods(Object listener) {
		HashMap<Short, List<Method>> priorityMethods = new HashMap<>();
		for (Class<?> clazz = listener.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
			Method[] declaredMethods = clazz.getDeclaredMethods();
			if (declaredMethods != null) {
				for (Method method : declaredMethods) {
					if (method.isAnnotationPresent(SmartSubscribe.class)) {
						checkArgument(method.getParameterCount() == 1, "Subscriber methods must have exactly 1 parameter, cannot be more or less. Pls check Class : %s , method :  %s ." + method.getName(), clazz.getName(), method.getName());
						SmartSubscribe[] annotations = method.getAnnotationsByType(SmartSubscribe.class);
						// 指定了某个特定的EventBus的identifier
						String busidentifier = annotations[0].smartEventBus();
						if (SmartStringUtil.isNotEmpty(busidentifier) && !SmartStringUtil.equals(busidentifier, this.bus.identifier())) {
							continue;
						}

						short priority = (short) (MAX_PRORITY - annotations[0].priority());
						List<Method> list = priorityMethods.get(priority);
						if (list == null) {
							list = new ArrayList<>();
							priorityMethods.put(priority, list);
						}
						list.add(method);
					}
				}
			}
		}
		return priorityMethods;
	}

	final Iterator<SmartSubscriber> getSubscribers(Class<?> clazz) {
		ConcurrentSkipListMap<Short, List<SmartSubscriber>> concurrentSkipListMap = eventTypeSubscribers.get(clazz);
		if (concurrentSkipListMap == null || concurrentSkipListMap.isEmpty()) {
			return Collections.emptyIterator();
		}
		ArrayList<SmartSubscriber> list = new ArrayList<>(concurrentSkipListMap.size());
		for (Map.Entry<Short, List<SmartSubscriber>> entry : concurrentSkipListMap.entrySet()) {
			list.addAll(entry.getValue());
		}
		return list.iterator();
	}
}

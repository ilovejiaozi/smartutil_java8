package com.xiaohanlin.smartutil.eventbus;

public class SmartEventHandleFunctionSubscriber<S, E> implements SmartSubscriber {

	private SmartEventBus bus;

	private SmartEventHandleFunction<S, E> eventHandleFunction;

	private S subscriber;

	public SmartEventHandleFunctionSubscriber(SmartEventBus bus, SmartEventHandleFunction<S, E> eventHandleFunction, S subscriber) {
		this.bus = bus;
		this.eventHandleFunction = eventHandleFunction;
		this.subscriber = subscriber;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void dispatchEvent(Object event) {
		this.eventHandleFunction.hanldleEvent(subscriber, (E) event);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bus == null) ? 0 : bus.hashCode());
		result = prime * result + ((eventHandleFunction == null) ? 0 : eventHandleFunction.hashCode());
		result = prime * result + ((subscriber == null) ? 0 : subscriber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmartEventHandleFunctionSubscriber<?, ?> other = (SmartEventHandleFunctionSubscriber<?, ?>) obj;
		if (bus == null) {
			if (other.bus != null)
				return false;
		} else if (!bus.equals(other.bus))
			return false;
		if (eventHandleFunction == null) {
			if (other.eventHandleFunction != null)
				return false;
		} else if (!eventHandleFunction.equals(other.eventHandleFunction))
			return false;
		if (subscriber == null) {
			if (other.subscriber != null)
				return false;
		} else if (!subscriber.equals(other.subscriber))
			return false;
		return true;
	}

	@Override
	public String getSmartSubscriberDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("SmartEventBus identify : ").append(this.bus.identifier()).append(" , subscriber class : " + subscriber.getClass().getName());
		return sb.toString();
	}
}

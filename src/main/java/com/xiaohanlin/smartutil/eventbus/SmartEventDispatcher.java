package com.xiaohanlin.smartutil.eventbus;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.concurrent.Executor;

import com.xiaohanlin.smartutil.log.SmartLogger;

/**
 * 内部默认实现两种分发机制，异步和同步
 * 
 * @author jiaozi
 */
public interface SmartEventDispatcher {

	public void dispatch(Object event, Iterator<SmartSubscriber> subscribers);

	/**
	 * 异步分发
	 * 
	 * @return
	 */
	public static SmartEventDispatcher Async(Executor executor, SmartLogger logger) {
		return new AsyncDispatcher(executor, logger);
	}

	public static SmartEventDispatcher Async(Executor executor, SmartLogger logger, SmartSubscriberExceptionHandler exceptionHandler) {
		return new AsyncDispatcher(executor, logger, exceptionHandler);
	}

	/**
	 * 同步分发
	 * 
	 * @author jiaozi
	 */
	public static SmartEventDispatcher Immediate(SmartLogger logger, SmartSubscriberExceptionHandler exceptionHandler) {
		return new ImmediateDispatcher(logger, exceptionHandler);
	}

	public static SmartEventDispatcher Immediate(SmartLogger logger) {
		return new ImmediateDispatcher(logger);
	}

	// ==========内部类==========

	public static abstract class KuEventDefaultDispatcher implements SmartEventDispatcher {
		protected SmartLogger logger;
		protected SmartSubscriberExceptionHandler exceptionHandler;

		KuEventDefaultDispatcher(SmartLogger logger, SmartSubscriberExceptionHandler exceptionHandler) {
			requireNonNull(logger);
			requireNonNull(exceptionHandler);
			this.logger = logger;
			this.exceptionHandler = exceptionHandler;
		}

		public abstract void dispatch(Object event, Iterator<SmartSubscriber> subscribers);
	}

	static class AsyncDispatcher extends KuEventDefaultDispatcher {

		private Executor executor;

		public AsyncDispatcher(Executor executor, SmartLogger logger) {
			super(logger, new LoggingHandler(logger));
			this.executor = executor;
		}

		public AsyncDispatcher(Executor executor, SmartLogger logger, SmartSubscriberExceptionHandler exceptionHandler) {
			super(logger, exceptionHandler);
			this.executor = executor;
		}

		@Override
		public void dispatch(Object event, Iterator<SmartSubscriber> subscribers) {

			executor.execute(new Runnable() {
				@Override
				public void run() {
					SmartSubscriber kuSubscriber = null;
					while (subscribers.hasNext()) {
						try {
							kuSubscriber = subscribers.next();
							kuSubscriber.dispatchEvent(event);
						} catch (Throwable e) {
							try {
								exceptionHandler.handleException(e, new SmartSubscriberExceptionContext(event, kuSubscriber));
							} catch (Throwable ex) {
								logger.error("<<AsyncDispatcher>>", ex);
							}
						}
					}
				}
			});
		}
	}

	static class ImmediateDispatcher extends KuEventDefaultDispatcher {
		ImmediateDispatcher(SmartLogger logger, SmartSubscriberExceptionHandler exceptionHandler) {
			super(logger, exceptionHandler);
		}

		ImmediateDispatcher(SmartLogger logger) {
			super(logger, new LoggingHandler(logger));
		}

		@Override
		public void dispatch(Object event, Iterator<SmartSubscriber> subscribers) {
			SmartSubscriber subscriber = null;
			while (subscribers.hasNext()) {
				try {
					subscriber = subscribers.next();
					subscriber.dispatchEvent(event);
				} catch (Throwable e) {
					try {
						exceptionHandler.handleException(e, new SmartSubscriberExceptionContext(event, subscriber));
					} catch (Throwable ex) {
						logger.error("<<AsyncDispatcher>>", ex);
					}
				}
			}
		}
	}

	static final class LoggingHandler implements SmartSubscriberExceptionHandler {
		private SmartLogger logger;

		public LoggingHandler(SmartLogger logger) {
			this.logger = logger;
		}

		@Override
		public void handleException(Throwable exception, SmartSubscriberExceptionContext context) {
			logger.error(message(context), exception);
		}

		private static String message(SmartSubscriberExceptionContext context) {
			return "<<SmartEventBus>>  dispatching event error , subscriber : " + context.getSubscriber() + " , event: " + context.getEvent();
		}
	}
}

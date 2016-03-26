package com.xiaohanlin.smartutil.eventbus;

/**
 * lambda 接口
 * @author jiaozi
 *
 * @param <S>
 * @param <E>
 */
@FunctionalInterface
public interface SmartEventHandleFunction<S, E> {
	public void hanldleEvent(S subscriber, E event);
}

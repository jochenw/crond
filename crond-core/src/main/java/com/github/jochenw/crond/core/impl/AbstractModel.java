package com.github.jochenw.crond.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.afw.di.api.IComponentFactoryAware;
import com.github.jochenw.afw.di.api.ILifecycleController.TerminableListener;
import com.github.jochenw.afw.di.api.LogInject;
import com.github.jochenw.crond.core.api.IModel;

import jakarta.inject.Inject;

public abstract class AbstractModel implements IModel, TerminableListener {
	private @Inject IComponentFactory componentFactory;
	private @LogInject ILog log;

	/** Called to start the model. Assumes, that required components have already
	 * been injected.
	 */
	@Override
	public void start() {}

	/** Called to stop the model.
	 */
	@Override
	public void shutdown() {}

	private final List<Listener> listeners = new ArrayList<>();

	@Override
	public void addListener(Listener pListener) {
		synchronized(listeners) {
			listeners.add(pListener);
		}
	}

	@Override
	public void removeListener(Listener pListener) {
		synchronized(listeners) {
			listeners.remove(pListener);
		}
	}

	/** Called to invoke the listeners by invoking the
	 * given consumer.
	 * @param pEvent The event notification; typically the consumer
	 *   will invoke a method on the {@link Listener} {@code pEvent}.
	 *   Events are supposed to be very quick, rather than blocking
	 *   resources, that are reserved, while the event operates.
	 */
	protected void note(Consumer<Listener> pEvent) {
		synchronized(listeners) {
			listeners.forEach(pEvent);
		}
	}
}

package com.github.jochenw.crond.backend.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.jochenw.afw.core.function.Functions.FailableConsumer;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.di.util.Exceptions;
import com.github.jochenw.crond.backend.model.IModel;


/** Abstract base implementation of a {@link IModel}.
 */
public abstract class AbstractModel implements IModel {
	public static class RegisteredListener {
		private boolean active;
		private final Listener listener;

		public RegisteredListener(Listener pListener) {
			listener = pListener;
			active = true;
		}
	}
	private final List<RegisteredListener> listeners = new ArrayList<>();

	@Override
	public void addListener(Listener pListener) {
		final Listener listener = Objects.requireNonNull(pListener, "Listener");
		synchronized(listeners) {
			final RegisteredListener regListener = new RegisteredListener(listener);
			listeners.add(regListener);
			regListener.active = true;
		}
	}

	@Override
	public void removeListener(Listener pListener) {
		final Listener listener = Objects.requireNonNull(pListener, "Listener");
		synchronized(listeners) {
			for (Iterator<RegisteredListener> iter = listeners.iterator();  iter.hasNext();  ) {
				final RegisteredListener regListener = iter.next();
				if (regListener.listener.equals(listener)) {
					synchronized(regListener) {
						regListener.active = false;
					}
					iter.remove();
					return;
				}
			}
		}
	}

	/** Called to notify the listeners by invoking the given action
	 * on all active, and registered listeners.
	 * @param pAction The action, which is being invoked on a listener.
	 */
	protected void notifyListeners(FailableConsumer<Listener,?> pAction) {
		synchronized(listeners) {
			for (RegisteredListener regListener : listeners) {
				final boolean active;
				synchronized(regListener) {
					active = regListener.active;
				}
				if (active) {
					try {
						pAction.accept(regListener.listener);
					} catch (Throwable t) {
						throw Exceptions.show(t);
					}
				}
			}
		}
	}
}

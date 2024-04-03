package com.github.jochenw.crond.backend.model;

/** This interface is the main access point for frontends, and
 * external API's to the backend. In an MVC architecture, the
 * model is designed to serve as the controller.
 */
public interface IModel {
	/** A listener is receiving notifications from the model
	 * about changes. (A notification is defined as an invocation of a listeners method.)
	 * A listener must be registered in the model
	 * by invoking {@link IModel#addListener(IListener)},
	 */
    public interface IListener {
    	
    }

    /** This method must be used to register a {@link IModel.IListener listener}
     * in the model. Once registered, the listener will receive notifications
     * from the model. A registered listener should also be deregistered by
     * invoking {@link #removeListener(IListener)}.
     * @param pListener The listener, which is being registered.
     * @see #removeListener(IListener)
     */
    public void addListener(IListener pListener);
    /** This method must be used to deregister a {@link IModel.IListener listener},
     * which has been registered previously.
     * @param pListener The listener, which is being deregistered.
     * @see #addListener(IListener)
     */
    public void removeListener(IListener pListener);
}

package com.github.jochenw.crond.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.github.jochenw.crond.backend.model.beans.Execution;
import com.github.jochenw.crond.backend.model.beans.Job;
import com.github.jochenw.crond.backend.model.beans.User;

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
    public static class Listener {
    	/** Notification: Indicates, that the given job has been created, and
    	 * was added to the model.
    	 * @param pJob The created job.
    	 */
    	public void jobCreated(Job pJob) {}
    	/** Notification: Indicates, that the given execution has been created, and
    	 * was added to the model.
    	 * @param pExecution The created execution.
    	 */
    	public void executionCreated(Execution pExecution) {}
    	/** Notification: Indicates, that the given user has been created, and
    	 * was added to the model.
    	 * @param pUser The created user.
    	 */
    	public void userCreated(User pUser) {}
    	/** Notification: Indicates, that the job with the given id has been
    	 * updated.
    	 * @param pJob The updated job.
    	 */
    	public void jobUpdated(Job pJob) {}
    	/** Notification: Indicates, that the execution with the given id has been
    	 * updated.
    	 * @param pExecution The updated execution.
    	 */
    	public void executionUpdated(Execution pExecution) {}
    	/** Notification: Indicates, that the user with the given id has been
    	 * updated.
    	 * @param pUser The updated execution.
    	 */
    	public void userUpdated(User pUser) {}
    	/** Notification: Indicates, that the job with the given id has been
    	 * deleted.
    	 * @param pJobId The deleted jobs id.
    	 */
    	public void jobDeleted(Job.Id pJobId) {}
    	/** Notification: Indicates, that the execution with the given id has been
    	 * deleted.
    	 * @param pExecutionId The deleted executions id.
    	 */
    	public void executionDeleted(Execution.Id pExecutionId) {}
    	/** Notification: Indicates, that the user with the given id has been
    	 * deleted.
    	 * @param pUserId The deleted users id.
    	 */
    	public void userDeleted(User.Id pUserId) {}
    }

    /** This method must be used to register a {@link IModel.IListener listener}
     * in the model. Once registered, the listener will receive notifications
     * from the model. A registered listener should also be deregistered by
     * invoking {@link #removeListener(IListener)}.
     * @param pListener The listener, which is being registered.
     * @see #removeListener(IListener)
     */
    public void addListener(Listener pListener);
    /** This method must be used to deregister a {@link IModel.IListener listener},
     * which has been registered previously.
     * @param pListener The listener, which is being deregistered.
     * @see #addListener(IListener)
     */
    public void removeListener(Listener pListener);

    /** Queries for the job with the given id.
     * @param pId The requested job's id.
     * @return The requested job, or null, if no such
     *   job is available.
     */
    public Job getJob(Job.Id pId);

    /** Queries for the job with the given id.
     * @param pId The requested job's id.
     * @return The requested job, or null, if no such
     *   job is available.
     */
    public default Job getJob(String pId) {
    	return getJob(new Job.Id(pId));
    }
 
    /** Queries for all the jobs.
     * @param pConsumer The consumer, which is supposed to receive the jobs,
     *   one by one.
     */
    public void getJobs(Consumer<Job> pConsumer);

    /** Queries for all the jobs, and returns them as a (possibly empty)
     * list.
     * @return The created list, which is mutable.
     */
    public default List<Job> getJobs() {
    	final List<Job> list = new ArrayList<>();
    	getJobs(list::add);
    	return list;
    }
    
    /** Queries for the execution with the given id.
     * @param pId The requested executions id.
     * @return The requested execution, or null, if no such
     *   execution is available.
     */
    public Execution getExecution(Execution.Id pId);

    /** Queries for the execution with the given id.
     * @param pId The requested executions id.
     * @return The requested execution, or null, if no such
     *   execution is available.
     */
    public default Execution getExecution(String pId) {
    	return getExecution(new Execution.Id(pId));
    }

    /** Queries for all the executions.
     * @param pConsumer The consumer, which is supposed to receive the executions,
     *   one by one.
     */
    public void getExecutions(Consumer<Execution> pConsumer);

    /** Queries for all the executions, and returns them as a (possibly empty)
     * list.
     * @return The created list, which is mutable.
     */
    public default List<Execution> getExecutions() {
    	final List<Execution> list = new ArrayList<>();
    	getExecutions(list::add);
    	return list;
    }

    /** Queries for the user with the given id.
     * @param pId The requested users id.
     * @return The requested user, or null, if no such
     *   user is available.
     */
    public User getUser(User.Id pId);

    /** Queries for the user with the given id.
     * @param pId The requested users id.
     * @return The requested user, or null, if no such
     *   user is available.
     */
    public default User getUser(String pId) {
    	return getUser(new User.Id(pId));
    }

    /** Queries for all the users.
     * @param pConsumer The consumer, which is supposed to receive the users,
     *   one by one.
     */
    public void getUsers(Consumer<User> pConsumer);

    /** Queries for all the users, and returns them as a (possibly empty)
     * list.
     * @return The created list, which is mutable.
     */
    public default List<User> getUsers() {
    	final List<User> list = new ArrayList<>();
    	getUsers(list::add);
    	return list;
    }
 
    /** Creates, and persists a new job.
     * @param pJob The job, which is being created.
     * @return The created job, which is supposed to replace the given.
     */
    public Job createJob(Job pJob);

    /** Updates, and persists a job, after updating it.
     * @param pJob The job, which is being updated.
     * @return The updated job, which is supposed to replace the given.
     */
    public Job updateJob(Job pJob);

    /** Deletes a job, and removes it from the persisted storage.
     */
    public void deleteJob(Job.Id pJob);

    /** Creates, and persists a new execution.
     * @param pExecution The execution, which is being created.
     * @return The created execution, which is supposed to replace the given.
     */
    public Execution createExecution(Execution pExecution);

    /** Updates, and persists an execution.
     * @param pExecution The execution, which is being created.
     * @return The updated execution, which is supposed to replace the given.
     */
    public Execution updateExecution(Execution pExecution);

    /** Deletes an execution, and removes it from the persisted storage.
     */
    public void deleteExecution(Execution.Id pExecution);

    /** Creates, and persists a new user.
     * @param pUser The execution, which is being created.
     * @return The created execution, which is supposed to replace the given.
     */
    public User createUser(User pUser);

    /** Updates, and persists a user.
     * @param pUser The user, which is being created.
     * @return The updated user, which is supposed to replace the given.
     */
    public User updateUser(User pUser);

    /** Delete a user, and removes it from the persisted storage.
     */
    public void deleteUser(User.Id pUser);
}

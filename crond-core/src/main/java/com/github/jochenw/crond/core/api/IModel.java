package com.github.jochenw.crond.core.api;

import java.util.NoSuchElementException;

import com.github.jochenw.afw.core.function.Functions.FailableConsumer;
import com.github.jochenw.afw.core.util.Objects.DuplicateElementException;

/** The cron servers data model.
 */
public interface IModel {
	public interface Listener {
		/** A new user has been added to the model.
		 * @param pUser The new user.
		 */
		public default void userAdded(IModel.User pUser) {}
		/** An existing user has been updated in the model.
		 * @param pUser The updated user. The users id is still the same.
		 */
		public default void userUpdated(User pUser) {}
		/** An existing user has been removed from the model.
		 * @param pUser The deleted user.
		 */
		public default void userDeleted(User pUser) {}
		/** A new job has been added to the model.
		 * @param pJob The new job.
		 */
		public default void jobAdded(IModel.Job pJob) {}
		/** An existing job has been updated in the model.
		 * @param pJob The updated job. The job id is still the same.
		 */
		public default void jobUpdated(IModel.Job pJob) {}
		/** An existing job has been removed from the model.
		 * @param pJob The deleted job.
		 */
		public default void jobDeleted(IModel.Job pJob) {}
		/** The model has been reinitialized, and the listener
		 * is supposed to reload all data. (The reload is
		 * supposed not to occur within this method, but
		 * afterwards.)
		 */
		public default void initialized() {}
	}
	public interface User {
		/** The users id.
		 */
		public Long getId();
		/** The users email address. Also, the primary key of the user
		 * within the collection of all users.
		 */
		public String getEmail();
		/** Returns the users name.
		 * @return The users name.
		 */
		public String getName();
	}
	public interface Job {
		/** The jobs id.
		 */
		public Long getId();
		/** User id of the jobs owner.
		 */
		public Long getUserId();
		/** The jobs name. (Short description)
		 */
		public String getName();
	}

	public void addListener(Listener pListener);
	public void removeListener(Listener pListener);
	/** Creates a new user with the given email, and name.
	 * @param pEmail The created users email. (Must be unique.)
	 * @param pName The created users name.
	 * @return The created user, with a valid, and unique
	 *   {@link User#getId() user id}.
	 * @throws DuplicateElementException The email address isn't unique.
	 */
	public User addUser(String pEmail, String pName) throws DuplicateElementException;
	/** Creates a new job with the given user id, and name.
	 * @param pUserId The job owners {@link User#getId() user id}.
	 * @param pName The jobs name. The name is supposed to be unique
	 *   within all jobs, that belong to the same owner.
	 * @return The created job, with a valid, and unique
	 *   {@link Job#getId() job id}.
	 * @throws DuplicatelementException The combination of {@code pUserId},
	 *   and {@code pName} isn't unique.
	 * @throws NoSuchElementException The given user id isn't valid.
	 */
	public Job addJob(Long pUserId, String pName) throws DuplicateElementException,
	  NoSuchElementException;
	/** Updates an existing user.
	 * @param pUser The updated user, with the same
	 *   {@link User#getId() user id}. All other fields
	 *   ({@link User#getEmail() email}, {@link User#getName() name),
	 *   may be updated. However, the updated email address must
	 *   still be unique.
	 * @throws NoSuchElementException The given users
	 *   {@link User#getId()} is unknown.
	 * @throws DuplicateElementException
	 *   The updated email address isn't unique.
	 */
	public void updateUser(User pUser) throws NoSuchElementException, DuplicateElementException;
	/** Updates an existing job.
	 * @param pJob The updated job, with the same
	 *   {@link Job#getId() job id}. All other fields
	 *   ({@link Job#getUserId() user id}, {@link Job#getName() name})
	 *   may be updated. However, the combination of user id, and
	 *   name, must still be unique.
	 * @throws DuplicatelementException The combination of {@code pUserId},
	 *   and {@code pName} isn't unique.
	 * @throws NoSuchElementException The given user id isn't valid.
	 */
	public void updateJob(Job pJob)  throws NoSuchElementException, DuplicateElementException;
	/** Removes an existing user.
	 * @param pUserId The removed users id.
	 * @throws NoSuchElementException The user id is unknown.
	 */
	public void removeUser(Long pUserId) throws NoSuchElementException;
	/** Removes an existing job.
	 * @param pJobId The removed jobs id.
	 * @throws NoSuchElementException The given job id isn't valid.
	 */
	public void removeJob(Long pJobId) throws NoSuchElementException;
	/** Returns the user with the given user id, if such a user exists,
	 * or null.
	 * @return The user with the given user id, or null.
	 * @param pUserId The requested users user id.
	 */
	public User getUserById(Long pUserId);
	/** Returns the user with the given email address, if such a user
	 * exists, or null.
	 * @return The user with the given email address, or null.
	 * @param pEmail The requested users email address.
	 */
	public User getUserByEmail(String pEmail);
	/** Returns the job with the given job id, if such a job exists,
	 * or null.
	 * @param pJobId The requested jobs id.
	 * @return The job with the given job id, or null.
	 */
	public Job getJobById(Long pJobId);
	/** Returns the job with the given user id, and name,
	 * if such a job exists, or null.
	 * @param pUserId The requested jobs user id.
	 * @param pName The requested jobs name.
	 * @return The job with the given job id, or null.
	 */
	public Job getJobByUserIdAndName(Long pUserId, String pName);
	/** Iterates over all the users in the model.
	 * @param pConsumer The consumer, which is being invoked for
	 *   every user, one by one.
	 */
	public void forEachUser(FailableConsumer<User,?> pConsumer);
	/** Iterates over all the jobs in the model.
	 * @param pConsumer The consumer, which is being invoked for
	 *   every job, one by one.
	 */
	public void forEachJob(FailableConsumer<Job,?> pConsumer);
}

package com.github.jochenw.crond.core.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.xml.sax.InputSource;

import com.github.jochenw.afw.core.function.Functions;
import com.github.jochenw.afw.core.function.Functions.FailableConsumer;
import com.github.jochenw.afw.core.util.Locked;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.core.util.Objects.DuplicateElementException;
import com.github.jochenw.crond.core.beans.JobImpl;
import com.github.jochenw.crond.core.beans.UserImpl;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/** An implementation of {@link IModel}, which persists data in a single
 * XML file. This is well suited for development, but not for productive
 * operation.
 */
public class XmlFileModel extends AbstractModel {
	private @Inject @Named(value="xml.model.file") Path modelFile;
	private final Locked<UserData> userData = new Locked<>(new UserData());
	private long maxUserId, maxJobId;

	public static class UserData {
		private final Map<Long,User> usersById = new HashMap<>();
		private final Map<String,User> usersByEmail = new HashMap<>();
		private final Map<Long,Job> jobsById = new HashMap<>();
		private final Map<String,Job> jobsByUserIdAndName = new HashMap<>();
	}

	@Override
	public void start() {
		readModelFile();
	}

	protected void readModelFile() {
		try (InputStream is = Files.newInputStream(modelFile);
			 BufferedInputStream bis = new BufferedInputStream(is)) {
			final InputSource isource = new InputSource(bis);
			isource.setSystemId(modelFile.toString());
			final XmlModelFileReader ch = XmlModelFileReader.read(isource);
			apply(ch);
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		} catch (Exception e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	protected void apply(XmlModelFileReader pData) {
		userData.runWriteLocked((ud) -> {
			ud.usersById.clear();
			ud.usersByEmail.clear();
			ud.jobsById.clear();
			ud.jobsByUserIdAndName.clear();
			maxUserId = 0;
			pData.getUsers().values().forEach((u) -> {
				addUser(ud, u);
			});
			pData.getJobs().values().forEach((j) -> {
				addJob(ud, j);
			});
		});
		note((l) -> l.initialized());
	}

	/** Called to add a new job to the model. Assumes, that the
	 * current thread holds an exclusive lock on {@link #userData}.
	 * @param pUserData The user data.
	 * @param pJob The new job.
	 */
	private void addJob(UserData pUserData, Job pJob) {
		final Long jobId = Objects.requireNonNull(pJob.getId());
		final Long userId = Objects.requireNonNull(pJob.getUserId());
		final String name = Objects.requireNonNull(pJob.getName());
		final String userIdAndName = asUserIdAndName(userId, name);
		if (pUserData.jobsById.put(jobId, pJob) != null) {
			throw new DuplicateElementException("Duplicate job id: " + jobId);
		}
		if (jobId.longValue() > maxJobId) {
			maxJobId = jobId.longValue();
		}
		if (pUserData.jobsByUserIdAndName.put(userIdAndName, pJob) != null) {
			throw new DuplicateElementException("Duplicate combination of user id, and name: " + userId + ", " + name);
		}
	}
	private String asUserIdAndName(Long pUserId, String pName) {
		return pUserId + ":" + pName;
	}
	/** Called to add a new user to the model. Assumes, that the
	 * current thread holds an exclusive lock on {@link #userData}.
	 * @param pUserData The user data, 
	 * @param pUser The new user.
	 */
	private void addUser(UserData pUserData, User pUser) {
		final Long userId = Objects.requireNonNull(pUser.getId());
		final String email = Objects.requireNonNull(pUser.getEmail());
		if (pUserData.usersById.put(userId, pUser) != null) {
			throw new DuplicateElementException("Duplicate user id: " + userId);
		}
		if (userId.longValue() > maxUserId) {
			maxUserId = userId.longValue();
		}
		if (pUserData.usersByEmail.put(email, pUser) != null) {
			throw new DuplicateElementException("Duplicate email address: " + email);
		}
	}

	@Override
	public User addUser(String pEmail, String pName) throws DuplicateElementException {
		final User user = userData.callWriteLocked((ud) -> {
			final long userId = Long.valueOf(maxUserId+1);
			final User u = UserImpl.of(Long.valueOf(userId), pEmail, pName);
			addUser(ud, u);
			save(ud);
			return u;
		});
		note((l) -> l.userAdded(user));
		return user;
	}

	@Override
	public void updateUser(User pUser) {
		final Long id = Objects.requireNonNull(pUser.getId());
		final String email = Objects.requireNonNull(pUser.getEmail());
		userData.runWriteLocked((ud) -> {
			ud.usersById.compute(id, (i,u) -> {
				if (u == null) {
					throw new NoSuchElementException("Unknown user id: " + i);
				} else {
					return pUser;
				}
			});
			ud.usersByEmail.compute(email, (e,u) -> {
				if (u != null) {
					if (!id.equals(u.getId())) {
						throw new DuplicateElementException("Duplicate email address: " + e);
					}
				}
				return pUser;
			});
			save(ud);
		});
		note((l) -> l.userUpdated(pUser));
	}

	@Override
	public User getUserById(Long pId) {
		return userData.callReadLocked((ud) -> {
			return ud.usersById.get(pId);
		});
	}

	@Override
	public void removeUser(Long pUserId) throws NoSuchElementException {
		final Long id = Objects.requireNonNull(pUserId);
		final User user = userData.callWriteLocked((ud) -> {
			final User u = ud.usersById.remove(id);
			if (u == null) {
				throw new NoSuchElementException("Unknown user id: " + id);
			}
			final String email = Objects.requireNonNull(u.getEmail());
			ud.usersByEmail.remove(email);
			save(ud);
			return u;
		});
		note((l) -> l.userDeleted(user));
	}

	/** Must be invoked, while synchronized on {@link #usersById}.
	 */
	protected void save(UserData pUserData) {
		new XmlModelFileWriter().write(modelFile, pUserData.usersById.values(),
				pUserData.jobsById.values(), false);
	}

	@Override
	public Job addJob(Long pUserId, String pName) throws DuplicateElementException, NoSuchElementException {
		final Job job = userData.callWriteLocked((ud) -> {
			final long jobId = Long.valueOf(maxJobId+1);
			final Job j = JobImpl.of(Long.valueOf(jobId), pUserId, pName);
			addJob(ud, j);
			save(ud);
			return j;
		});
		note((l) -> l.jobAdded(job));
		return job;
	}

	@Override
	public void updateJob(Job pJob) throws NoSuchElementException, DuplicateElementException {
		final Long id = Objects.requireNonNull(pJob.getId());
		final Long userId = Objects.requireNonNull(pJob.getUserId());
		final String name = Objects.requireNonNull(pJob.getName());
		final String userIdAndName = asUserIdAndName(userId, name);
		userData.runWriteLocked((ud) -> {
			ud.jobsById.compute(id, (i,j) -> {
				if (j == null) {
					throw new NoSuchElementException("Unknown job id: " + i);
				} else {
					return pJob;
				}
			});
			ud.jobsByUserIdAndName.compute(userIdAndName, (uian, j) -> {
				if (j != null) {
					if (!id.equals(j.getId())) {
						throw new DuplicateElementException("Duplicate combination of user id, and name: "
								+ userId + ", " + name);
					}
				}
				return pJob;
			});
			save(ud);
		});
		note((l) -> l.jobUpdated(pJob));
	}

	@Override
	public void removeJob(Long pJobId) throws NoSuchElementException {
		final Long id = Objects.requireNonNull(pJobId);
		final Job job = userData.callWriteLocked((ud) -> {
			final Job j = ud.jobsById.remove(id);
			if (j == null) {
				throw new NoSuchElementException("Unknown job id: " + id);
			}
			final Long userId = Objects.requireNonNull(j.getUserId());
			final String name = Objects.requireNonNull(j.getName());
			final String userIdAndName = asUserIdAndName(userId, name);
			ud.jobsByUserIdAndName.remove(userIdAndName);
			save(ud);
			return j;
		});
		note((l) -> l.jobDeleted(job));
	}

	@Override
	public User getUserByEmail(String pEmail) {
		final String email = Objects.requireNonNull(pEmail);
		return userData.callReadLocked((ud) -> ud.usersByEmail.get(email));
	}

	@Override
	public Job getJobById(Long pJobId) {
		final Long id = Objects.requireNonNull(pJobId);
		return userData.callReadLocked((ud) -> ud.jobsById.get(id));
	}

	@Override
	public Job getJobByUserIdAndName(Long pUserId, String pName) {
		final Long userId = Objects.requireNonNull(pUserId);
		final String name = Objects.requireNonNull(pName);
		final String id = asUserIdAndName(userId, name);
		return userData.callReadLocked((ud) -> ud.jobsByUserIdAndName.get(id));
	}

	@Override
	public void forEachUser(FailableConsumer<User, ?> pConsumer) {
		userData.runReadLocked((ud) -> ud.usersById.values().forEach((u) -> Functions.accept(pConsumer, u)));
	}

	@Override
	public void forEachJob(FailableConsumer<Job, ?> pConsumer) {
		userData.runReadLocked((ud) -> ud.jobsById.values().forEach((j) -> Functions.accept(pConsumer, j)));
	}
}

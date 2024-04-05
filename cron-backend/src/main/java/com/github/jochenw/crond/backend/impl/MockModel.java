package com.github.jochenw.crond.backend.impl;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.jochenw.afw.core.function.Functions.FailableConsumer;
import com.github.jochenw.crond.backend.model.beans.Execution;
import com.github.jochenw.crond.backend.model.beans.Job;
import com.github.jochenw.crond.backend.model.beans.Job.Id;
import com.github.jochenw.crond.backend.model.beans.User;

public class MockModel extends AbstractModel {
	private final Path modelFile;
	private final BiConsumer<Path,MockModel> persistor;
	private final BiConsumer<Path,MockModel> loader;
	private final Map<String,Job> jobs = new HashMap<>(); 
	private final Map<String,Execution> executions = new HashMap<>(); 
	private final Map<String,User> users = new HashMap<>(); 

	
	public MockModel(Path pModelFile,
			         BiConsumer<Path, MockModel> pPersistor, BiConsumer<Path, MockModel> pLoader) {
		modelFile = pModelFile;
		persistor = pPersistor;
		loader = pLoader;
		load();
	}

	protected void persist() {
		if (persistor != null) {
			persistor.accept(modelFile, this);
		}
	}

	protected void load() {
		synchronized(jobs) {
			jobs.clear();
		}
		synchronized(executions) {
			executions.clear();
		}
		synchronized(users) {
			users.clear();
		}
		if (loader != null) {
			loader.accept(modelFile, this);
		}
	}

	@Override
	public Job getJob(Id pId) {
		synchronized(jobs) {
			return jobs.get(pId.getId());
		}
	}

	@Override
	public void getJobs(Consumer<Job> pConsumer) {
		synchronized(jobs) {
			jobs.values().forEach(pConsumer);
		}
	}

	@Override
	public Execution getExecution(com.github.jochenw.crond.backend.model.beans.Execution.Id pId) {
		synchronized(executions) {
			return executions.get(pId.getId());
		}
	}

	@Override
	public void getExecutions(Consumer<Execution> pConsumer) {
		synchronized(executions) {
			executions.values().forEach(pConsumer);
		}
	}

	@Override
	public User getUser(com.github.jochenw.crond.backend.model.beans.User.Id pId) {
		synchronized(users) {
			return users.get(pId.getId());
		}
	}

	@Override
	public void getUsers(Consumer<User> pConsumer) {
		synchronized(users) {
			users.values().forEach(pConsumer);
		}
	}

	protected String newId(Map<String,?> pMap) {
		for (long l = pMap.size();  ;  l++) {
			final String id = String.valueOf(l);
			if (!pMap.containsKey(id)) {
				return id;
			}
		}
	}

	@Override
	public Job createJob(Job pJob) {
		final Job job;
		synchronized(jobs) {
			final String jobIdStr = newId(jobs);
			Job.Id jobId = new Job.Id(jobIdStr);
			job = new Job(jobId, pJob.getName(), pJob.getOwnerId());
			jobs.put(jobIdStr, job);
		}
		persistAndNotify((l) -> l.jobCreated(job));
		return job;
	}

	@Override
	public Job updateJob(Job pJob) {
		final Job.Id jobId = pJob.getId();
		if (jobId == null) {
			throw new NullPointerException("Missing job id");
		}
		synchronized(jobs) {
			if (jobs.put(jobId.getId(), pJob) == null) {
				throw new IllegalArgumentException("Unknown job id: " + jobId);
			}
		}
		persistAndNotify((l) -> l.jobUpdated(pJob));
		return pJob;
	}

	@Override
	public void deleteJob(Job.Id pJobId) {
		final Job.Id jobId = pJobId;
		if (jobId == null) {
			throw new NullPointerException("Missing parameter: Job id");
		}
		synchronized(jobs) {
			if (jobs.remove(pJobId.getId()) == null) {
				throw new IllegalArgumentException("Unknown job id: " + jobId);
			}
		}
		persistAndNotify((l) -> l.jobDeleted(jobId));
	}

	@Override
	public Execution createExecution(Execution pExecution) {
		final Execution execution;
		synchronized(executions) {
			final String executionIdStr = newId(executions);
			Execution.Id executionId = new Execution.Id(executionIdStr);
			execution = new Execution(executionId, pExecution.getJobId(),
					                  pExecution.getStartTime(), pExecution.getEndTime());
			executions.put(executionIdStr, execution);
		}
		persistAndNotify((l) -> l.executionCreated(execution));
		return execution;
	}

	@Override
	public Execution updateExecution(Execution pExecution) {
		final Execution.Id executionId = pExecution.getId();
		if (executionId == null) {
			throw new NullPointerException("Missing execution id");
		}
		synchronized(executions) {
			if (executions.put(executionId.getId(), pExecution) == null) {
				throw new IllegalArgumentException("Unknown execution id: " + executionId);
			}
		}
		persistAndNotify((l) -> l.executionUpdated(pExecution));
		return pExecution;
	}

	@Override
	public void deleteExecution(com.github.jochenw.crond.backend.model.beans.Execution.Id pExecutionId) {
		final Execution.Id executionId = pExecutionId;
		if (executionId == null) {
			throw new NullPointerException("Missing parameter: Execution id");
		}
		synchronized(executions) {
			if (executions.remove(executionId.getId()) == null) {
				throw new IllegalArgumentException("Unknown job id: " + executionId);
			}
		}
		persistAndNotify((l) -> l.executionDeleted(executionId));
	}

	@Override
	public User createUser(User pUser) {
		final User user;
		synchronized(users) {
			final String userIdStr = newId(users);
			User.Id userId = new User.Id(userIdStr);
			user = new User(userId, pUser.getName(), pUser.getEmail());
			users.put(userIdStr, user);
		}
		persistAndNotify((l) -> l.userCreated(user));
		return user;
	}

	@Override
	public User updateUser(User pUser) {
		final User.Id userId = pUser.getId();
		if (userId == null) {
			throw new NullPointerException("Missing user id");
		}
		synchronized(users) {
			if (users.put(userId.getId(), pUser) == null) {
				throw new IllegalArgumentException("Unknown user id: " + userId);
			}
		}
		persistAndNotify((l) -> l.userUpdated(pUser));
		return pUser;
	}

	@Override
	public void deleteUser(com.github.jochenw.crond.backend.model.beans.User.Id pUserId) {
		final User.Id userId = pUserId;
		if (userId == null) {
			throw new NullPointerException("Missing parameter: User id");
		}
		synchronized(users) {
			if (users.remove(userId.getId()) == null) {
				throw new IllegalArgumentException("Unknown job id: " + userId);
			}
		}
		persistAndNotify((l) -> l.userDeleted(userId));
	}

	protected void persistAndNotify(FailableConsumer<Listener,?> pConsumer) {
		persist();
		notifyListeners(pConsumer);
	}
}

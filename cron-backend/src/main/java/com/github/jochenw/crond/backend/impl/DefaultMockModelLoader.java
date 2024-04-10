package com.github.jochenw.crond.backend.impl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.xml.XMLConstants;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.afw.core.util.Sax;
import com.github.jochenw.afw.core.util.Sax.AbstractContentHandler;
import com.github.jochenw.crond.backend.model.beans.Execution;
import com.github.jochenw.crond.backend.model.beans.Job;
import com.github.jochenw.crond.backend.model.beans.User;

public class DefaultMockModelLoader implements BiConsumer<Path, MockModel> {
	public static class ModelLoadingHandler extends AbstractContentHandler {
		private final List<User> users;
		private final Set<String> userIds = new HashSet<>();
		private final List<Job> jobs;
		private final Set<String> jobIds = new HashSet<>();
		private final List<Execution> executions;
		private final Set<String> executionIds = new HashSet<>();
		private boolean inUsers, inJobs, inExecutions;

		public ModelLoadingHandler(MockModel pModel) {
			users = pModel.getUsers();
			jobs = pModel.getJobs();
			executions = pModel.getExecutions();
		}

		@Override
		public void startDocument() throws SAXException {
			users.clear();
			jobs.clear();
			executions.clear();
			inUsers = inJobs = inExecutions = false;
		}

		@Override
		public void endDocument() throws SAXException {
		}

		protected boolean isElement(String pName, String pUri, String pLocalName) {
			if (!DefaultMockModelPersistor.NS_MODEL_PERSISTED.equals(pUri)) {
				return false;
			}
			if (!pName.equals(pLocalName)) {
				return false;
			}
			return true;
		}

		protected String getAttribute(Attributes pAttrs, String pName) throws SAXException {
			return pAttrs.getValue(XMLConstants.NULL_NS_URI, pName);
		}
	
		protected String requireAttribute(Attributes pAttrs, String pName) throws SAXException {
			final String value = getAttribute(pAttrs, pName);
			if (value == null  ||  value.length() == 0) {
				throw error("Missing, or empty, attribute: " + pName);
			}
			return value;
		}

		protected ZonedDateTime getDateTimeAttribute(Attributes pAttrs, String pName) throws SAXException {
			final String strValue = getAttribute(pAttrs, pName);
			if (strValue == null  ||  strValue.length() == 0) {
				return null;
			}
			try {
				return ZonedDateTime.parse(strValue, DateTimeFormatter.ISO_DATE_TIME);
			} catch (DateTimeParseException e) {
				throw error("Invalid value for date/time attribute "
						+ pName + ": " + strValue);
			}
		}
	
		protected ZonedDateTime requireDateTimeAttribute(Attributes pAttrs, String pName)
		        throws SAXException {
			final ZonedDateTime value = getDateTimeAttribute(pAttrs, pName);
			if (value == null) {
				throw error("Missing, or empty, attribute: " + pName);
			}
			return value;
		}

		protected void assertElement(int pLevel, String pName, String pUri, String pLocalName)
		        throws SAXException {
			if (!isElement(pName, pUri, pLocalName)) {
				throw error("Expected " + pName + " element at level "
						+ pLevel + ", got " + asQName(pUri, pLocalName));
			}
		}

		@Override
		public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
			final int level = super.incLevel();
			switch(level) {
			case 1:
				assertElement(level, "model", pUri, pLocalName);
				break;
			case 2:
				if (isElement("users", pUri, pLocalName)) {
					inUsers = true;
				} else if (isElement("jobs", pUri, pLocalName)) {
					inJobs = true;
				} else if (isElement("executions", pUri, pLocalName)) {
					inExecutions = true;
				} else {
					throw error("Expected users|jobs|executions element at level "
							+ level + ", got " + asQName(pUri, pLocalName));
				}
				break;
			case 3:
				if (inUsers) {
					assertElement(level, "user", pUri, pLocalName);
					final String idStr = requireAttribute(pAttrs, "id");
					if (userIds.contains(idStr)) {
						throw error("Duplicate user id: " + idStr);
					}
					final String name = requireAttribute(pAttrs, "name");
					startTextElement(level, (email) -> {
						final User.Id id = new User.Id(idStr);
						final User user = new User(id, name, email);
						users.add(user);
						userIds.add(idStr);
					});
				} else if (inJobs) {
					assertElement(level, "job", pUri, pLocalName);
					final String idStr = requireAttribute(pAttrs, "id");
					if (jobIds.contains(idStr)) {
						throw error("Duplicate job id: " + idStr);
					}
					final String ownerIdStr = requireAttribute(pAttrs, "ownerId");
					final String name = requireAttribute(pAttrs, "name");
					if (!userIds.contains(ownerIdStr)) {
						throw error("Unkown owner id " + ownerIdStr
								+ " for job id " + idStr);
					}
					final User.Id ownerId = new User.Id(ownerIdStr);
					final Job.Id jobId = new Job.Id(idStr);
					final Job job = new Job(jobId, name, ownerId);
					jobs.add(job);
					jobIds.add(idStr);
				} else if (inExecutions) {
					final String idStr = requireAttribute(pAttrs, "id");
					if (executionIds.contains(idStr)) {
						throw error("Duplicate execution id: " + idStr);
					}
					final String jobIdStr = requireAttribute(pAttrs, "jobId");
					if (!jobIds.contains(jobIdStr)) {
						throw error("Unknwn job id " + jobIdStr
								+ " for execution id " + idStr);
					}
					final ZonedDateTime startTime = requireDateTimeAttribute(pAttrs, "startTime");
					final ZonedDateTime endTime = getDateTimeAttribute(pAttrs, "endTime");
					final Execution execution = new Execution(new Execution.Id(idStr), new Job.Id(jobIdStr), startTime, endTime);
					executions.add(execution);
					executionIds.add(idStr);
				} else {
					throw new IllegalStateException("Expected to be inside users|jobs|executions.");
				}
				break;
			default:
				throw error("Unexpected element " + asQName(pUri, pLocalName)
					+ " at level " + level);
			}
		}

		@Override
		public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
			final int level = getLevel();
			decLevel();
			switch(level) {
			case 1,3,4:
				// Ignore this, it is as expected, and there is nothing to do.
				break;
			case 2:
				inUsers = inJobs = inExecutions = false;
				break;
			default:
				throw error("Unexpected element " + asQName(pUri, pLocalName)
					+ " at level " + level);
			}
		}
	}

	@Override
	public void accept(Path pPath, MockModel pModel) {
		final Consumer<MockModel> consumer =
				(model) -> Sax.parse(pPath, new ModelLoadingHandler(model));
		if (Files.isRegularFile(pPath)) {
			pModel.runLocked(consumer);
		}
	}

}

package com.github.jochenw.crond.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.github.jochenw.afw.core.util.Sax.AbstractContentHandler;
import com.github.jochenw.afw.di.util.Exceptions;
import com.github.jochenw.crond.core.api.IModel.Job;
import com.github.jochenw.crond.core.api.IModel.User;
import com.github.jochenw.crond.core.beans.JobImpl;
import com.github.jochenw.crond.core.beans.UserImpl;

public class XmlModelFileReader extends AbstractContentHandler {
	private final Map<Long,User> users = new HashMap<>();
	private final Map<Long,Job> jobs = new HashMap<>();
	private final Set<String> emailAddresses = new HashSet<>();
	private final Set<String> userIdsAndEmails = new HashSet<>();
	private boolean inUsers, inJobs;

	@Override
	public void startDocument() throws SAXException {
		users.clear();
		inUsers = false;
	}

	@Override
	public void endDocument() throws SAXException {
		if (inUsers) {
			throw error("Unterminated element model/users");
		}
	}

	@Override
	public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
		final int level = incLevel();
		if (!XmlModelFileWriter.NS.equals(pUri)) {
			throw error("Expected namespace " + XmlModelFileWriter.NS + ", got " + asQName(pUri, pLocalName));
		}
		switch(level) {
		case 1:
			if (!"model".equals(pLocalName)) {
				throw error("Expected root element model, got " + asQName(pUri, pLocalName));
			}
			break;
		case 2:
			if ("users".equals(pLocalName)) {
				inUsers = true;
			} else if ("jobs".equals(pLocalName)) {
				inJobs = true;
			} else {
				throw error("Expected model/users element at level 2, got " + asQName(pUri, pLocalName));
			}
			break;
		case 3:
			if (inUsers) {
				if ("user".equals(pLocalName)) {
					final Long userId = requireLongAttribute(pAttrs, "id");
					final String email = requireAttribute(pAttrs, "email");
					final String name = requireAttribute(pAttrs, "name");
					final User user = UserImpl.of(userId, email, name);
					final User duplicatedUser = users.put(userId, user);
					if (duplicatedUser != null) {
						throw error("Duplicate user id: " + userId);
					}
					if (!emailAddresses.add(email)) {
						throw error("Duplicate email address: " + email);
					}
				} else {
					throw error("Expected model/users/user element at level 3, got " + asQName(pUri, pLocalName));
				}
			} else if (inJobs) {
				if ("job".equals(pLocalName)) {
					final Long jobId = requireLongAttribute(pAttrs, "id");
					final Long userId = requireLongAttribute(pAttrs, "userId");
					final String name = requireAttribute(pAttrs, "name");
					final Job job = JobImpl.of(jobId, userId, name);
					final Job duplicatedJob = jobs.put(jobId, job);
					if (duplicatedJob != null) {
						throw error("Duplicate job id: " + jobId);
					}
					final String userIdAndName = userId + ":" + name;
					if (!userIdsAndEmails.add(userIdAndName)) {
						throw error("Duplicate combination of user id, and name: " + userId + ", " + name);
					}
				} else {
					throw error("Expected model/jobs/job element at level 3, got " + asQName(pUri, pLocalName));
				}
			} else {
				throw error("Unexpected element at level 3: Expected model/users/user, got " + asQName(pUri, pLocalName));
			}
			break;
		}
	}

	protected Long requireLongAttribute(Attributes pAttrs, String pAttrName) throws SAXException {
		final String valueStr = requireAttribute(pAttrs, pAttrName);
		try {
			return Long.valueOf(valueStr);
		} catch (NumberFormatException nfe) {
			throw error("Invalid value for attribute " + pAttrName + ": Expected long integer, got " + valueStr);
		}
	}

	protected String requireAttribute(Attributes pAttrs, String pAttrName) throws SAXException {
		final String value = pAttrs.getValue(XMLConstants.NULL_NS_URI, pAttrName);
		if (value == null) {
			throw error("Missing attribute: " + pAttrName);
		}
		if (value.length() == 0) {
			throw error("Empty attribute: " + pAttrName);
		}
		return value;
	}

	@Override
	public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
		final int level = getLevel();
		decLevel();
		if (!XmlModelFileWriter.NS.equals(pUri)) {
			throw error("Expected namespace " + XmlModelFileWriter.NS + ", got " + asQName(pUri, pLocalName));
		}
		switch(level) {
		case 1:
			if (!"model".equals(pLocalName)) {
				throw error("Expected root element model, got " + asQName(pUri, pLocalName));
			}
			break;
		case 2:
			if ("users".equals(pLocalName)) {
				inUsers = false;
			} else if ("jobs".equals(pLocalName)) {
				inJobs = false;
			} else {
				throw error("Expected model/users element at level 2, got " + asQName(pUri, pLocalName));
			}
			break;
		case 3:
			if (inUsers) {
				// Nothing to do, the User has already been created in startElement().
			} else if (inJobs) {
				// Nothing to do, the Job has already been created in startElement().
			} else {
				throw error("Unexpected element at level 3: Expected model/users/user, got " + asQName(pUri, pLocalName));
			}
			break;
		}
	}

	public Map<Long,User> getUsers() {
		return users;
	}

	public Map<Long,Job> getJobs() {
		return jobs;
	}
	
	public static XmlModelFileReader read(InputSource pSource) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(false);
			final XmlModelFileReader xmfr = new XmlModelFileReader();
			final XMLReader xr = spf.newSAXParser().getXMLReader();
			xr.setContentHandler(xmfr);
			xr.parse(pSource);
			return xmfr;
		} catch (Exception e) {
			throw Exceptions.show(e);
		}
	}
}

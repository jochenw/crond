package com.github.jochenw.crond.core.impl;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.github.jochenw.afw.di.util.Exceptions;
import com.github.jochenw.crond.core.api.IModel.Job;
import com.github.jochenw.crond.core.api.IModel.User;

public class XmlModelFileWriter {
	public static String NS = "http://namespaces.github.com/jochenw/crond/xmlModelFile/1.0.0";

	public void write(Path pModelFile, Collection<User> pUsers, Collection<Job> pJobs, boolean pPrettyPrint) {
		try (OutputStream os = Files.newOutputStream(pModelFile);
			 BufferedOutputStream bos = new BufferedOutputStream(os)) {
			write(pUsers, pJobs, pPrettyPrint, bos, pModelFile.toString());
		} catch (Exception e) {
			throw Exceptions.show(e);
		}
	}

	public void write(Collection<User> pUsers, Collection<Job> pJobs, boolean pPrettyPrint, OutputStream bos, String pUri)
			throws TransformerFactoryConfigurationError, TransformerConfigurationException, SAXException {
		final SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
		final TransformerHandler th = stf.newTransformerHandler();
		final StreamResult sr = new StreamResult(bos);
		sr.setSystemId(pUri);
		th.setResult(sr);
		final Transformer t = th.getTransformer();
		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		t.setOutputProperty(OutputKeys.STANDALONE, "yes");
		t.setOutputProperty(OutputKeys.INDENT, pPrettyPrint ? "yes" : "no");
		th.startDocument();
		final Attributes NO_ATTRS = new AttributesImpl();
		th.startPrefixMapping(XMLConstants.DEFAULT_NS_PREFIX, NS);
		th.startElement(NS, "model", "model", NO_ATTRS);
		th.startElement(NS, "users", "users", NO_ATTRS);
		if (pUsers != null) {
			for (User u : pUsers) {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "id", "id", "CDATA", u.getId().toString());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "email", "email", "CDATA", u.getEmail());
				final String name = u.getName();
				if (name != null) {
					attrs.addAttribute(XMLConstants.NULL_NS_URI, "name", "name", "CDATA", name);
				}
				th.startElement(NS, "user", "user", attrs);
				th.endElement(NS, "user", "user");
			}
		}
		th.endElement(NS, "users", "users");
		th.startElement(NS, "jobs", "jobs", NO_ATTRS);
		if (pJobs != null) {
			for (Job j : pJobs) {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "id", "id", "CDATA", j.getId().toString());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "userId", "userId", "CDATA", j.getUserId().toString());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "name", "name", "CDATA", j.getName());
				th.startElement(NS, "job", "job", attrs);
				th.endElement(NS, "job", "job");
			}
		}
		th.endElement(NS, "jobs", "jobs");
		th.endElement(NS, "model", "model");
		th.endPrefixMapping(XMLConstants.DEFAULT_NS_PREFIX);
		th.endDocument();
	}
}

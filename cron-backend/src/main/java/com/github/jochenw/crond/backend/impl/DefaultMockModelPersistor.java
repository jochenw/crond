package com.github.jochenw.crond.backend.impl;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import com.github.jochenw.afw.core.function.Functions;
import com.github.jochenw.afw.core.function.Functions.FailableBiConsumer;
import com.github.jochenw.afw.core.function.Functions.FailableConsumer;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.di.util.Exceptions;

public class DefaultMockModelPersistor implements BiConsumer<Path,MockModel> {
	@Override
	public void accept(Path pFile, MockModel pModel) {
		try (OutputStream out = Files.newOutputStream(pFile);
			 BufferedOutputStream bOut = new BufferedOutputStream(out)) {
			write(bOut, pModel);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static final String NS_MODEL_PERSISTED = "http://namespaces.github.com/jochenw/crond/ModelPersisted/1.0.0";
	private static final Attributes NO_ATTRS = new AttributesImpl();

	private void write(OutputStream pOut, MockModel pModel) {
		try {
			final TransformerHandler th = ((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler();
			final Transformer t = th.getTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.setOutputProperty(OutputKeys.INDENT, "no");
			th.setResult(new StreamResult(pOut));
			final FailableBiConsumer<String,Attributes,Exception> elementStart = (tag,att) -> {
				final Attributes attrs = Objects.notNull(att, NO_ATTRS);
				th.startElement(NS_MODEL_PERSISTED, tag, "m:" + tag, attrs);
			};
			final FailableConsumer<String,Exception> elementEnd = (tag) -> {
				th.endElement(NS_MODEL_PERSISTED, tag, "m:" + tag);
			};
			final FailableConsumer<String,Exception> elementText = (text) -> {
				final char[] chars = text.toCharArray();
				th.characters(chars, 0, chars.length);
			};
			th.startDocument();
			th.startPrefixMapping("m", NS_MODEL_PERSISTED);
			elementStart.accept("model", NO_ATTRS);
			elementStart.accept("users", NO_ATTRS);
			pModel.getUsers((u) -> {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "id", "id", "CDATA", u.getId().getId());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "name", "name", "CDATA", u.getName());
				try {
					elementStart.accept("user", attrs);
					elementStart.accept("email", NO_ATTRS);
					elementText.accept(u.getEmail());
					elementEnd.accept("email");
					elementEnd.accept("user");
				} catch (Exception e) {
					throw Exceptions.show(e);
				}
			});
			elementEnd.accept("users");
			elementStart.accept("jobs", NO_ATTRS);
			elementEnd.accept("jobs");
			pModel.getJobs((j) -> {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "id", "id", "CDATA", j.getId().getId());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "ownerId", "ownerId", "CDATA", j.getOwnerId().getId());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "name", "name", "CDATA", j.getName());
				Functions.accept(elementStart, "job", attrs);
				Functions.accept(elementEnd, "job");
			});
			elementStart.accept("executions", NO_ATTRS);
			pModel.getExecutions((e) -> {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "id", "id", "CDATA", e.getId().getId());
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "jobId", "jobId", "CDATA", e.getJobId().getId());
				if (e.getStartTime() != null) {
					attrs.addAttribute(XMLConstants.NULL_NS_URI, "startTime", "startTime", "CDATA", DateTimeFormatter.ISO_DATE_TIME.format(e.getStartTime()));
				}
				if (e.getEndTime() != null) {
					attrs.addAttribute(XMLConstants.NULL_NS_URI, "startTime", "startTime", "CDATA", DateTimeFormatter.ISO_DATE_TIME.format(e.getEndTime()));
				}
				Functions.accept(elementStart, "execution", attrs);
				Functions.accept(elementEnd, "execution");
			});
			elementEnd.accept("executions");
			elementEnd.accept("model");
			th.endDocument();
		} catch (Exception e) {
			throw new UndeclaredThrowableException(e);
		}
	}
}

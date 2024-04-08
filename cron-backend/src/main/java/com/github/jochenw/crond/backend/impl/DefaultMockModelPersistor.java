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
import com.github.jochenw.afw.core.util.Sax;
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

	private void write(OutputStream pOut, MockModel pModel) {
		Sax.creator().withNamespaceUri(NS_MODEL_PERSISTED).write(pOut, (sw) -> {
			sw.writeElement("model", (sw2) -> {
				sw2.writeElement("users", (sw3) -> {
					pModel.getUsers((u) -> {
						sw3.writeElement("user", (sw4) -> {
							sw4.writeElement("email", u.getEmail());
						},
						"id", u.getId().getId(), "name", u.getName());
					});
				});
				sw2.writeElement("jobs", (sw3) -> {
					pModel.getJobs((j) -> {
						sw3.writeElement("job", (sw4) -> {},
								         "id", j.getId().getId(), "ownerId", j.getOwnerId().getId(), "name", j.getName());
					});
				});
				sw2.writeElement("executions", (sw3) -> {
					pModel.getExecutions((e) -> {
						sw3.writeElement("execution", (sw4) -> {},
								         "id", e.getId().getId(), "jobId", e.getJobId().getId(),
								         "startTime", e.getStartTime(), "endTime", e.getEndTime());
					});
				});
			});
		});
	}
}

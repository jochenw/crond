package com.github.jochenw.crond.core.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.jochenw.afw.core.inject.AfwCoreOnTheFlyBinder;
import com.github.jochenw.afw.core.log.ILog.Level;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.log.simple.SimpleLogFactory;
import com.github.jochenw.afw.core.util.tests.Tests;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.crond.core.api.IModel.Job;
import com.github.jochenw.crond.core.api.IModel.User;
import com.github.jochenw.crond.core.beans.UserImpl;

class XmlModelFileIoTest {
	@Test
	void testWriteRead() throws Exception {
		final Path testDir = Tests.requireTestDirectory(XmlModelFileIoTest.class);
		final Path modelFile = Files.createTempFile(testDir, "modelFile", ".xml");
		System.out.println("Model file = " + modelFile);
		final ILogFactory lf = SimpleLogFactory.ofSystemOut(Level.TRACE);
		Files.deleteIfExists(modelFile);
		final IComponentFactory cf = IComponentFactory.builder().jakarta()
				.module((b) -> {
					b.bind(Path.class, "xml.model.file").toInstance(modelFile);
					b.bind(ILogFactory.class).toInstance(lf);
					b.bind(XmlFileModel.class);
				}).onTheFlyBinder(new AfwCoreOnTheFlyBinder()).build();
		runTestWriteRead(cf, "WriteRead1", true);
		runTestWriteRead(cf, "WriteRead2", false);
		assertTrue(Files.isRegularFile(modelFile));
	}

	private void runTestWriteRead(IComponentFactory pComponentFactory, String pUri, boolean pPrettyPrint)
			throws TransformerFactoryConfigurationError, TransformerConfigurationException, SAXException, IOException {
		final XmlFileModel xfm = pComponentFactory.requireInstance(XmlFileModel.class);
		final User user1 = xfm.addUser("jochen.wiedmann@gmail.com", "Wiedmann, Jochen");
		final User user2 = xfm.addUser("tobias.huehner@softwareag.com", "H\u00fchner, Tobias");
		final List<User> users = Arrays.asList(user1, user2);
		final List<Job> jobs = Arrays.asList();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new XmlModelFileWriter().write(users, jobs, pPrettyPrint, baos, pUri);
		final InputSource isource = new InputSource(new ByteArrayInputStream(baos.toByteArray()));
		isource.setSystemId(pUri);
		final XmlModelFileReader xmfr = XmlModelFileReader.read(isource);
		assertSameUsers(users, xmfr.getUsers());
	}

	protected void assertSameUsers(List<User> pExpectedUsers, Map<Long,User> pActualUsers) {
		assertEquals(pExpectedUsers.size(), pActualUsers.size());
		for (User expectedUser : pExpectedUsers) {
			final User actualUser = pActualUsers.get(expectedUser.getId());
			assertNotNull(actualUser);
			assertEquals(expectedUser.getEmail(), actualUser.getEmail());
			assertEquals(expectedUser.getName(), actualUser.getName());
		}
	}
}

package com.github.jochenw.crond.backend.impl;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.junit.Test;
import org.xml.sax.InputSource;

import com.github.jochenw.afw.core.util.Sax;
import com.github.jochenw.crond.backend.impl.DefaultMockModelLoader.ModelLoadingHandler;

public class DefaultMockModelLoaderTest {
	private static final String XML =
			"<model xmlns='" + DefaultMockModelPersistor.NS_MODEL_PERSISTED + "'>\n"
			+ " <users>\n"
			+ "   <user id='0' name='Jochen Wiedmann'>\n"
			+ "     <email>jochen@apache.org</email>\n"
			+ "   </user>\n"
			+ "   <user id='1' name='Michael Widenius'>\n"
			+ "     <email>monty@mysql.org</email>\n"
			+ "   </user>\n"
			+ "   <user id='2' name='Jim Jagielski'>\n"
			+ "     <email>jim@apache.org</email>\n"
			+ "   </user>\n"
			+ " </users>\n"
			+ " <jobs>\n"
			+ "   <job id='0' name='Daily Backup' ownerId='0'></job>\n"
			+ "   <job id='1' name='Accounting' ownerId='2'></job>\n"
			+ " </jobs>\n"
			+ " <executions>\n"
			+ "   <execution id='0' jobId='0'"
			+     " startTime='2024-04-10T22:45:40.177427941+02:00[Europe/Berlin]' endTime='2024-04-10T22:52:37.177326942+02:00[Europe/Berlin]'/>\n"
			+ "   <execution id='1' jobId='0'"
			+     " startTime='2024-04-11T22:45:40.000000003+02:00[Europe/Berlin]' endTime='2024-04-11T22:52:31.008641293+02:00[Europe/Berlin]'/>\n"
			+ "   <execution id='2' jobId='0'"
			+     " startTime='2024-04-10T22:45:40.177427941+02:00[Europe/Berlin]'/>\n"
			+ " </executions>\n"
			+ "</model>\n";
	@Test
	public void testHandler() {
		final InputSource isource = new InputSource(new StringReader(XML));
		final MockModel model = new MockModel(null, null, null);
		Sax.parse(isource, new ModelLoadingHandler(model));
		assertEquals(3, model.getUsers().size());
		assertEquals(2, model.getJobs().size());
		assertEquals(3, model.getExecutions().size());
	}

}

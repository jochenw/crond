package com.github.jochenw.crond.ui.vaadin.srvlt;

import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.github.jochenw.afw.core.DefaultResourceLoader;
import com.github.jochenw.afw.core.ResourceLocator;
import com.github.jochenw.afw.core.inject.AfwCoreOnTheFlyBinder;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.log.log4j.Log4j2LogFactory;
import com.github.jochenw.afw.core.log.simple.SimpleLogFactory;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.afw.di.api.Application;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.afw.di.api.ILifecycleController;
import com.github.jochenw.afw.di.impl.DefaultLifecycleController;


@WebListener
public class CronSrvInitializer implements ServletContextListener {
	public static class Data {
		private final Application application;
		public Data(Application pApplication) {
			application = pApplication;
		}
		public Application getApplication() { return application; }
	}

	private ILog log;

	private static final String KEY = CronSrvInitializer.class.getName() + ".KEY";

	@Override
	public void contextInitialized(ServletContextEvent pSce) {
		final ServletContext sc = pSce.getServletContext();
		final String instanceName = getInstanceName(sc.getContextPath());
		final Data data = newData(instanceName);
		log.exiting("contextInitialized");
		sc.setAttribute(KEY, data);
	}

	@Override
	public void contextDestroyed(ServletContextEvent pSce) {
		log.entering("contextDestroyed");
		final ServletContext sc = pSce.getServletContext();
		final Data data = getData(sc);
		sc.setAttribute(KEY, data);
		log.exiting("contextDestroyed");
	}

	/**
	 * Returns the data object.
	 * @param <O> The data objects type.
	 * @param pContext The servlet context, that has been initialized, and contains the data
	 *   object.
	 * @return The created d
	 */
	public static Data getData(ServletContext pContext) {
		final Data data = (Data) pContext.getAttribute(KEY);
		if (data == null) {
			throw new IllegalStateException("Data not available in context");
		}
		return data;
	}

	/** Converts the current web applications context path into the
	 * application instance name.
	 * @param pContextName The web applications context path.
	 * @return The applications instance name, as derived from the context path.
	 */
	protected String getInstanceName(String pContextName) {
		if (Strings.isEmpty(pContextName)) {
			return "ROOT";
		} else if (pContextName.startsWith("/")) {
			return pContextName.substring(1);
		} else {
			return pContextName;
		}
	}

	protected Data newData(String pInstanceName) {
		final ResourceLocator resourceLocator = new DefaultResourceLoader("cronsrv", pInstanceName);
		final ILogFactory logFactory = getLogFactory(resourceLocator);
		// From now on, the variable log may be used.
		final Properties[] properties = getProperties(resourceLocator);
		final ILifecycleController lc = new DefaultLifecycleController();
		final com.github.jochenw.afw.di.api.Module module = (b) -> {
			b.bind(ILifecycleController.class).toInstance(lc);
			b.bind(ILogFactory.class).toInstance(logFactory);
			b.bind(Properties.class, "factory").toInstance(properties[0]);
			b.bind(Properties.class).toInstance(properties[1]);
			properties[1].forEach((k,v) -> {
				b.bind(String.class, (String) k).toInstance((String) v);
			});
		};
		final Application application = Application.of(() ->
		    IComponentFactory.builder().module(module).onTheFlyBinder(new AfwCoreOnTheFlyBinder()).build());
		return new Data(application);
	}

	private ILogFactory getLogFactory(ResourceLocator pResourceLocator) {
		final String uri = "log4j2.xml";
		final URL url = pResourceLocator.getResource(uri);
		final ILogFactory logFactory;
		if (url == null) {
			logFactory = SimpleLogFactory.ofSystemOut();
		} else {
			logFactory = Log4j2LogFactory.of(url);
		}
		log = logFactory.getLog(CronSrvInitializer.class);
		if (url == null) {
			log.warnf("getLogFactory", "Log4j configuration file not found: %s", uri);
			log.warn("getLogFactory", "Logging to System.out as a fallback.");
		} else {
			log.infof("getLogFactory", "Logging initialized from %s", url);
		}
		return logFactory;
	}

	private Properties[] getProperties(ResourceLocator pResourceLocator) {
		final String factoryUri = pResourceLocator.getApplicationName() + "-factory.properties";
		final URL factoryPropertiesUrl = pResourceLocator.requireResource(factoryUri);
		log.infof("getProperties", "Factory properties loaded from: " + factoryPropertiesUrl);
		final Properties factoryProperties = Streams.load(factoryPropertiesUrl);
		final String instanceUri = pResourceLocator.getApplicationName() + ".properties";
		final URL instancePropertiesUrl = pResourceLocator.getResource(instanceUri);
		if (instancePropertiesUrl == null) {
			log.warnf("getProperties", "Instance properties not found: %s", instanceUri);
			return new Properties[] { factoryProperties, factoryProperties };
		} else {
			final Properties instanceProperties = Streams.load(instancePropertiesUrl);
			log.infof("getProperties", "Instance properties loaded from: %s", instancePropertiesUrl);
			final Properties actualProperties = new Properties(factoryProperties);
			actualProperties.putAll(instanceProperties);
			return new Properties[] { factoryProperties, actualProperties };
		}
	}

	/** Returns the application object.
	 */
	public static Application getApplication(ServletContext pCtx) {
		return getData(pCtx).getApplication();
	}

	/** Returns the component factory.
	 */
	public static IComponentFactory getComponentFactory(ServletContext pCtx) {
		return getData(pCtx).getApplication().getComponentFactory();
	}
}

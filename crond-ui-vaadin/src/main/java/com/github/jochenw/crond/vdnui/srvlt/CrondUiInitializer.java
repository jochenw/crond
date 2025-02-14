package com.github.jochenw.crond.vdnui.srvlt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.jochenw.afw.core.data.Data;
import com.github.jochenw.afw.core.inject.AfwCoreOnTheFlyBinder;
import com.github.jochenw.afw.core.log.ILog;
import com.github.jochenw.afw.core.log.ILog.Level;
import com.github.jochenw.afw.core.log.ILogFactory;
import com.github.jochenw.afw.core.log.log4j.Log4j2LogFactory;
import com.github.jochenw.afw.core.log.simple.SimpleLogFactory;
import com.github.jochenw.afw.core.props.DefaultPropertyFactory;
import com.github.jochenw.afw.core.props.IPropertyFactory;
import com.github.jochenw.afw.core.util.Holder;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.afw.di.api.Application;
import com.github.jochenw.afw.di.api.IComponentFactory;
import com.github.jochenw.afw.di.api.ILifecycleController;
import com.github.jochenw.afw.di.api.Module;
import com.github.jochenw.crond.core.api.IModel;
import com.github.jochenw.crond.core.impl.XmlFileModel;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class CrondUiInitializer implements ServletContextListener {
	private static final String KEY = CrondUiInitializer.class.getName() + ".KEY";
	private ILog log;

	@Override
	public void contextInitialized(ServletContextEvent pSce) {
		ServletContextListener.super.contextInitialized(pSce);
		final ServletContext ctx = pSce.getServletContext();
		final Holder<String> log4jXmlUrlHolder = new Holder<>();
		final Holder<String> instancePropertiesUrlHolder = new Holder<>();
		final IComponentFactory cf = newComponentFactory(ctx.getContextPath(),
				                                         log4jXmlUrlHolder::set,
				                                         instancePropertiesUrlHolder::set);
		ctx.setAttribute(KEY, cf);
		log = cf.requireInstance(ILogFactory.class).getLog(CrondUiInitializer.class);
		final String mName = "contextInitialized";
		log.info(mName, "Starting at {}", DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()));
		if (log4jXmlUrlHolder.get() == null) {
			log.info(mName, "Logging initialized with default settings.");
		} else {
			log.info(mName, "Logging initialized from {}", log4jXmlUrlHolder.get());
		}
		log.info(mName, "Instance properties loaded from {}", instancePropertiesUrlHolder.get());
	}

	@Override
	public void contextDestroyed(ServletContextEvent pSce) {
		log.entering("contextDestroyed");
		ServletContextListener.super.contextDestroyed(pSce);
		final IComponentFactory cf = getContextFactory(pSce.getServletContext());
		final ILifecycleController lc = cf.requireInstance(ILifecycleController.class);
		lc.shutdown();
		log.exiting("contextDestroyed");
	}

	public static IComponentFactory getContextFactory(ServletContext pCtx) {
		final IComponentFactory cf = (IComponentFactory) pCtx.getAttribute(KEY);
		if (cf == null) {
			throw new IllegalStateException("IContextFactory not found.");
		}
		return cf;
	}

	protected IComponentFactory newComponentFactory(String pPath, Consumer<String> pLog4jUriConsumer,
			                                        Consumer<String> pCrondUiPropertiesUriConsumer) {
		URL log4jXmlUrl;
		final boolean haveApplicationPath = pPath != null  &&  pPath.length() > 0  &&  !"ROOT".equals(pPath);
		final Path baseDir;
		final String crondUiBaseDirProperty = System.getProperty("crondui.base.dir");
		if (crondUiBaseDirProperty == null) {
			baseDir = null;
		} else {
			final Path dir = Paths.get(crondUiBaseDirProperty);
			if (Files.isDirectory(dir)) {
				baseDir = dir;
			} else {
				baseDir = null;
			}
		}
		final Function<String,URL> finder = (uri) -> {
			if (baseDir == null) {
				return Thread.currentThread().getContextClassLoader().getResource(uri);
			} else {
				final Path file = baseDir.resolve(uri);
				if (Files.isRegularFile(file)) {
					try {
						return file.toUri().toURL();
					} catch (IOException ioe) {
						throw new UncheckedIOException(ioe);
					}
				} else {
					System.out.println("Warning: File not found: " + file.toAbsolutePath());
					return null;
				}
			}
		};
		if (haveApplicationPath) {
			final String ctxUri = pPath + "/log4j2,xml";
			log4jXmlUrl = finder.apply(ctxUri);
		} else {
			log4jXmlUrl = null;
		}
		if (log4jXmlUrl == null) {
			final String uri = "log4j2.xml";
			log4jXmlUrl = finder.apply(uri);
		}
		final ILogFactory lf;
		if (log4jXmlUrl == null) {
			pLog4jUriConsumer.accept(null);
			lf = SimpleLogFactory.ofSystemOut(Level.TRACE);
		} else {
			lf = Log4j2LogFactory.of(log4jXmlUrl);
		}
		pLog4jUriConsumer.accept(log4jXmlUrl == null ? null : log4jXmlUrl.toExternalForm());
		final String factoryPropertiesUri = "crondui-factory.properties";
		final URL factoryPropertiesUrl = Thread.currentThread().getContextClassLoader().getResource(factoryPropertiesUri);
		if (factoryPropertiesUrl == null) {
			throw new IllegalStateException("Factory properties not found: " + factoryPropertiesUri);
		}
		final Properties factoryProperties = Streams.load(factoryPropertiesUrl);
		URL instancePropertiesUrl;
		if (haveApplicationPath) {
			final String ctxUri = pPath + "/crondui.properties";
			instancePropertiesUrl = finder.apply(ctxUri);
		} else {
			instancePropertiesUrl = null;
		}
		if (instancePropertiesUrl == null) {
			final String uri = "crondui.properties";
			instancePropertiesUrl = finder.apply(uri);
			if (instancePropertiesUrl == null) {
				throw new IllegalStateException("Instance properties not found: " + uri);
			}
		}
		final Properties instanceProperties = Streams.load(instancePropertiesUrl);
		pCrondUiPropertiesUriConsumer.accept(instancePropertiesUrl.toExternalForm());
		final Properties properties = new Properties();
		properties.putAll(factoryProperties);
		properties.putAll(instanceProperties);
		final Path modelFilePath = Data.requirePath(properties, "crondui.model.file");
		final Module module = (b) -> {
			b.bind(ILogFactory.class).toInstance(lf);
			b.bind(Properties.class).toInstance(properties);
			b.bind(Properties.class, "factory").toInstance(factoryProperties);
			b.bind(Properties.class, "instance").toInstance(instanceProperties);
			b.bind(IPropertyFactory.class).toInstance(new DefaultPropertyFactory(properties));
			b.bind(IModel.class).to(XmlFileModel.class);
			b.bind(Path.class, "xml.model.file").toInstance(modelFilePath);
		};
		final Application application = Application.of(module, "jakarta", new AfwCoreOnTheFlyBinder());
		return application.getComponentFactory();
	}
}

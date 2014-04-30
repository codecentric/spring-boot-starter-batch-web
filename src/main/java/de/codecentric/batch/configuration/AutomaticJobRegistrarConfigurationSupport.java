package de.codecentric.batch.configuration;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extend this class to add custom {@link ApplicationContextFactory}.
 * 
 * @author Thomas Bosch
 */
public abstract class AutomaticJobRegistrarConfigurationSupport {

	@Autowired
	private AutomaticJobRegistrar automaticJobRegistrar;

	@PostConstruct
	public void initialize() throws Exception {
		addApplicationContextFactories(automaticJobRegistrar);
	}

	/**
	 * Add ApplicationContextFactories to the given job registrar.
	 * 
	 * @param automaticJobRegistrar
	 *            Bean
	 * @throws Exception
	 *             Some error.
	 */
	protected abstract void addApplicationContextFactories(AutomaticJobRegistrar automaticJobRegistrar)
			throws Exception;

}

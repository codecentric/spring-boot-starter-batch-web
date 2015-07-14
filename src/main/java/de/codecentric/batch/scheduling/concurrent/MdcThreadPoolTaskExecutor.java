package de.codecentric.batch.scheduling.concurrent;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A SLF4J MDC-compatible {@link ThreadPoolExecutor}.
 * <p/>
 * In general, MDC is used to store diagnostic information (e.g. logfile name) in per-thread variables, to facilitate logging. However, although MDC
 * data is passed to thread children, this doesn't work when threads are reused in a thread pool. This is a drop-in replacement for
 * {@link ThreadPoolTaskExecutor} sets MDC data before each task appropriately.
 * <p/>
 *
 * @author Dennis Schulte
 */
public class MdcThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

	private static final long serialVersionUID = 1L;
	private boolean useFixedContext = false;
	private Map<String, String> fixedContext;

	public MdcThreadPoolTaskExecutor() {
		super();
	}

	public MdcThreadPoolTaskExecutor(Map<String, String> fixedContext) {
		super();
		this.fixedContext = fixedContext;
		useFixedContext = (fixedContext != null);
	}

	private Map<String, String> getContextForTask() {
		return useFixedContext ? fixedContext : MDC.getCopyOfContextMap();
	}

	/**
	 * All executions will have MDC injected. {@code ThreadPoolExecutor}'s submission methods ({@code submit()} etc.)
	 * all delegate to this.
	 */
	@Override
	public void execute(Runnable command) {
		super.execute(wrap(command, getContextForTask()));
	}

	public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
		return new Runnable() {
			@Override
			public void run() {
				Map<String, String> previous = MDC.getCopyOfContextMap();
				if (context == null) {
					MDC.clear();
				} else {
					MDC.setContextMap(context);
				}
				try {
					runnable.run();
				} finally {
					if (previous == null) {
						MDC.clear();
					} else {
						MDC.setContextMap(previous);
					}
				}
			}
		};
	}
}
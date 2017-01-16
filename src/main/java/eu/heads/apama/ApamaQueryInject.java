package eu.heads.apama;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.annotation.Update;
import org.kevoree.api.Context;
import org.kevoree.log.Log;

import com.apama.engine.MonitorScript;
import com.apama.engine.beans.EngineClientFactory;
import com.apama.engine.beans.interfaces.EngineClientInterface;
import com.apama.util.CompoundException;

@ComponentType(version = 6, description = "Inject Apama EPL Code into running Correlator.")
public class ApamaQueryInject {

	private static final String EPL_CODE_DELETED = "Cleanup: all EPL code deleted.";

	private static final String EPL_CODE_WARNINGS = "EPL code injected, no warnings.";

	private static final String FILES_TO_INJECT = "EPL Files to inject:";

	private static final String WARNINGS_FROM_EPL_CODE = "Warnings from EPL code:";

	@Param(defaultValue = "false")
	private String clean;

	@KevoreeInject
	private Context context;

	private EngineClientInterface engineClient;

	@Param(defaultValue = "")
	private String files;

	@Param(defaultValue = "localhost")
	private String host;

	@Param(defaultValue = "15903")
	private int port;

	@Param(defaultValue = "my-injection-process")
	private String processName;

	@Param(defaultValue = "")
	private String query;

	public void setClean(String clean) {
		this.clean = clean;
	}

	public void setCorrelator(String string) {
		String[] split = string.split(":");
		this.host = split[0];
		this.port = Integer.parseInt(split[1]);
	}

	public void setFiles(String files) {
		this.files = files;
	}

	@Start
	public void start() {
		try {
			engineClient = EngineClientFactory.createEngineClient(host, port, processName);
			if ("true".equalsIgnoreCase(clean)) {
				engineClient.deleteAll();
				logInfo(EPL_CODE_DELETED);
			}
			if (files != null && !files.isEmpty()) {
				List<String> fileList = loadFiles(files);
				logInfo(FILES_TO_INJECT);
				for (String fileName : fileList) {
					logInfo(fileName);
				}
				String[] injectWarnings = engineClient.injectMonitorScriptFromFile(fileList);
				if (injectWarnings.length > 0) {
					logInfo(WARNINGS_FROM_EPL_CODE);
					for (int i = 0; i < injectWarnings.length; i++) {
						logInfo(injectWarnings[i]);
					}
				} else {
					logInfo(EPL_CODE_WARNINGS);
				}
			} else if (query != null & !query.isEmpty()) {
				// Inject the MonitorScript from query parameter
				MonitorScript epl = new MonitorScript(query);
				engineClient.injectMonitorScript(epl);
			} else {
				logInfo("Use parameter 'query' or 'files' to inject EPL code. Both are empty.");
			}
		} catch (CompoundException e) {
			logError("Error during EPL code injection.", e);
		}
	}

	@Stop
	public void stop() {
		// do NOT deleteAll().
		// This may delete scripts from other applications running in the same
		// correlator.
		engineClient.dispose();
	}

	@Update
	public void update() {
		this.stop();
		this.start();
	}

	private List<String> loadFiles(String files2) {
		List<String> asList = Arrays.asList(files2.split("[;\r\n]"));
		List<String> fileNames = new ArrayList<>();
		for (String fileName : asList) {
			if (!fileName.isEmpty()) {
				fileNames.add(fileName);
			}
		}
		return fileNames;
	}

	/**
	 * Log a message at ERROR level. If the Kevoree context is <code>null</code>,
	 * message is written with <code>System.out</code>.
	 * 
	 * @param message
	 *            the message to log.
	 */
	private void logError(String message, Throwable ex) {
		if (this.context == null) {
			System.out.println(message);
			ex.printStackTrace(System.out);
		} else {
			Log.error("{}: " + message + System.lineSeparator() + "{}", null, this.context.getPath(), ex.toString());
		}
	}

	/**
	 * Log a message at INFO level. If the Kevoree context is <code>null</code>,
	 * message is written with <code>System.out</code>.
	 * 
	 * @param message
	 *            the message to log.
	 */
	private void logInfo(String message) {
		if (this.context == null) {
			System.out.println(message);
		} else {
			Log.info("{}: " + message, this.context.getPath());
		}
	}
}

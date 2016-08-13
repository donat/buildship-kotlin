package org.eclipse.buildship.kotlin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jetbrains.kotlin.script.ScriptTemplateProvider;
import org.osgi.framework.Bundle;

public class GradleKotlinScriptTemplateProvider implements ScriptTemplateProvider {

	// TODO (donat) extension point can't be used if ScriptTemplateProvider is implemented in Java
	
	// TODO (donat) In order to load a TAPI model for a project, the the extension point should
	// supply the corresponding IProject instance
	
	private static String[] classpathEntries = new String[] {
				"/bin",
				"/lib/gradle-core-3.0-20160720184418+0000.jar",
				"/lib/gradle-script-kotlin-0.3.0.jar",
				"/lib/gradle-tooling-api-3.0-20160720184418+0000.jar", 
				"/lib/slf4j-api-1.7.10.jar"
	};

	@Override
	public Iterable<String> getDependenciesClasspath() {
		Bundle pluginBundle = Platform.getBundle(Activator.PLUGIN_ID);
		ArrayList<String> result = new ArrayList<String>();
		for (String path : classpathEntries) {
			try {
				result.add(FileLocator.toFileURL(pluginBundle.getEntry(path)).getFile());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return result;
	}

	@Override
	public Map<String, Object> getEnvironment() {
		HashMap<String, Object> environment = new HashMap<String, Object>();
		environment.put("rtPath", rtPath());
		return environment;
	}

	@Override
	public String getId() {
		return "Test";
	}

	@Override
	public String getTemplateClassName() {
		return "org.eclipse.buildship.kotlin.KotlinBuildScript";
	}

	@Override
	public int getVersion() {
		return 10;
	}

	@Override
	public boolean isValid() {
		return true;
	}
	
	private List<File> rtPath() {
		File rtJar = new File(JavaRuntime.getDefaultVMInstall().getInstallLocation(), "jre/lib/rt.jar");
		if (rtJar.exists()) {
			return Arrays.asList(rtJar);
		} 
		
		return Collections.emptyList();
	}
}
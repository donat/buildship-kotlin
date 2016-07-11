package org.eclipse.buildship.kotlin

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Platform
import org.jetbrains.kotlin.script.ScriptTemplateProvider
import java.io.File

class GradleKotlinScriptTemplateProvider : ScriptTemplateProvider {

	// TODO (donat) extension point can't be used if ScriptTemplateProvider is implemented in Java
	
	// TODO (donat) In order to load a TAPI model for a project, the the extension point should
	// supply the corresponding IProject instance
	
	companion object {
		val classpathEntries = listOf(
				"/bin",
				"/lib/gradle-core-3.0-20160720184418+0000.jar",
				"/lib/gradle-script-kotlin-0.3.0.jar",
				"/lib/gradle-tooling-api-3.0-20160720184418+0000.jar", 
				"/lib/slf4j-api-1.7.10.jar"
		)
	}
	
    override val dependenciesClasspath: Iterable<String>
        get() {
            val pluginBundle = Platform.getBundle(Activator.PLUGIN_ID)
    		return classpathEntries.map { FileLocator.toFileURL(pluginBundle.getEntry(it)).getFile() }
        }

    override val environment: Map<String, Any?>?
        get() {
			return mapOf("rootProject" to rootProjectLocation())
		} 
		
	private fun getModels(): List<File> {
		val model = KotlinModelQuery.retrieveKotlinBuildScriptModelFrom(rootProjectLocation()!!)
		return model.classPath
	}
	
	private fun rootProjectLocation(): File? {
		return rootProject()?.getLocation()?.toFile()
	} 
	
	private fun rootProject(): IProject? {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects().find {
			it.isAccessible() && it.getFile("build.gradle.kts").exists()
		}
	}
	
	private fun externalLibs(): List<File> {
		// TODO (donat) collect external libs. Will we ever have one?
		return emptyList()
	}
	
    override val id: String
        get() = "Test"

    override val isValid: Boolean
        get() = true

    override val templateClassName: String
        get() = "org.eclipse.buildship.kotlin.KotlinBuildScript"

    override val version: Int
        get() = 10
}

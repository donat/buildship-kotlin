package org.eclipse.buildship.kotlin

import org.gradle.internal.classpath.ClassPath
import org.gradle.script.lang.kotlin.provider.KotlinScriptPluginFactory
import org.gradle.script.lang.kotlin.support.KotlinBuildScriptModel
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents
import org.jetbrains.kotlin.script.ScriptDependenciesResolverEx
import java.io.File
import java.net.URLClassLoader
import java.util.Arrays

class GradleKotlinScriptDependenciesResolver : ScriptDependenciesResolverEx {

    override fun resolve(script: ScriptContents, environment: Map<String, Any?>?, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {
		if (environment == null) {
			return previousDependencies			
		} else {
			val projectRoot = environment["rootProject"] as File
			val model = KotlinModelQuery.retrieveKotlinBuildScriptModelFrom(projectRoot)
			return retrieveDependenciesFromProject(projectRoot, model)
		}
    }
	
	  private fun retrieveDependenciesFromProject(projectRoot: File, model: KotlinBuildScriptModel): KotlinScriptExternalDependencies {
	  	val classpath = model.classPath
		val gradleInstallation = classpath.find { it.absolutePath.contains("dists") && it.parentFile.name.equals("lib") }!!.parentFile
		val sources = /*gradleKotlinJar +*/ buildSrcRootsOf(projectRoot) + sourceRootsOf(gradleInstallation)
        return makeDependencies(emptyList(), sources)
	  }

    /**
     * Returns all conventional source directories under buildSrc if any.
     * This won't work for buildSrc projects with a custom source directory layout
     * but should account for the majority of cases and it's cheap.
     */
    private fun buildSrcRootsOf(projectRoot: File): Collection<File> =
        subDirsOf(File(projectRoot, "buildSrc/src/main"))

    private fun sourceRootsOf(gradleInstallation: File): Collection<File> =
        subDirsOf(File(gradleInstallation, "src"))

    private fun subDirsOf(dir: File): Collection<File> =
        if (dir.isDirectory)
            dir.listFiles().filter { it.isDirectory }
        else
            emptyList()

    private fun makeDependencies(classPath: Iterable<File>, sources: Iterable<File> = emptyList()): KotlinScriptExternalDependencies =
        object : KotlinScriptExternalDependencies {
            override val classpath = classPath
            override val imports = implicitImports
            override val sources = sources
        }

    companion object {
        val implicitImports = listOf(
        	"java.io.*",
            "org.gradle.api.plugins.*",
            "org.gradle.script.lang.kotlin.*")
    }
}

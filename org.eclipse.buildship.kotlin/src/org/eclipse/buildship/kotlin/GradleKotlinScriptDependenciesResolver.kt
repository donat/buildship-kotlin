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
			val scriptFile = script.file!!
			
			@Suppress("UNCHECKED_CAST")
			val rtPath = environment["rtPath"] as List<File>
			val projectRoot = rootProjectLocation(scriptFile)
			val model = KotlinModelQuery.retrieveKotlinBuildScriptModelFrom(projectRoot)
			return retrieveDependenciesFromProject(projectRoot, model, rtPath)
		}
    }
	
    private fun retrieveDependenciesFromProject(projectRoot: File, model: KotlinBuildScriptModel, rtPath: List<File>): KotlinScriptExternalDependencies {
        val (classpath, ignored) = model.classPath.partition { !it.name.startsWith("groovy") }
        val gradleKotlinJar = classpath.filter { it.name.startsWith("gradle-script-kotlin-") }
        val gradleInstallation = classpath.find { it.absolutePath.contains("dists") && it.parentFile.name.equals("lib") }!!.parentFile.parentFile
        val sources = gradleKotlinJar + buildSrcRootsOf(projectRoot) + sourceRootsOf(gradleInstallation)
		printProjectDependencies(classpath, ignored, gradleKotlinJar, gradleInstallation, sources, rtPath)
        return makeDependencies(rtPath + classpath, sources)
    }
	
	private fun printProjectDependencies(classpath: Iterable<File>, ignored: Iterable<File>, gradleKotlinJar: Iterable<File>, gradleInstallation: File, sources: Iterable<File>, rtPath: Iterable<File>) {
		System.out.println("--- Project dependencies ---")
		print("Runtime  :  ", rtPath)
		print("Classpath:  ", classpath)
		print("Ignored:    ", ignored)
		print("KotlinJar:  ", gradleKotlinJar)
		print("GradleDist: ", listOf(gradleInstallation))
		print("Sources:    ", sources)
	}
	
	private fun print(type: String, entries: Iterable<File>) {
		entries.forEach {  System.out.println("${type}${it.absolutePath}") }
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
	
	private fun rootProjectLocation(scriptFile: File): File {
		if (scriptFile.name == "build.gradle.kts") return File(scriptFile.parent)
		
		// TODO: search for project root directory
		return null!!
	} 
	
    companion object {
        val implicitImports = listOf(
            "org.gradle.api.plugins.*",
            "org.gradle.script.lang.kotlin.*")
    }
}

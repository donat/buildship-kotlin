package org.eclipse.buildship.kotlin

import org.gradle.script.lang.kotlin.support.KotlinBuildScriptModel
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptContents
import org.jetbrains.kotlin.script.ScriptDependenciesResolver
import org.jetbrains.kotlin.script.asFuture
import java.io.File
import java.util.concurrent.Future

class GradleKotlinScriptDependenciesResolver : ScriptDependenciesResolver {
	
	override fun resolve(script: ScriptContents,
				environment: Map<String, Any?>?,
				report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
				previousDependencies: KotlinScriptExternalDependencies?) : Future<KotlinScriptExternalDependencies?> {
		if (environment == null) {
			return makeDependencies(emptyList()).asFuture()			
		} else {
			@Suppress("UNCHECKED_CAST")
			val rtPath = environment["rtPath"] as List<File>
			val projectRoot = environment["rootProject"] as File
			val model = KotlinModelQuery.retrieveKotlinBuildScriptModelFrom(projectRoot)
			return retrieveDependenciesFromProject(projectRoot, model, rtPath).asFuture()
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
	
    companion object {
        val implicitImports = listOf(
            "org.gradle.api.plugins.*",
            "org.gradle.script.lang.kotlin.*")
    }
}

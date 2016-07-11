package org.eclipse.buildship.kotlin

import org.gradle.script.lang.kotlin.provider.KotlinScriptPluginFactory
import org.gradle.script.lang.kotlin.support.KotlinBuildScriptModel
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import java.io.File
import java.net.URI

/**
 * Copied from gradle-script-kotlin project
 */

object KotlinModelQuery {

	fun retrieveKotlinBuildScriptModelFrom(projectDir: File, javaHome: File? = null): KotlinBuildScriptModel {
		return withConnectionFrom(connectorFor(projectDir)) {
			model(KotlinBuildScriptModel::class.java)
				.setJavaHome(javaHome)
				.setJvmArguments("-D${KotlinScriptPluginFactory.modeSystemPropertyName}=${KotlinScriptPluginFactory.classPathMode}")
				.get()
		}
	}

	fun connectorFor(projectDir: File): GradleConnector =
			GradleConnector.newConnector().forProjectDirectory(projectDir).useDistribution(URI("https://repo.gradle.org/gradle/dist-snapshots/gradle-script-kotlin-3.0.0-20160801131723+0000-all.zip"))

	inline fun <T> withConnectionFrom(connector: GradleConnector, block: ProjectConnection.() -> T): T =
			connector.connect().use(block)

	inline fun <T> ProjectConnection.use(block: (ProjectConnection) -> T): T {
		try {
			return block(this)
		} finally {
			close()
		}
	}
}


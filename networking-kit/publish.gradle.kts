import java.util.Properties
import java.io.FileInputStream

apply(plugin = "maven-publish")

// Load GitHub properties
val githubProperties = Properties().apply {
    load(FileInputStream(rootProject.file("github.properties"))) // Set env variable GPR_USER & GPR_API_KEY if not adding a properties file
}

afterEvaluate {
    val group = "com.indiedev"
    val versionCode = "1.0.0"
    val githubUrl = "https://maven.pkg.github.com/madnankhan56/networking-kit"

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = group
                artifactId = "networking-kit-release"
                version = versionCode
            }

            create<MavenPublication>("debug") {
                from(components["debug"])

                groupId = group
                artifactId = "networking-kit-debug"
                version = versionCode
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri(githubUrl)

                credentials {
                    username = githubProperties["gpr.usr"] as String? ?: System.getenv("GPR_USER")
                    password = githubProperties["gpr.key"] as String? ?: System.getenv("GPR_API_KEY")
                }
            }
        }
    }
}



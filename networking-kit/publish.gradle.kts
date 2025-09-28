import java.util.Properties
import java.io.FileInputStream

apply(plugin = "maven-publish")

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.indiedev"         // 👈 custom groupId
            artifactId = "networkingKit-release"    // 👈 library name
            version = "1.0.0"                // 👈 version (from your tag)

            afterEvaluate {
                from(components["release"])
            }
        }

        create<MavenPublication>("debug") {
            groupId = "com.indiedev"         // 👈 custom groupId
            artifactId = "networkingKit-debug"    // 👈 library name
            version = "1.0.0"

            from(components["debug"])
        }
    }
}


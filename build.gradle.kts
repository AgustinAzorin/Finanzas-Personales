// Root build file. Each subproject applies its own plugins (see app/build.gradle.kts
// and engine/build.gradle.kts) so that building a single module — e.g. `:engine:test` —
// does not force Gradle to resolve plugins (like AGP) that only the other module needs.

plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modCompileOnly("curse.maven:jei-238222:6600227")
    modCompileOnly("curse.maven:configured-457570:4011355")
    modCompileOnly("curse.maven:upgrade-aquatic-326895:6393133")
    modCompileOnly("curse.maven:blueprint-382216:6408581")
}
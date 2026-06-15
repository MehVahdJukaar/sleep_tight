plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}")

    modCompileOnly("curse.maven:yacl-667299:3987709")
    modCompileOnly("com.terraformersmc:modmenu:4.0.6")
}
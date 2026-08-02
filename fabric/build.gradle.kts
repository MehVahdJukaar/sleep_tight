plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version = extra["moonlight_version"] as String
val mc_version = extra["mc_version"] as String
val sbl_version = extra["sbl_version"] as String
val codecui_version = extra["codecui_version"] as String
val supplementaries_version = extra["supplementaries_version"] as String

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}")
    modRuntimeOnly("net.mehvahdjukaar:codecui-fabric:${codecui_version}")

    modCompileOnly("net.mehvahdjukaar:supplementaries-fabric:${supplementaries_version}")
    modImplementation("curse.maven:heartstone-573152:7278328")
    modCompileOnly("curse.maven:handcrafted-538214:6330030")
    modCompileOnly("curse.maven:resourceful-lib-570073:5659871")
    modCompileOnly("net.tslat.smartbrainlib:SmartBrainLib-fabric-${mc_version}:${sbl_version}")

    modCompileOnly("curse.maven:yacl-667299:3987709")
    modCompileOnly("maven.modrinth:modmenu:11.0.4")
    modCompileOnly("curse.maven:entity-model-features-844662:5722728")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:5000985")
    modCompileOnly("curse.maven:quark-243121:8146177")
    modCompileOnly("curse.maven:zeta-968868:7980010")

    //   modRuntimeOnly("curse.maven:artsandcrafts-1034791:8041066")
    //  modRuntimeOnly("curse.maven:jinxedlib-1203401:6727690")
}

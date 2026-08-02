plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val moonlight_version = extra["moonlight_version"] as String
val mc_version = extra["mc_version"] as String
val sbl_version = extra["sbl_version"] as String
val supplementaries_version = extra["supplementaries_version"] as String
val heartstone_version = extra["heartstone_version"] as String

dependencies {
    //@jar skips moonlight's module metadata: its jar variants are tagged neoforge-only, so in this module gradle
    //would otherwise fall back to the access transformer variant and the whole api would be missing from the classpath
    modCompileOnly("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}@jar")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modImplementation("curse.maven:supplementaries-412082:4375776")
    modImplementation("curse.maven:heartstone-573152:7278328")
    modImplementation("curse.maven:handcrafted-538214:6330030")
    modImplementation("curse.maven:resourceful-lib-570073:5659871")

    modImplementation("net.tslat.smartbrainlib:SmartBrainLib-fabric-${mc_version}:${sbl_version}")

    modCompileOnly("curse.maven:entity-model-features-844662:5722728")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:5000985")
    modCompileOnly("curse.maven:quark-243121:8146177")
    modCompileOnly("curse.maven:zeta-968868:7980010")
}
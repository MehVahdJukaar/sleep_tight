plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val mc_version: String by extra
val sbl_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modImplementation("curse.maven:supplementaries-412082:4375776")
    modImplementation("curse.maven:heartstone-573152:7278328")
    modImplementation("curse.maven:handcrafted-538214:6330030")
    modImplementation("curse.maven:resourceful-lib-570073:5659871")
    modImplementation("net.tslat.smartbrainlib:SmartBrainLib-fabric-${mc_version}:${sbl_version}")

    modCompileOnly("curse.maven:jei-238222:6600227")
    modCompileOnly("curse.maven:configured-457570:4011355")
    modCompileOnly("curse.maven:upgrade-aquatic-326895:6393133")
    modCompileOnly("curse.maven:blueprint-382216:6408581")
    modCompileOnly("curse.maven:entity-model-features-844662:5722728")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:5000985")
    modCompileOnly("curse.maven:quark-243121:8146177")
    modCompileOnly("curse.maven:zeta-968868:7980010")
}

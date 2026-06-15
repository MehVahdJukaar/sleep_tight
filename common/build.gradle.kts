plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val moonlight_version: String by extra
val mc_version: String by extra
val sbl_version: String by extra
val supplementaries_version: String by extra
val heartstone_version: String by extra

dependencies {
    modCompileOnly("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modImplementation("curse.maven:supplementaries-412082:4375776")
    modImplementation("curse.maven:heartstone-573152:7278328")
    modImplementation("curse.maven:handcrafted-538214:6330030")
    modImplementation("curse.maven:resourceful-lib-570073:5659871")

    modImplementation("net.tslat.smartbrainlib:SmartBrainLib-fabric-${mc_version}:${sbl_version}")

    modCompileOnly("curse.maven:entity-model-features-844662:5722728")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:5000985")
    modCompileOnly("org.violetmoon.quark:Quark-4.0-beta-426")
    modCompileOnly("org.violetmoon.zeta:Zeta-1.0-beta-1")
}
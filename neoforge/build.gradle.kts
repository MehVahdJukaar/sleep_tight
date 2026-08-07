plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val codecui_version = extra["codecui_version"] as String
val mc_version: String by extra
val sbl_version: String by extra

dependencies {
    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    modRuntimeOnly("net.mehvahdjukaar:codecui-neoforge:${codecui_version}")

    modCompileOnly("curse.maven:supplementaries-412082:4375776")
    modImplementation("curse.maven:heartstone-573152:7278328")
    modImplementation("curse.maven:handcrafted-538214:6330030")
    modImplementation("curse.maven:resourceful-lib-570073:5973188")
    //modImplementation("net.tslat.smartbrainlib:SmartBrainLib-fabric-${mc_version}:${sbl_version}")

    modCompileOnly("curse.maven:jei-238222:6600227")
    modCompileOnly("curse.maven:configured-457570:4011355")
    modCompileOnly("curse.maven:upgrade-aquatic-326895:6393133")
    modCompileOnly("curse.maven:blueprint-382216:6408581")
    modCompileOnly("curse.maven:entity-model-features-844662:5722728")
    modCompileOnly("curse.maven:entity-texture-features-fabric-568563:5000985")
    modCompileOnly("curse.maven:quark-243121:8146177")
    modCompileOnly("curse.maven:zeta-968868:7980010")
    //issue triage only, remove when done: #146 (3d skin layers) and #139 (sable sublevels)
    //version id, not "1.11.2": 41 artifacts share that version number and gradle picks the fabric one
    modCompileOnly("maven.modrinth:3dskinlayers:xPYbAPfz")
    modCompileOnly("curse.maven:fsable-1312371:8263584")

    modRuntimeOnly("curse.maven:farmers-delight-398521:8083481")
    modRuntimeOnly("curse.maven:artsandcrafts-1034791:8041062")
    modRuntimeOnly("curse.maven:jinxedlib-1203401:6727693")
    modRuntimeOnly("curse.maven:delighto-flight-1347014:8012635")
}

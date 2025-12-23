plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("gg.essential.multi-version.root")
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("io.github.juuxel.loom-quiltflower-mini") version "171a6e2e49" apply false
}

preprocess {
    val fabric12108 = createNode("1.21.8-fabric", 12108, "yarn")
    val forge10809 = createNode("1.8.9-forge", 10809, "srg")
    
    fabric12108.link(forge10809)
}
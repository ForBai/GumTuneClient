plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("gg.essential.multi-version.root")
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("io.github.juuxel.loom-quiltflower-mini") version "171a6e2e49" apply false
}

preprocess {
    val fabric12108 = createNode("1.21.8-fabric", 12108, "yarn")
    val forge10809 = createNode("1.8.9-forge", 10809, "srg")
    
    fabric12108.link(forge10809)
}
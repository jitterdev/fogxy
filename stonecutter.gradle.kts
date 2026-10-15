plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1"

stonecutter parameters {
    replacements.string(current.parsed >= "1.21.5") {
        replace("com.mojang.blaze3d.platform.GlStateManager", "com.mojang.blaze3d.opengl.GlStateManager")
        replace("getTimer()", "getDeltaTracker()")
    }
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
        replace("camera.getPosition()", "camera.position()")
    }
    replacements.string(current.parsed >= "26.2") {
        replace("getMainCamera()", "mainCamera()")
    }
}

tasks.register("runActiveClient") {
    group = "stonecutter"
    description = "Runs the client of the active Stonecutter version"
    dependsOn(provider { ":${stonecutter.current!!.project}:runClient" })
}

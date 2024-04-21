@file:JvmName("ApiInit")

package io.github.joemama.loader.api

import io.github.joemama.loader.ModLoader
import io.github.joemama.loader.api.event.LoaderEvents
import io.github.joemama.loader.api.event.MapEventContainer
import io.github.joemama.loader.asm.AccessModifier
import io.github.joemama.loader.asm.InjectionPoint
import io.github.joemama.loader.asm.openMethod
import io.github.joemama.loader.transformer.ClassContainer
import io.github.joemama.loader.transformer.Transformation
import net.minecraft.server.Bootstrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface CommonEntrypoint {
    companion object {
        const val KEY = "common"
    }

    fun onInit()
}

object LoaderApi {
    val logger: Logger = LoggerFactory.getLogger("Kittens API")
}

@Suppress("unused")
fun apiInit() {
    LoaderApi.logger.trace("Calling common entrypoint")
    ModLoader.callEntrypoint(CommonEntrypoint.KEY, CommonEntrypoint::onInit)
    LoaderEvents.entrypointLoaded.fire(MapEventContainer.JointEventContext(CommonEntrypoint.KEY, Unit))
}

object BuiltInRegistriesTransformation : Transformation {
    override fun transform(container: ClassContainer) {
        container.openMethod("bootStrap", "()V") {
            inject(InjectionPoint.Invoke(owner, "freeze", limit = 1)) {
                // running this early in case mods want to print using System.out
                invokeStatic(locate(Bootstrap::class.java), "wrapStreams")
                invokeStatic(locate("io.github.joemama.loader.api.ApiInit"), "apiInit")
            }
        }
    }
}

object BootstrapTransformation : Transformation {
    override fun transform(container: ClassContainer) {
        container.openMethod("wrapStreams", "()V") { access(AccessModifier.PUBLIC) }
    }
}

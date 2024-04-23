package felis.kittens.core.pack

import felis.ModLoader
import felis.kittens.core.Kittens
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.CompositePackResources
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionSerializer
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.Pack.Info
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.repository.RepositorySource
import net.minecraft.server.packs.resources.IoSupplier
import net.minecraft.world.flag.FeatureFlagSet
import java.io.InputStream
import java.nio.file.Files
import java.util.function.Consumer
import java.util.function.UnaryOperator
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

// TODO: Reloadable listener and Atlas stitching
object ModPackSource : RepositorySource {
    override fun loadPacks(registrar: Consumer<Pack>) {
        val packSource = PackSource.create(UnaryOperator.identity(), true)
        val modPacks = ModLoader.discoverer.asSequence()
            .map { it.meta.modid }
            .filter { it != Kittens.MODID }
            .map { ModPackResources(it) }
            .toList()
        val primary = ModPackResources(Kittens.MODID)
        val pack = CompositePackResources(primary, modPacks)

        registrar.accept(
            Pack.create(
                "felis_data",
                Component.translatable("kittens.resources"),
                true,
                SimpleReferenceSupplier(pack),
                Info(
                    Component.translatable("kittens.resources"),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    listOf()
                ),
                Pack.Position.TOP,
                true,
                packSource
            )
        )
    }
}

data class SimpleReferenceSupplier(val ref: PackResources) : ResourcesSupplier {
    override fun openPrimary(p0: String): PackResources = this.ref
    override fun openFull(p0: String, p1: Info): PackResources = this.ref
}

class ModPackResources(private val modid: String) : PackResources {
    private val mod = ModLoader.discoverer.first { it.meta.modid == this.modid }
    override fun close() {}

    override fun getRootResource(vararg path: String?): IoSupplier<InputStream>? =
        this.getResource(path.joinToString("/"))

    override fun getResource(type: PackType, path: ResourceLocation): IoSupplier<InputStream>? =
        this.getResource("${type.directory}/${path.namespace}/${path.path}")

    private fun getResource(path: String): IoSupplier<InputStream>? =
        this.mod.getContentPath(path)?.let { IoSupplier.create(it) }

    override fun listResources(type: PackType, namespace: String, path: String, output: PackResources.ResourceOutput) {
        val fullPath = "${type.directory}/$namespace/$path"
        val dir = this.mod.contentCollection.getContentPath(fullPath) ?: return
        Files.walk(dir).asSequence().filter { !it.isDirectory() }
            .map { Pair(ResourceLocation(namespace, it.relativeTo(dir.parent).pathString), it) }
            .forEach { (loc, path) ->
                output.accept(loc, IoSupplier.create(path))
            }
    }

    override fun getNamespaces(type: PackType): MutableSet<String> =
        this.mod.contentCollection.getContentPath(type.directory)?.let { path ->
            Files.list(path).use { stream ->
                stream.asSequence()
                    .filter { it.isDirectory() }
                    .map { it.name }
                    .toCollection(mutableSetOf())
            }
        } ?: mutableSetOf()

    override fun <T : Any?> getMetadataSection(ser: MetadataSectionSerializer<T>): T? = null
    override fun packId(): String = this.modid
}
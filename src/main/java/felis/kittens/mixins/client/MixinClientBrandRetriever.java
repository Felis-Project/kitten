package felis.kittens.mixins.client;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.obfuscate.DontObfuscate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ClientBrandRetriever.class)
public class MixinClientBrandRetriever {
    /**
     * @author 0xJoeMama
     * @reason nobody else gets the right to change our branding
     */
    @DontObfuscate
    @Overwrite
    public static String getClientModName(){
        return "Felis";
    }
}

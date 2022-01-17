package traben.emissive_entities.mixin.client.entity.multitexture;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.BeeEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.LlamaEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.emissive_entities.client.EmissiveFeatureRenderer;

import static traben.emissive_entities.client.EmissiveClient.addEmissiveRenderer;

@Mixin(LlamaEntityRenderer.class)
public class MIX_LlamaEntityRenderer {
    public LlamaEntity mainEntity;

    @Inject(method = "getTexture(Lnet/minecraft/entity/passive/LlamaEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"))
    private void injected(LlamaEntity llamaEntity, CallbackInfoReturnable<Identifier> cir) {
        mainEntity = llamaEntity;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void injected(EntityRendererFactory.Context ctx, EntityModelLayer layer, CallbackInfo ci) {

        LivingEntityRenderer renderer = (LivingEntityRenderer) (Object)this;
        FeatureRenderer[]adding= {new EmissiveFeatureRenderer(renderer){
            @Override
            public RenderLayer getEyesTexture() {
                //return RenderLayer.getEyes(new Identifier("textures/entity/chicken_e.png"));
                String path = getTexture(mainEntity).getPath().replace(".png","_e.png");
                return RenderLayer.getEyes(new Identifier(path));
            }
        }};
        addEmissiveRenderer(renderer,ctx,adding);
    }

   // this.features.add(new EndermanEyeFeatureRenderer(this))
}

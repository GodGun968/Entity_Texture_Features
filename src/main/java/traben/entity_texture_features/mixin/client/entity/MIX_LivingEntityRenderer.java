package traben.entity_texture_features.mixin.client.entity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_texture_features.client.entity_texture_features_METHODS;
import traben.entity_texture_features.client.randomCase;

import java.util.UUID;

import static traben.entity_texture_features.client.entity_texture_features_CLIENT.*;

@Mixin(LivingEntityRenderer.class)
public abstract class MIX_LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M>, entity_texture_features_METHODS {
    @Shadow public abstract M getModel();

    @Shadow protected M model;

    @Shadow @Nullable protected abstract RenderLayer getRenderLayer(T entity, boolean showBody, boolean translucent, boolean showOutline);

    protected MIX_LivingEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }


    //  [0] = total randoms, [1] = self random
    private static float ticker = 0;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", shift = At.Shift.AFTER))
    private void applyEmissive(T livingEntity, float a, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        UUID id = livingEntity.getUuid();
        String fileString = getTexture(livingEntity).getPath();
        //System.out.println(fileString);
        if (UUID_randomTextureSuffix.containsKey(id)) {
            if (UUID_randomTextureSuffix.get(id) != 0 && optifineOldOrVanilla.containsKey(fileString)) {
                fileString = switch (optifineOldOrVanilla.get(fileString)) {
                    case 0 -> fileString.replace("textures", "optifine/random");
                    case 1 -> fileString.replace("textures/entity", "optifine/mob");
                    default -> fileString;
                };
                fileString = fileString.replace(".png", UUID_randomTextureSuffix.get(id) + ".png");
            }
        }
        if (Texture_Emissive.containsKey(fileString)) {
            if (Texture_Emissive.get(fileString) != null) {
                //VertexConsumer textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getEyes(Texture_Emissive.get(fileString)));
                VertexConsumer textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(Texture_Emissive.get(fileString),true));
                //one check most efficient instead of before and after applying
                if (irisDetected) {
                    matrixStack.scale(1.015f, 1.015f, 1.015f);
                    this.getModel().render(matrixStack
                            , textureVert
                            , 15728640
                            , OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                    matrixStack.scale(1f, 1f, 1f);
                }else{
                    this.getModel().render(matrixStack
                            , textureVert
                            , 15728640
                            , OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        } else {//creates and sets emissive for texture if it exists
            Identifier fileName_e = new Identifier(fileString.replace(".png", emissiveSuffix+".png"));
            if (isExistingFile( fileName_e)) {
                VertexConsumer textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(fileName_e,true));
                Texture_Emissive.put(fileString, fileName_e);
                //one check most efficient instead of before and after applying
                if (irisDetected) {
                    matrixStack.scale(1.015f, 1.015f, 1.015f);
                    this.getModel().render(matrixStack, textureVert, 15728640, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                    matrixStack.scale(1f, 1f, 1f);
                }else{
                    this.getModel().render(matrixStack, textureVert, 15728640, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            } else {
                Texture_Emissive.put(fileString, null);
            }
        }
        //cheeky lil fun for the dev
        //just makes my player look enchanted to others in multiplayer ;P
        if (livingEntity.getUuid().toString().equals("fd22e573-178c-415a-94fe-e476b328abfd")){
            //glow
            //VertexConsumer textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(getTexture(livingEntity),true));

            //enchanted
            VertexConsumer textureVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(this.getTexture(livingEntity)), false, true);
            this.getModel().render(matrixStack, textureVert, 15728640, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);

            //creeper overlay
//            ticker += 0.3;
//            int f = (int) ticker;
//            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEnergySwirl(new Identifier("textures/entity/creeper/creeper_armor.png"), f* 0.01F  % 1.0F, f * 0.01F % 1.0F));
//            matrixStack.scale(1.015f, 1.015f, 1.015f);
//            this.getModel().render(matrixStack, vertexConsumer, 15728640, OverlayTexture.DEFAULT_UV, 0.5F, 0.5F, 0.5F, 0.5F);
//            matrixStack.scale(1f, 1f, 1f);
//            if (ticker >640) ticker=0;
        }
    }


    @Redirect(
            method = "getRenderLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getTexture(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/Identifier;"))
    private Identifier returnOwnRandomTexture(@SuppressWarnings("rawtypes") LivingEntityRenderer instance, Entity entity) {

        @SuppressWarnings("unchecked") Identifier vanilla = getTexture((T) entity);
        String path = vanilla.getPath();
        UUID id = entity.getUuid();
        try {
            //if needs to check if change required
            if (UUID_entityAwaitingDataClearing.containsKey(id)){
                //wait a tick
                if (UUID_entityAwaitingDataClearing.get(id)+1 < entity.world.getTime()) {
                    int hold = UUID_randomTextureSuffix.get(id);
                    resetSingleData(id);
                    for (randomCase test :
                            Texture_OptifineRandomSettingsPerTexture.get(path)) {
                        if (test.testEntity((LivingEntity) entity, UUID_entityAlreadyCalculated.contains(id))) {
                            UUID_randomTextureSuffix.put(id, test.getWeightedSuffix(id, true));//,ignoreOnePNG.get(path)));
                            break;
                        }
                    }
                    //if didnt change keep the same
                    if (!UUID_randomTextureSuffix.containsKey(id)) {
                        UUID_randomTextureSuffix.put(id, hold);
                    }
                    UUID_entityAwaitingDataClearing.remove(id);
                }
            }
            if (!Texture_OptifineOrTrueRandom.containsKey(vanilla.getPath())) {
                processNewRandomTextureCandidate(vanilla.getPath());
            }
            if (Texture_OptifineOrTrueRandom.get(path)) {//optifine random
                //if it doesn't have a random already assign one
                if (!UUID_randomTextureSuffix.containsKey(id)) {
                    System.out.println("doing it");
                    for (randomCase test :
                            Texture_OptifineRandomSettingsPerTexture.get(path)) {
                        if (test.testEntity((LivingEntity) entity, UUID_entityAlreadyCalculated.contains(id))) {
                            UUID_randomTextureSuffix.put(id, test.getWeightedSuffix(id,true));//,ignoreOnePNG.get(path)));
                            break;
                        }
                    }
                    //if all failed set to vanilla
                    if (!UUID_randomTextureSuffix.containsKey(id)) {
                        //System.out.println("Entity Texture Features - optifine properties failed to assign texture. setting "+entity.getEntityName()+" to vanilla texture");
                        UUID_randomTextureSuffix.put(id, 0);
                    }
                    if (!UUID_entityAlreadyCalculated.contains(id)){UUID_entityAlreadyCalculated.add(id);}
                }
                if (UUID_randomTextureSuffix.get(id) == 0) {
                    return vanilla;
                } else {
                    return switch (optifineOldOrVanilla.get(path)) {
                        case 0 -> new Identifier(vanilla.getPath().replace(".png", UUID_randomTextureSuffix.get(id) + ".png").replace("textures", "optifine/random"));
                        case 1 -> new Identifier(vanilla.getPath().replace(".png", UUID_randomTextureSuffix.get(id) + ".png").replace("textures/entity", "optifine/mob"));
                        default -> new Identifier(vanilla.getPath().replace(".png", UUID_randomTextureSuffix.get(id) + ".png"));
                    };
                }

            } else {//true random assign
                if (Texture_TotalTrueRandom.get(path) > 0) {
                    if (!UUID_randomTextureSuffix.containsKey(id)) {
                        int randomReliable = id.hashCode() > 0 ? id.hashCode() : -id.hashCode();
                        randomReliable %= Texture_TotalTrueRandom.get(path) + 1;
                        if (randomReliable == 1){ //&& ignoreOnePNG.get(path)) {
                            randomReliable = 0;
                        }
                        UUID_randomTextureSuffix.put(id, randomReliable);
                        if (!UUID_entityAlreadyCalculated.contains(id)){UUID_entityAlreadyCalculated.add(id);}
                    }
                    if (UUID_randomTextureSuffix.get(id) == 0) {
                        return vanilla;
                    } else {
                        return switch (optifineOldOrVanilla.get(path)) {
                            case 0 -> new Identifier(vanilla.getPath().replace(".png", UUID_randomTextureSuffix.get(id) + ".png").replace("textures", "optifine/random"));
                            case 1 -> new Identifier(vanilla.getPath().replace(".png", UUID_randomTextureSuffix.get(id) + ".png").replace("textures/entity", "optifine/mob"));
                            default -> new Identifier(vanilla.getPath().replace(".png", UUID_randomTextureSuffix.get(id) + ".png"));
                        };
                    }
                } else {
                    return vanilla;
                }
            }

        }catch(Exception e){
            return vanilla;
        }
    }


}



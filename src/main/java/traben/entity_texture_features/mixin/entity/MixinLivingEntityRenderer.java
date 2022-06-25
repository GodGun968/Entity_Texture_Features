package traben.entity_texture_features.mixin.entity;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_texture_features.texture_handlers.ETFManager;
import traben.entity_texture_features.texture_handlers.ETFTexture;
import traben.entity_texture_features.utils.ETFCustomPlayerFeatureModel;
import traben.entity_texture_features.utils.ETFPlayerSkinUtils;

import java.util.UUID;

import static traben.entity_texture_features.ETFClient.ETFConfigData;

@SuppressWarnings("rawtypes")
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {
    private ETFCustomPlayerFeatureModel customPlayerModel = null;
    private ETFTexture thisETFTexture = null;
    //double it
    private int etf$timerBeforeTrySkin = 400;

    protected MixinLivingEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);

    }

    @Shadow
    public abstract M getModel();

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V", shift = At.Shift.AFTER)
    )
    private void etf$applyRenderFeatures(T livingEntity, float a, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        //UUID id = livingEntity.getUuid();
        if (!(livingEntity instanceof PlayerEntity)) {

//            Identifier texture = etf$returnAlteredTexture((LivingEntityRenderer) (Object) this, livingEntity);
//            ETFUtils.generalEmissiveRenderModel(matrixStack, vertexConsumerProvider, texture, this.getModel());
            thisETFTexture.renderEmissive(matrixStack, vertexConsumerProvider, this.getModel());

        } else if (ETFConfigData.skinFeaturesEnabled) { // is a player
            customPlayerModel = new ETFCustomPlayerFeatureModel<>();
            etf$renderSkinFeatures(livingEntity.getUuid(), (PlayerEntity) livingEntity, matrixStack, vertexConsumerProvider, i);
        }
        //potion effects
        //todo return after rewrite and do better
//        if (ETFConfigData.enchantedPotionEffects != ETFConfig.EnchantedPotionEffectsEnum.NONE
//                && !livingEntity.getActiveStatusEffects().isEmpty()
//                && !livingEntity.hasStatusEffect(StatusEffects.INVISIBILITY)
//        ) {
//            etf$renderPotion(livingEntity, matrixStack, vertexConsumerProvider);
//        }

        //randomly mark texture for rechecking randomized by UUID


    }

    @Redirect(
            method = "getRenderLayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getTexture(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/Identifier;"))
    private Identifier etf$returnAlteredTexture(@SuppressWarnings("rawtypes") LivingEntityRenderer instance, Entity inEntity) {
        @SuppressWarnings("unchecked")
        T entity = (T) inEntity;

        //this is to support inspectio or other abstract rendering mods
        //a simple way to detect if the displayed entity is actually in a special rendering window

        if (entity instanceof PlayerEntity) {
            return getTexture(entity);
        }
        thisETFTexture = ETFManager.getETFTexture(getTexture(entity), entity);
        //        System.out.println("texturestate="+thisETFTexture.currentTextureState.toString());
//        if(use == null){
//            System.out.println(thisETFTexture);
//            System.out.println("failed cause null="+getTexture(entity).toString()+entity.getType());
//            return getTexture(entity);
//        }

        return thisETFTexture.getTextureIdentifier(entity);

//        Identifier originalIdentifierToBeUsedIfChanged = null;
//        String texturePath = textureIdentifier.toString();
//
//        UUID id = entity.getUuid();
//
//        //debug print handled in general return to capture non-LivingEntityRenderers
//        //ETFUtils.checkAndPrintEntityDebugIfNeeded(id,texturePath);
//
//        if (!(entity instanceof PlayerEntity)) {
//            if (entity instanceof ShulkerEntity) {
//                //set to use vanilla shulker properties if color has been changed
//                //setting the below will trigger a return to the original coloured shulker if no random is applied
//                originalIdentifierToBeUsedIfChanged = new Identifier(texturePath);
//                texturePath = "minecraft:textures/entity/shulker/shulker.png";
//                textureIdentifier = new Identifier(texturePath);
//            }
//            if (ETFConfigData.enableCustomTextures) {
//                try {
//                    // return via general method but also apply the original texture check if return is unalterred
//                    Identifier check = ETFUtils.generalProcessAndReturnAlteredTexture(textureIdentifier, entity);
//                    //System.out.println("texture="+check.toString());
//                    if (originalIdentifierToBeUsedIfChanged != null && check.toString().equals(textureIdentifier.toString())) {
//                        return ETFUtils.returnBlinkIdOrGiven(entity, originalIdentifierToBeUsedIfChanged.toString(), id);
//                    } else {
//                        return check;
//                    }
//                } catch (Exception e) {
//                    ETFUtils.logError(e.toString(), false);
//                }
//            }
//        } else { // is player
//            if (ETFConfigData.skinFeaturesEnabled) {
//                if (ETFConfigData.debugLoggingMode != ETFConfig.DebugLogMode.None && UUID_DEBUG_EXPLANATION_MARKER.contains(id)) {
//                    //stringbuilder as chat messages have a prefix that can be bothersome
//                    StringBuilder message = new StringBuilder();
//                    boolean inChat = ETFConfigData.debugLoggingMode == ETFConfig.DebugLogMode.Chat;
//                    message.append("ETF entity debug data for player [").append(entity.getDisplayName()).append("]");
//
//                    if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.containsKey(id))
//                        message.append("\nplayer has features=").append(ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.getBoolean(id));
//                    if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.containsKey(id))
//                        message.append("\nplayer skin has downloaded yet for check=").append(ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.getBoolean(id));
//
//                    ETFUtils.logMessage(String.valueOf(message), inChat);
//                    UUID_DEBUG_EXPLANATION_MARKER.remove(id);
//                }
//                if (etf$timerBeforeTrySkin > 0) {
//                    etf$timerBeforeTrySkin--;
//                } else {
//
//                    if (!ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.containsKey(id) && !ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.containsKey(id)) {
//                        ETFPlayerSkinUtils.checkPlayerForSkinFeatures(id, (PlayerEntity) entity);
//                    }
//                    if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.containsKey(id) && ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.containsKey(id)) {
//                        if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.getBoolean(id)) {
//                            if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.getBoolean(id)) {
//                                return ETFUtils.returnBlinkIdOrGiven(entity, ETFPlayerSkinUtils.SKIN_NAMESPACE + id + ".png", id, true);
//                            } else {
//                                return textureIdentifier;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        //return original if it was changed and should be set back to original
//        if (originalIdentifierToBeUsedIfChanged == null) {
//            return ETFUtils.returnBlinkIdOrGiven(entity, textureIdentifier.toString(), id);
//        } else {
//            return ETFUtils.returnBlinkIdOrGiven(entity, originalIdentifierToBeUsedIfChanged.toString(), id);
//        }
    }

    private void etf$renderPotion(T livingEntity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        VertexConsumer textureVert;
        switch (ETFConfigData.enchantedPotionEffects) {
            case ENCHANTED -> {
                textureVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(etf$returnAlteredTexture((LivingEntityRenderer) (Object) this, livingEntity)), false, true);
                this.getModel().render(matrixStack, textureVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
            }
            case GLOWING -> {
                //textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(etf$returnAlteredTexture((LivingEntityRenderer) (Object) this, livingEntity), true));
                if (ETFConfigData.fullBrightEmissives) {
                    textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(etf$returnAlteredTexture((LivingEntityRenderer) (Object) this, livingEntity), true));
                } else {
                    textureVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(etf$returnAlteredTexture((LivingEntityRenderer) (Object) this, livingEntity)));
                }

                this.getModel().render(matrixStack, textureVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
            }
            case CREEPER_CHARGE -> {
                int f = (int) ((float) livingEntity.world.getTime() / 10);
                VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEnergySwirl(new Identifier("textures/entity/creeper/creeper_armor.png"), f * 0.01F % 1.0F, f * 0.01F % 1.0F));
                matrixStack.scale(1.1f, 1.1f, 1.1f);
                this.getModel().render(matrixStack, vertexConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0.5F, 0.5F, 0.5F, 0.5F);
                matrixStack.scale(1f, 1f, 1f);
            }
        }
    }

    private void etf$renderSkinFeatures(UUID id, PlayerEntity player, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        //skin http://textures.minecraft.net/texture/a81cd0629057a42f3d8b7b714b1e233a3f89e33faeb67d3796a52df44619e888

        String skinPossiblyBlinking = etf$returnAlteredTexture((LivingEntityRenderer) (Object) this, player).toString();
        if (skinPossiblyBlinking.contains("_transparent")) {
            skinPossiblyBlinking = skinPossiblyBlinking.replace("_transparent", "");
        }
        if (etf$timerBeforeTrySkin > 0) {
            etf$timerBeforeTrySkin--;
        } else {
            if (!ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.containsKey(id) && !ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.containsKey(id)) {
                //check for mark
                ETFPlayerSkinUtils.checkPlayerForSkinFeatures(id, player);
            }
            if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.containsKey(id) && ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.containsKey(id)) {
                if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_SKIN_DOWNLOADED_YET.getBoolean(id)) {
                    if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FEATURES.getBoolean(id)) {

                        //villager nose
                        if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_VILLAGER_NOSE.getBoolean(id)) {

                            customPlayerModel.nose.copyTransform(((PlayerEntityModel) this.getModel()).head);
                            Identifier villager = new Identifier("textures/entity/villager/villager.png");
                            VertexConsumer villagerVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(villager));
                            customPlayerModel.nose.render(matrixStack, villagerVert, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                        }

                        //coat features
                        ItemStack armour = player.getInventory().getArmorStack(1);
                        if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_COAT.getBoolean(id) &&
                                player.isPartVisible(PlayerModelPart.JACKET) &&
                                !(armour.isOf(Items.CHAINMAIL_LEGGINGS) ||
                                        armour.isOf(Items.LEATHER_LEGGINGS) ||
                                        armour.isOf(Items.DIAMOND_LEGGINGS) ||
                                        armour.isOf(Items.GOLDEN_LEGGINGS) ||
                                        armour.isOf(Items.IRON_LEGGINGS) ||
                                        armour.isOf(Items.NETHERITE_LEGGINGS))
                        ) {
                            String coat = ETFPlayerSkinUtils.SKIN_NAMESPACE + id + "_coat.png";

                            if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FAT_COAT.getBoolean(id)) {
                                customPlayerModel.fatJacket.copyTransform(((PlayerEntityModel) this.getModel()).jacket);
                            } else {
                                customPlayerModel.jacket.copyTransform(((PlayerEntityModel) this.getModel()).jacket);
                            }
                            //perform texture features
                            VertexConsumer coatVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(new Identifier(coat)));
                            matrixStack.push();


                            if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FAT_COAT.getBoolean(id)) {
                                customPlayerModel.fatJacket.render(matrixStack, coatVert, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                            } else {
                                customPlayerModel.jacket.render(matrixStack, coatVert, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                            }
                            if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_ENCHANT_COAT.containsKey(id)) {
                                if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_ENCHANT_COAT.getBoolean(id)) {
                                    Identifier enchant = new Identifier(coat.replace(".png", "_enchant.png"));
                                    VertexConsumer enchantVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(enchant), false, true);

                                    if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FAT_COAT.getBoolean(id)) {
                                        customPlayerModel.fatJacket.render(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
                                    } else {
                                        customPlayerModel.jacket.render(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
                                    }
                                }
                            }
                            if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_EMISSIVE_COAT.containsKey(id)) {
                                if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_EMISSIVE_COAT.getBoolean(id)) {
                                    Identifier emissive = new Identifier(coat.replace(".png", "_e.png"));
                                    VertexConsumer emissVert;// = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissive, true));
                                    if (ETFConfigData.fullBrightEmissives) {
                                        emissVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissive, true));
                                    } else {
                                        emissVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(emissive));
                                    }

                                    if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_FAT_COAT.getBoolean(id)) {
                                        customPlayerModel.fatJacket.render(matrixStack, emissVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                                    } else {
                                        customPlayerModel.jacket.render(matrixStack, emissVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                                    }
                                }
                            }
                            matrixStack.pop();
                        }

                        //perform texture features
                        if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_ENCHANT_SKIN.getBoolean(id)) {
                            Identifier enchant = skinPossiblyBlinking.contains(".png") ?
                                    new Identifier(skinPossiblyBlinking.replace(".png", "_enchant.png")) :
                                    new Identifier(ETFPlayerSkinUtils.SKIN_NAMESPACE + id + "_enchant.png");
                            VertexConsumer enchantVert = ItemRenderer.getArmorGlintConsumer(vertexConsumerProvider, RenderLayer.getArmorCutoutNoCull(enchant), false, true);
                            this.getModel().render(matrixStack, enchantVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.16F);
                        }
                        if (ETFPlayerSkinUtils.UUID_PLAYER_HAS_EMISSIVE_SKIN.getBoolean(id)) {
                            Identifier emissive = skinPossiblyBlinking.contains(".png") ?
                                    new Identifier(skinPossiblyBlinking.replace(".png", "_e.png")) :
                                    new Identifier(ETFPlayerSkinUtils.SKIN_NAMESPACE + id + "_e.png");
                            VertexConsumer emissVert;// = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissive, true));
                            if (ETFConfigData.fullBrightEmissives) {
                                emissVert = vertexConsumerProvider.getBuffer(RenderLayer.getBeaconBeam(emissive, true));
                            } else {
                                emissVert = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(emissive));
                            }
                            this.getModel().render(matrixStack, emissVert, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
                        }
                    }
                }
            }

        }
    }

}



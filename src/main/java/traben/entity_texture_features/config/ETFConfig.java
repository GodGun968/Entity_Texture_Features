package traben.entity_texture_features.config;

public class ETFConfig {
    public boolean allowIllegalTexturePaths = false;
    public boolean enableCustomTextures = true;
    public boolean enableCustomBlockEntities = true;
    public UpdateFrequency textureUpdateFrequency_V2 = UpdateFrequency.Fast;
    public boolean restrictUpdateProperties = true;
    public boolean restrictBiome = true;
    public boolean restrictHeight = true;
    public boolean restrictBlock = true;
    public boolean restrictWeather = true;
    public boolean restrictDayTime = true;
    public boolean restrictMoonPhase = true;
    public boolean enableEmissiveTextures = true;
    public boolean enableEmissiveBlockEntities = true;

    public boolean fullBrightEmissives = false;

    public boolean specialEmissiveShield = true;
    public boolean alwaysCheckVanillaEmissiveSuffix = true;


    public EnchantedPotionEffectsEnum enchantedPotionEffects = EnchantedPotionEffectsEnum.NONE;
    public boolean skinFeaturesEnabled = true;
    public boolean skinFeaturesEnableTransparency = true;
    public boolean skinFeaturesEnableFullTransparency = false;
    public boolean skinFeaturesPrintETFReadySkin = false;

    public boolean ignoreConfigWarnings = false;

    public boolean enableEnemyTeamPlayersSkinFeatures = true;
    public boolean enableBlinking = true;
    public int blinkFrequency = 150;
    public int blinkLength = 1;
    public boolean enableTridents = true;
    public boolean enableElytra = true;
    public boolean elytraThicknessFix = false;

    public double advanced_IncreaseCacheSizeModifier = 1.0;

    public DebugLogMode debugLoggingMode = DebugLogMode.None;

    //string name stuff more indepth than other enum for backwards compatibility
    public enum EnchantedPotionEffectsEnum {
        NONE("None"),
        ENCHANTED("Enchanted"),
        GLOWING("Glowing"),
        CREEPER_CHARGE("Creeper Charge");

        private final String name;

        EnchantedPotionEffectsEnum(String nameX) {
            this.name = nameX;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unused")
    public enum UpdateFrequency {
        Never(-1),
        Slow(80),
        Average(20),
        Fast(5),
        Instant(1);

        final private int delay;

        UpdateFrequency(int delay) {
            this.delay = delay;
        }

        public int getDelay() {
            return delay;
        }
    }

    @SuppressWarnings("unused")
    public enum DebugLogMode {
        None,
        Log,
        Chat
    }

}

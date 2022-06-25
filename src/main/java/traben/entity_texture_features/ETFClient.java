package traben.entity_texture_features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.entity_texture_features.config.ETFConfig;
import traben.entity_texture_features.utils.ETFUtils2;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class ETFClient implements ClientModInitializer {


    //data for different elytra wing texture conversion
    public static final Object2ObjectOpenHashMap<Identifier, Identifier> IDENTIFIER_OPPOSITE_ELYTRA_IDENTIFIER = new Object2ObjectOpenHashMap<>();

    //trident entities do not send item name data to clients when thrown, this is to keep that name in memory so custom tridents can at least display until reloading
    public static final Object2ReferenceOpenHashMap<UUID, String> UUID_TRIDENT_NAME = new Object2ReferenceOpenHashMap<>();
    public static final String MOD_ID = "etf";
    //logging object
    public final static Logger LOGGER = LoggerFactory.getLogger("Entity Texture Features");
    //whether the iris mod was detected on load
    public static boolean irisDetected = false;
    //marks whether mooshroom mushroom overrides exist
    public static int mooshroomRedCustomShroom = 0;
    public static int mooshroomBrownCustomShroom = 0;
    public static Boolean lecternHasCustomTexture = null;
    //config object
    public static ETFConfig ETFConfigData;

    @Override
    public void onInitializeClient() {


        LOGGER.info("Loading! 1.19");

        if (FabricLoader.getInstance().getModContainer("iris").isPresent()) {
            //LOGGER.info("Iris mod detected : message will be shown in settings");
            irisDetected = true;
        }

        etf$loadConfig();
    }

    // config code based on bedrockify & actually unbreaking fabric config code
    // https://github.com/juancarloscp52/BedrockIfy/blob/1.17.x/src/main/java/me/juancarloscp52/bedrockify/Bedrockify.java
    // https://github.com/wutdahack/ActuallyUnbreakingFabric/blob/1.18.1/src/main/java/wutdahack/actuallyunbreaking/ActuallyUnbreaking.java
    public void etf$loadConfig() {
        File config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "entity_texture_features.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (config.exists()) {
            try {
                FileReader fileReader = new FileReader(config);
                ETFConfigData = gson.fromJson(fileReader, ETFConfig.class);
                fileReader.close();
                ETFUtils2.saveConfig();
            } catch (IOException e) {
                ETFUtils2.logMessage("Config could not be loaded, using defaults", false);
                ETFConfigData = new ETFConfig();
                ETFUtils2.saveConfig();
            }
        } else {
            ETFConfigData = new ETFConfig();
            ETFUtils2.saveConfig();
        }
    }

}

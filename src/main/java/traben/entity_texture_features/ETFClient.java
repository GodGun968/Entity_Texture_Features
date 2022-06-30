package traben.entity_texture_features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.entity_texture_features.config.ETFConfig;
import traben.entity_texture_features.utils.ETFUtils2;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class ETFClient implements ClientModInitializer {

    public static final String MOD_ID = "etf";
    //logging object
    public final static Logger LOGGER = LoggerFactory.getLogger("Entity Texture Features");
    //config object
    public static ETFConfig ETFConfigData;

    @Override
    public void onInitializeClient() {

        LOGGER.info("Loading! 1.19");
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

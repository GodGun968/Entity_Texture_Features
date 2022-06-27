package traben.entity_texture_features.texture_handlers;

import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.config.ETFConfig;
import traben.entity_texture_features.utils.ETFUtils2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static traben.entity_texture_features.ETFClient.ETFConfigData;

// ETF re-write
//this class will ideally be where everything in vanilla interacts to get ETF stuff done
public class ETFManager {

    /*
     * Storage reasonings
     *
     * for every storage map using an entity that cannot be stored in a fast-util primitive type
     * will utilise a cache that can clear contents after reaching certain sizes to prevent exceeding memory
     *
     * for every storage map keyed by a vanilla or confirmed existing texture they will remain as non clearing maps as they have an upper size limit
     *
     *
     *
     */
    public static final ObjectOpenHashSet<String> EMISSIVE_SUFFIX_LIST = new ObjectOpenHashSet<>();
    public static final UUID ETF_GENERIC_UUID = UUID.nameUUIDFromBytes(("GENERIC").getBytes());
    static final Object2LongOpenHashMap<UUID> ENTITY_BLINK_TIME = new Object2LongOpenHashMap<>();
    static final ETFLruCache<ETFCacheKey, ETFTexture> ENTITY_TEXTURE_MAP =
            //  CacheBuilder.newBuilder().maximumSize(2048).initialCapacity(512).build();
            new ETFLruCache<>() {
            };
    //if false variant 1 will need to use vanilla texture otherwise vanilla texture has an override in other directory
    //private static final Object2BooleanOpenHashMap<Identifier> OPTIFINE_1_HAS_REPLACEMENT = new Object2BooleanOpenHashMap<>();
    static final ETFLruCache<ETFCacheKey, ObjectImmutableList<String>> ENTITY_SPAWN_CONDITIONS_CACHE =
            //CacheBuilder.newBuilder().maximumSize(2048).initialCapacity(512).build();
            new ETFLruCache<>() {
            };
//    static final ETFCache<ETFCacheID, ETFTexture> ENTITY_FEATURE_MAP =
//            //CacheBuilder.newBuilder().maximumSize(2048).initialCapacity(512).build();
//            new ETFCache<>(2048) {
//            };
    //private static final Object2ReferenceOpenHashMap<@NotNull UUID, @NotNull ETFTexture> ENTITY_TEXTURE_MAP = new Object2ReferenceOpenHashMap<>();
    //this is a cache of all known ETFTexture versions of any existing resourcepack texture, used to prevent remaking objects
    private static final Object2ReferenceOpenHashMap<@NotNull Identifier, @Nullable ETFTexture> ETF_TEXTURE_CACHE = new Object2ReferenceOpenHashMap<>();
    //null means it is true random as in no properties
    private static final Object2ReferenceOpenHashMap<Identifier, @Nullable List<ETFTexturePropertyCase>> OPTIFINE_PROPERTY_CACHE = new Object2ReferenceOpenHashMap<>();
    private static final Object2BooleanOpenHashMap<UUID> ENTITY_IS_UPDATABLE = new Object2BooleanOpenHashMap<>();
    private static final ObjectOpenHashSet<UUID> ENTITY_UPDATE_QUEUE = new ObjectOpenHashSet<>();
    private static final ObjectOpenHashSet<UUID> ENTITY_DEBUG_QUEUE = new ObjectOpenHashSet<>();
    //contains the total number of variants for any given vanilla texture
    private static final Object2IntOpenHashMap<Identifier> TRUE_RANDOM_COUNT_CACHE = new Object2IntOpenHashMap<>();
    private static final ETFTexture ETF_ERROR_TEXTURE = getErrorETFTexture();

    private static ETFTexture getErrorETFTexture() {
        ETFUtils2.registerNativeImageToIdentifier(ETFUtils2.emptyNativeImage(), new Identifier("etf:error.png"));
        ETFUtils2.logWarn("getErrorETFTexture() was called, investigate this if called too much");
        return new ETFTexture(new Identifier("etf:error.png"));//, ETFTexture.TextureSource.GENERIC_DEBUG);
    }

    public static void reset() {
        ETF_TEXTURE_CACHE.clear();
        ENTITY_TEXTURE_MAP.clear();
        //ENTITY_FEATURE_MAP.clear();
        ENTITY_SPAWN_CONDITIONS_CACHE.clear();
        OPTIFINE_PROPERTY_CACHE.clear();
        ENTITY_IS_UPDATABLE.clear();
        ENTITY_UPDATE_QUEUE.clear();
        ENTITY_DEBUG_QUEUE.clear();
        TRUE_RANDOM_COUNT_CACHE.clear();
        ENTITY_BLINK_TIME.clear();

        ETFDirectory.clear();
        //reset emissive suffix
        EMISSIVE_SUFFIX_LIST.clear();
        try {
            List<Properties> props = new ArrayList<>();
            String[] paths = {"optifine/emissive.properties", "textures/emissive.properties", "etf/emissive.properties"};
            for (String path :
                    paths) {
                Properties prop = ETFUtils2.readAndReturnPropertiesElseNull(new Identifier(path));
                if (prop != null)
                    props.add(prop);
            }
            for (Properties prop :
                    props) {
                //not an optifine property that I know of but this has come up in a few packs, so I am supporting it
                if (prop.containsKey("entities.suffix.emissive")) {
                    if (prop.getProperty("entities.suffix.emissive") != null)
                        EMISSIVE_SUFFIX_LIST.add(prop.getProperty("entities.suffix.emissive"));
                }
                if (prop.containsKey("suffix.emissive")) {
                    if (prop.getProperty("suffix.emissive") != null)
                        EMISSIVE_SUFFIX_LIST.add(prop.getProperty("suffix.emissive"));
                }
            }
            if (ETFConfigData.alwaysCheckVanillaEmissiveSuffix) {
                EMISSIVE_SUFFIX_LIST.add("_e");
            }

            if (EMISSIVE_SUFFIX_LIST.size() == 0) {
                ETFUtils2.logMessage("no emissive suffixes found: default emissive suffix '_e' used");
                EMISSIVE_SUFFIX_LIST.add("_e");
            } else {
                ETFUtils2.logMessage("emissive suffixes loaded: " + EMISSIVE_SUFFIX_LIST);
            }
        } catch (Exception e) {
            ETFUtils2.logError("emissive suffixes could not be read: default emissive suffix '_e' used");
            EMISSIVE_SUFFIX_LIST.add("_e");
        }
    }

    public static void removeThisEntityDataFromAllStorage(ETFCacheKey ETFId){
        ENTITY_TEXTURE_MAP.removeEntryOnly(ETFId);
        //ENTITY_FEATURE_MAP.clear();
        ENTITY_SPAWN_CONDITIONS_CACHE.removeEntryOnly(ETFId);

        UUID uuid = ETFId.getMobUUID();
        ENTITY_IS_UPDATABLE.removeBoolean(uuid);
        ENTITY_UPDATE_QUEUE.remove(uuid);
        ENTITY_DEBUG_QUEUE.remove(uuid);
        ENTITY_BLINK_TIME.removeLong(uuid);
    }


    public static void checkIfShouldTriggerUpdate(UUID id) {
        //type safe check as returns false if missing

        if (ENTITY_IS_UPDATABLE.getBoolean(id)
                && ETFConfigData.enableCustomTextures
                && ETFConfigData.textureUpdateFrequency_V2 != ETFConfig.UpdateFrequency.Never) {
            if (ENTITY_UPDATE_QUEUE.size() > 2000)
                ENTITY_UPDATE_QUEUE.clear();
            int delay = ETFConfigData.textureUpdateFrequency_V2.getDelay();
            long randomizer = delay * 20L;
            if (System.currentTimeMillis() % randomizer == Math.abs(id.hashCode()) % randomizer
            ) {
                //marks texture to update next render if a certain delay time is reached
                ENTITY_UPDATE_QUEUE.add(id);
            }
        }
    }

    public static void markEntityForDebugPrint(UUID id) {
        if (ETFConfigData.debugLoggingMode != ETFConfig.DebugLogMode.None) {
            ENTITY_DEBUG_QUEUE.add(id);
        }
    }

//    @NotNull
//    public static <T extends Entity> ETFTexture getETFTextureOfFeature(T entity, Identifier vanillaFeatureTexture) {
//        UUID id = entity.getUuid();
//        if (id == GENERIC_UUID) {
//            return getETFDefaultTexture(vanillaFeatureTexture);
//        }
//
////        if (vanillaFeatureTexture == null) {
////            ETFUtils2.logError("feature texture was null");
////            return ETF_ERROR_TEXTURE;
////        }
////        if (entity == null) {
////            ETFUtils2.logError("feature entity was null");
////            return getETFDefaultTexture(vanillaFeatureTexture);
////        }
//
//        int UUID_TEXTURE = id.hashCode() + vanillaFeatureTexture.hashCode();
//
//        ETFTexture baseETFTexture = ENTITY_TEXTURE_MAP.get(id);
//        ETFTexture featureETFTexture = ENTITY_FEATURE_MAP.get(UUID_TEXTURE);
//        if (featureETFTexture != null && baseETFTexture != null) {
//            return featureETFTexture;
//        }
//
//        //usually only 1 feature except for villager and warden
//        if (ENTITY_TEXTURE_MAP.containsKey(id)) {
//            if (baseETFTexture != null) {
//                // = ENTITY_TEXTURE_MAP.peek(entity.getUuid());
//                Identifier possibleFeatureVariantIdentifier = baseETFTexture.getFeatureTexture(vanillaFeatureTexture);
//                ETFTexture possibleFeatureVariantETFTexture = getOrCreateETFTexture(vanillaFeatureTexture, possibleFeatureVariantIdentifier);
//                ENTITY_FEATURE_MAP.put(UUID_TEXTURE, possibleFeatureVariantETFTexture);
//                return possibleFeatureVariantETFTexture;
//            } else {
//                ENTITY_FEATURE_MAP.put(UUID_TEXTURE, getETFDefaultTexture(vanillaFeatureTexture));
//                return getETFDefaultTexture(vanillaFeatureTexture);
//            }
//        }
//        //nothing to work with might need 1 more go around
//        ETFUtils2.logWarn("getETFFeatureTexture() found a null, investigate this if it happens endlessly " + vanillaFeatureTexture + UUID_TEXTURE);
//        ENTITY_FEATURE_MAP.put(UUID_TEXTURE, getETFDefaultTexture(vanillaFeatureTexture));
//        return getETFDefaultTexture(vanillaFeatureTexture);
//    }


    @NotNull
    public static ETFTexture getETFDefaultTexture(Identifier vanillaIdentifier) {

        return getOrCreateETFTexture(vanillaIdentifier, vanillaIdentifier);
    }

    @NotNull
    public static <T extends Entity> ETFTexture getETFTexture(@NotNull Identifier vanillaIdentifier,@NotNull T entity, @NotNull TextureSource source) {

        //System.out.println("1");
//        UUID q1 = UUID.randomUUID();
//        UUID q2 = UUID.randomUUID();
//        ENTITY_TEXTURE_MAP.put(q1,new ETFTexture(vanillaIdentifier,vanillaIdentifier));
//        ENTITY_TEXTURE_MAP.put(q2,new ETFTexture(vanillaIdentifier,vanillaIdentifier));
//        System.out.println(ENTITY_TEXTURE_MAP.peek(q1).toString());
//        System.out.println(ENTITY_TEXTURE_MAP.peek(q2).toString());

        //runs even without custom textures enabled, as this supports emissives and vanilla textures

//        if (entity == null) {
//            return getETFDefaultTexture(vanillaIdentifier);
//        }//System.out.println("3");

        UUID id = entity.getUuid();
        //use custom cache id this differentiates feature renderer calls here and makes the base feature still identifiable by uuid only when features are called
        ETFCacheKey cacheKey = new ETFCacheKey(id,source == TextureSource.ENTITY_FEATURE ? vanillaIdentifier : null);
        //fastest in subsequent runs
        if (id == ETF_GENERIC_UUID || entity.getBlockPos().equals(Vec3i.ZERO)) {
            return getETFDefaultTexture(vanillaIdentifier);
        }
        if (ENTITY_TEXTURE_MAP.containsKey(cacheKey)) {
            ETFTexture quickReturn = ENTITY_TEXTURE_MAP.get(cacheKey);
            if (quickReturn == null) {
                ETFTexture vanillaETF = getETFDefaultTexture(vanillaIdentifier);
                ENTITY_TEXTURE_MAP.put(cacheKey, vanillaETF);
                quickReturn = vanillaETF;

            }
            if(source == TextureSource.ENTITY) {
                if (ENTITY_DEBUG_QUEUE.contains(id)) {
                    boolean inChat = ETFConfigData.debugLoggingMode == ETFConfig.DebugLogMode.Chat;
                    //todo better rewrite debug print
                    ETFUtils2.logMessage(quickReturn.toString(), inChat);
                    ETFUtils2.logMessage("entity cache size = " + ENTITY_TEXTURE_MAP.size() +
                            "\ntexture cache size = " + ETF_TEXTURE_CACHE.size() +
                            "\noriginal spawn state = " + ENTITY_SPAWN_CONDITIONS_CACHE.get(cacheKey) +
                            "\noptifine property key count = " + (OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier) != null && OPTIFINE_PROPERTY_CACHE.containsKey(vanillaIdentifier) ? OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier).size() : 0) +
                            "\ntrue random count = " + TRUE_RANDOM_COUNT_CACHE.getInt(vanillaIdentifier), inChat);
                    //ETFUtils2.logMessage("feature cache size = " + ENTITY_FEATURE_MAP.size(), inChat);
                    //ETFUtils.logMessage("cache="+ETF_TEXTURE_CACHE.keySet(),inChat);
                    ENTITY_DEBUG_QUEUE.remove(id);
                }
                if (ENTITY_UPDATE_QUEUE.contains(id)) {
                    //Identifier vanilla = context.getTexture(entity);
                    Identifier newVariantIdentifier = returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, true);
                    ENTITY_TEXTURE_MAP.put(cacheKey, Objects.requireNonNullElse(getOrCreateETFTexture(vanillaIdentifier, Objects.requireNonNullElse(newVariantIdentifier, vanillaIdentifier)), getETFDefaultTexture(vanillaIdentifier)));

                    ENTITY_UPDATE_QUEUE.remove(id);
                } else {
                    checkIfShouldTriggerUpdate(id);
                }
            }
            //System.out.println("quickreturn");
            //this is where 99.99% of calls here will end only the very first call to this method by an entity goes further
            //the first call by any entity of a type will go the furthest and be the slowest as it triggers the initial setup, this makes all future calls by the same entity type faster
            //this is as close as possible to method start I can move this without losing update and debug functionality
            //this is the focal point of the rewrite where all the optimization is expected
            return quickReturn;
        }
        //need to create or find an ETFTexture object for entity and find or add to cache and entity map
        //firstly just going to check if this mob is some sort of gui element or not a real mod


        Identifier possibleIdentifier;
        if(source == TextureSource.ENTITY_FEATURE){
            possibleIdentifier = getPossibleVariantIdentifierRedirectForFeatures(entity, vanillaIdentifier,source);
        }else{
            possibleIdentifier = getPossibleVariantIdentifier(entity, vanillaIdentifier,source);
        }

        ETFTexture foundTexture;
        foundTexture = Objects.requireNonNullElse(getOrCreateETFTexture(vanillaIdentifier, possibleIdentifier == null ? vanillaIdentifier : possibleIdentifier), getETFDefaultTexture(vanillaIdentifier));
        ENTITY_TEXTURE_MAP.put(cacheKey, foundTexture);
        return foundTexture;


    }

    @Nullable //when vanilla
    private static <T extends Entity> Identifier getPossibleVariantIdentifierRedirectForFeatures(T entity, Identifier vanillaIdentifier,TextureSource source) {
            Identifier regularReturnIdentifier = getPossibleVariantIdentifier(entity, vanillaIdentifier,source);
            if (regularReturnIdentifier == null || vanillaIdentifier.equals(regularReturnIdentifier)) {
                //random assignment either failed or returned texture1
                //as this is a feature we will also try one last time to match it to a possible variant of the base texture

                ETFCacheKey baseCacheId = new ETFCacheKey(entity.getUuid(), null);

                if (ENTITY_TEXTURE_MAP.containsKey(baseCacheId)) {
                    ETFTexture baseETFTexture = ENTITY_TEXTURE_MAP.get(baseCacheId);
                    if (baseETFTexture != null) {
                        return baseETFTexture.getFeatureTexture(vanillaIdentifier);
                    }
                }
            }else{
                return regularReturnIdentifier;
            }
            return null;
    }

    @Nullable //when vanilla
    private static <T extends Entity> Identifier getPossibleVariantIdentifier(T entity, Identifier vanillaIdentifier,TextureSource source) {

        if (ETFConfigData.enableCustomTextures) {
            //has this been checked before?
            if (TRUE_RANDOM_COUNT_CACHE.containsKey(vanillaIdentifier) || OPTIFINE_PROPERTY_CACHE.containsKey(vanillaIdentifier)) {
                //has optifine checked before?
                if (OPTIFINE_PROPERTY_CACHE.containsKey(vanillaIdentifier)) {
                    List<ETFTexturePropertyCase> optifineProperties = OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier);
                    if (optifineProperties != null) {
                        return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, false, optifineProperties);
                    }
                }
                //has true random checked before?
                if (TRUE_RANDOM_COUNT_CACHE.containsKey(vanillaIdentifier) && source != TextureSource.ENTITY_FEATURE) {
                    int randomCount = TRUE_RANDOM_COUNT_CACHE.getInt(vanillaIdentifier);
                    if (randomCount != TRUE_RANDOM_COUNT_CACHE.defaultReturnValue()) {
                        return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier, randomCount);
                    }
                }
                //if we got here the texture is NOT random after having already checked before so return null
                return null;
            }


            //this is a new texture, we need to find what kind of random it is

            //if not null the below two represent the highest version of said files
            Identifier possibleProperty = ETFDirectory.getDirectoryVersionOf(ETFUtils2.replaceIdentifier(vanillaIdentifier, ".png", ".properties"));
            Identifier possible2PNG = ETFDirectory.getDirectoryVersionOf(ETFUtils2.replaceIdentifier(vanillaIdentifier, ".png", "2.png"));


            //if both null vanilla fallback as no randoms
            if (possible2PNG == null && possibleProperty == null) {
                //this will tell next call with this texture that these have been checked already
                OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
                return null;
            } else if (/*only*/possibleProperty == null) {
                if( source != TextureSource.ENTITY_FEATURE) {
                    newTrueRandomTextureFound(vanillaIdentifier, possible2PNG);
                    return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier);
                }
            } else if (/*only*/possible2PNG == null) {
                //optifine random confirmed
                newOptifineTextureFound(vanillaIdentifier, possibleProperty);
                return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, false);
            } else {//neither null this will be annoying
                //if 2.png is higher it MUST be treated as true random confirmed
                ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
                String p2pngPackName = resources.getResource(possible2PNG).isPresent() ? resources.getResource(possible2PNG).get().getResourcePackName() : null;
                String propertiesPackName = resources.getResource(possibleProperty).isPresent() ? resources.getResource(possibleProperty).get().getResourcePackName() : null;
                ObjectOpenHashSet<String> packs = new ObjectOpenHashSet<>();
                //if (p2pngPackName != null)
                    packs.add(p2pngPackName);
                //if (propertiesPackName != null)
                    packs.add(propertiesPackName);
               // System.out.println("debug6534="+p2pngPackName+","+propertiesPackName+","+ETFUtils2.returnNameOfHighestPackFrom(packs));
                if (propertiesPackName != null && propertiesPackName.equals(ETFUtils2.returnNameOfHighestPackFrom(packs))) {
                    newOptifineTextureFound(vanillaIdentifier, possibleProperty);
                    return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, false);
                } else {
                    if( source != TextureSource.ENTITY_FEATURE) {
                        newTrueRandomTextureFound(vanillaIdentifier, possible2PNG);
                        return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier);
                    }
                }
            }
        }
        //marker to signify code has run before and is not random or true random
        OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
        //use vanilla as fallback
        return null;
    }

    private static void newOptifineTextureFound(Identifier vanillaIdentifier, Identifier properties) {

        try {
            Properties props = ETFUtils2.readAndReturnPropertiesElseNull(properties);

            if (props != null) {
                Set<String> propIds = props.stringPropertyNames();
                //set so only 1 of each
                Set<Integer> numbers = new HashSet<>();

                //get the numbers we are working with
                for (String str :
                        propIds) {
                    numbers.add(Integer.parseInt(str.replaceAll("\\D", "")));
                }
                //sort from lowest to largest
                List<Integer> numbersList = new ArrayList<>(numbers);
                Collections.sort(numbersList);
                List<ETFTexturePropertyCase> allCasesForTexture = new ArrayList<>();
                for (Integer num :
                        numbersList) {
                    //System.out.println("constructed as "+num);
                    //loops through each known number in properties
                    //all case.1 ect should be processed here
                    Integer[] suffixes = {};
                    Integer[] weights = {};
                    String[] biomes = {};
                    Integer[] heights = {};
                    ArrayList<String> names = new ArrayList<>();
                    String[] professions = {};
                    String[] collarColours = {};
                    int baby = 0; // 0 1 2 - dont true false
                    int weather = 0; //0,1,2,3 - no clear rain thunder
                    String[] health = {};
                    Integer[] moon = {};
                    String[] daytime = {};
                    String[] blocks = {};
                    String[] teams = {};
                    Integer[] sizes = {};

                    if (props.containsKey("skins." + num) || props.containsKey("textures." + num)) {
                        String dataFromProps = props.containsKey("skins." + num) ? props.getProperty("skins." + num).trim() : props.getProperty("textures." + num).trim();
                        String[] skinData = dataFromProps.split("\s+");
                        ArrayList<Integer> suffixNumbers = new ArrayList<>();
                        for (String data :
                                skinData) {
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    suffixNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    suffixNumbers.add(Integer.parseInt(data.replaceAll("\\D", "")));
                                }
                            }
                        }
                        suffixes = suffixNumbers.toArray(new Integer[0]);
                    }
                    if (props.containsKey("weights." + num)) {
                        String dataFromProps = props.getProperty("weights." + num).trim();
                        String[] weightData = dataFromProps.split("\s+");
                        ArrayList<Integer> builder = new ArrayList<>();
                        for (String s :
                                weightData) {
                            s = s.trim();
                            if (!s.replaceAll("\\D", "").isEmpty()) {
                                builder.add(Integer.parseInt(s.replaceAll("\\D", "")));
                            }
                        }
                        weights = builder.toArray(new Integer[0]);
                    }
                    if (props.containsKey("biomes." + num)) {
                        String dataFromProps = props.getProperty("biomes." + num).trim();
                        biomes = dataFromProps.toLowerCase().split("\s+");
                    }
                    //add legacy height support
                    if (!props.containsKey("heights." + num) && (props.containsKey("minHeight." + num) || props.containsKey("maxHeight." + num))) {
                        String min = "-64";
                        String max = "319";
                        if (props.containsKey("minHeight." + num)) {
                            min = props.getProperty("minHeight." + num).trim();
                        }
                        if (props.containsKey("maxHeight." + num)) {
                            max = props.getProperty("maxHeight." + num).trim();
                        }
                        props.put("heights." + num, min + "-" + max);
                    }
                    if (props.containsKey("heights." + num)) {
                        String dataFromProps = props.getProperty("heights." + num).trim();
                        String[] heightData = dataFromProps.split("\s+");
                        ArrayList<Integer> heightNumbers = new ArrayList<>();
                        for (String data :
                                heightData) {
                            data = data.replaceAll("\\(", "").replaceAll("\\)", "");
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    heightNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    heightNumbers.add(Integer.parseInt(data.replaceAll("\\D", "")));
                                }
                            }
                        }
                        heights = heightNumbers.toArray(new Integer[0]);
                    }

                    if (props.containsKey("names." + num)) {
                        String dataFromProps = props.getProperty("names." + num).trim();
                        if (dataFromProps.contains("regex:") || dataFromProps.contains("pattern:")) {
                            names.add(dataFromProps);
                        } else {
                            //names = dataFromProps.split("\s+");
                            //allow    "multiple names" among "other"
                            //List<String> list = new ArrayList<>();
                            //add the full line as the first name option to allow for simple multiple names
                            //incase someone just writes   names.1=john smith
                            //instead of                   names.1="john smith"
                            names.add(dataFromProps);

                            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(dataFromProps);
                            while (m.find()) {
                                names.add(m.group(1).replace("\"", "").trim());
                            }
                            //names.addAll(list);
                        }
                    }
                    if (props.containsKey("name." + num)) {
                        String dataFromProps = props.getProperty("name." + num).trim();
                        names.add(dataFromProps);
                    }
                    if (props.containsKey("professions." + num)) {
                        professions = props.getProperty("professions." + num).trim().split("\s+");
                    }
                    if (props.containsKey("collarColors." + num) || props.containsKey("colors." + num)) {
                        collarColours = props.containsKey("collarColors." + num) ? props.getProperty("collarColors." + num).trim().split("\s+") : props.getProperty("colors." + num).trim().split("\s+");
                    }
                    if (props.containsKey("baby." + num)) {
                        String dataFromProps = props.getProperty("baby." + num).trim();
                        switch (dataFromProps) {
                            case "true" -> baby = 1;
                            case "false" -> baby = 2;
                        }
                    }
                    if (props.containsKey("weather." + num)) {
                        String dataFromProps = props.getProperty("weather." + num).trim();
                        switch (dataFromProps) {
                            case "clear" -> weather = 1;
                            case "rain" -> weather = 2;
                            case "thunder" -> weather = 3;
                        }
                    }
                    if (props.containsKey("health." + num)) {
                        health = props.getProperty("health." + num).trim().split("\s+");
                    }
                    if (props.containsKey("moonPhase." + num)) {
                        String dataFromProps = props.getProperty("moonPhase." + num).trim();
                        String[] moonData = dataFromProps.split("\s+");
                        ArrayList<Integer> moonNumbers = new ArrayList<>();
                        for (String data :
                                moonData) {
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    moonNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    moonNumbers.add(Integer.parseInt(data.replaceAll("\\D", "")));
                                }
                            }
                        }
                        moon = moonNumbers.toArray(new Integer[0]);
                    }
                    if (props.containsKey("dayTime." + num)) {
                        daytime = props.getProperty("dayTime." + num).trim().split("\s+");
                    }
                    if (props.containsKey("blocks." + num)) {
                        blocks = props.getProperty("blocks." + num).trim().split("\s+");
                    } else if (props.containsKey("block." + num)) {
                        blocks = props.getProperty("block." + num).trim().split("\s+");
                    }
                    if (props.containsKey("teams." + num)) {
                        String teamData = props.getProperty("teams." + num).trim();
                        List<String> list = new ArrayList<>();
                        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(teamData);
                        while (m.find()) {
                            list.add(m.group(1).replace("\"", ""));
                        }
                        teams = list.toArray(new String[0]);
                    } else if (props.containsKey("team." + num)) {
                        String teamData = props.getProperty("team." + num).trim();
                        List<String> list = new ArrayList<>();
                        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(teamData);
                        while (m.find()) {
                            list.add(m.group(1).replace("\"", ""));
                        }
                        teams = list.toArray(new String[0]);
                    }

                    if (props.containsKey("sizes." + num)) {
                        String dataFromProps = props.getProperty("sizes." + num).trim();
                        String[] sizeData = dataFromProps.split("\s+");
                        ArrayList<Integer> sizeNumbers = new ArrayList<>();
                        for (String data :
                                sizeData) {
                            //check if range
                            data = data.trim();
                            if (!data.replaceAll("\\D", "").isEmpty()) {
                                if (data.contains("-")) {
                                    sizeNumbers.addAll(Arrays.asList(ETFUtils2.getIntRange(data)));
                                } else {
                                    sizeNumbers.add(Integer.parseInt(data.replaceAll("\\D", "")));
                                }
                            }
                        }
                        sizes = sizeNumbers.toArray(new Integer[0]);
                    }

                    //array faster to use
                    //list easier to build
                    String[] namesArray = names.toArray(new String[0]);

                    if (suffixes.length != 0) {
                        allCasesForTexture.add(new ETFTexturePropertyCase(suffixes, weights, biomes, heights, namesArray, professions, collarColours, baby, weather, health, moon, daytime, blocks, teams, /*num,*/ sizes));
                    }
                }
                if (!allCasesForTexture.isEmpty()) {
                    //it all worked now just get the first texture called and everything is set for the next time the texture is called for fast processing
                    OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, allCasesForTexture);
                    //return returnAlreadyConfirmedOptifineTexture(entity,vanillaIdentifier,allCasesForTexture);
                } else {
                    ETFUtils2.logMessage("Ignoring properties file that failed to load any cases @ " + vanillaIdentifier, false);
                    OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
                }
            } else {//properties file is null
                ETFUtils2.logMessage("Ignoring properties file that was null @ " + vanillaIdentifier, false);
                OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
            }
        } catch (Exception e) {
            ETFUtils2.logWarn("Ignoring properties file that caused Exception @ " + vanillaIdentifier + e, false);
            OPTIFINE_PROPERTY_CACHE.put(vanillaIdentifier, null);
        }

        //return null if properties failed to read/load/work or pass high school english
        //return null;
    }


    private static void newTrueRandomTextureFound(Identifier vanillaIdentifier, Identifier variant2PNG) {
        //here 2.png is confirmed to exist and has its directory already applied
        //I'm going to ignore 1.png that will be hardcoded as vanilla or optifine replaced
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
        int totalTextureCount = 2;
        while (resources.getResource(ETFUtils2.replaceIdentifier(variant2PNG, "[0-9]+(?=\\.png)", String.valueOf((totalTextureCount + 1)))).isPresent()) {
            totalTextureCount++;
        }
        //here totalTextureCount == the confirmed last value of the random order
        //System.out.println("total true random was="+totalTextureCount);
        TRUE_RANDOM_COUNT_CACHE.put(vanillaIdentifier, totalTextureCount);

        //make sure to return first check
        //return returnAlreadyConfirmedTrueRandomTexture(entity,vanillaIdentifier,totalTextureCount);
        //can't return null as 2.png confirmed exists
    }

    @Nullable
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedOptifineTexture(T entity, Identifier vanillaIdentifier, boolean isThisAnUpdate) {
        return returnNewAlreadyConfirmedOptifineTexture(entity, vanillaIdentifier, isThisAnUpdate, OPTIFINE_PROPERTY_CACHE.get(vanillaIdentifier));
    }

    @Nullable
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedOptifineTexture(T entity, Identifier vanillaIdentifier, boolean isThisAnUpdate, List<ETFTexturePropertyCase> optifineProperties) {

        int variantNumber = testAndGetVariantNumberFromOptiFineCases(entity, isThisAnUpdate, optifineProperties);

        Identifier variantIdentifier = returnNewAlreadyNumberedRandomTexture(vanillaIdentifier, variantNumber);
        if (variantIdentifier == null) {
            return null;
        }
        //must test these exist
        if (ETF_TEXTURE_CACHE.containsKey(variantIdentifier)) {
            if (ETF_TEXTURE_CACHE.get(variantIdentifier) == null) {
                return null;
            }
            //then we know it exists
            return variantIdentifier;
        }
        Optional<Resource> variantResource = MinecraftClient.getInstance().getResourceManager().getResource(variantIdentifier);
        if (variantResource.isPresent()) {
            return variantIdentifier;
            //it will be added to cache for future checks later
        } else {
            ETF_TEXTURE_CACHE.put(variantIdentifier, null);
        }
        //ETFUtils.logError("texture assign has failed, vanilla texture has been used as fallback");

        return null;
    }

    private static <T extends Entity> int testAndGetVariantNumberFromOptiFineCases(T entity, boolean isThisAnUpdate, List<ETFTexturePropertyCase> optifineProperties) {
        try {
            for (ETFTexturePropertyCase property :
                    optifineProperties) {
                if (property.doesEntityMeetConditionsOfThisCase((LivingEntity) entity, isThisAnUpdate, ENTITY_IS_UPDATABLE)) {
                    return property.getAnEntityVariantSuffixFromThisCase(entity.getUuid());
                }
            }
        } catch (Exception e) {
            return 1;
        }

        //ETFUtils.logError("optifine property checks found no match using vanilla");
        return 1;
    }

    @NotNull
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedTrueRandomTexture(T entity, Identifier vanillaIdentifier) {
        return returnNewAlreadyConfirmedTrueRandomTexture(entity, vanillaIdentifier, TRUE_RANDOM_COUNT_CACHE.getInt(vanillaIdentifier));
    }

    @NotNull
    private static <T extends Entity> Identifier returnNewAlreadyConfirmedTrueRandomTexture(T entity, Identifier vanillaIdentifier, int totalCount) {
        int randomReliable = Math.abs(entity.getUuid().hashCode());
        randomReliable %= totalCount;
        randomReliable++;
        //no need to test as they have already all been confirmed existing by code
        Identifier toReturn = returnNewAlreadyNumberedRandomTexture(vanillaIdentifier, randomReliable);
        return toReturn == null ? vanillaIdentifier : toReturn;
    }

    @Nullable
    private static Identifier returnNewAlreadyNumberedRandomTexture(Identifier vanillaIdentifier, int variantNumber) {

        Identifier identifierWithDirectory;
        if (variantNumber == 1) {
            //return returnTextureOneIdentifier(vanillaIdentifier);
            //should pick the highest directory anyway
            identifierWithDirectory = ETFDirectory.getDirectoryVersionOf(vanillaIdentifier);
        } else {
            identifierWithDirectory = ETFDirectory.getDirectoryVersionOf(ETFUtils2.replaceIdentifier(vanillaIdentifier, ".png", variantNumber + ".png"));
        }
        if (identifierWithDirectory != null) {
            return identifierWithDirectory;
        }
        ETFUtils2.logError("texture assign has failed, and should not have (possibly incorrect OptiFine properties file), vanilla texture has been used as fallback");
        return null;
    }

    // @NotNull
    //private static Identifier returnTextureOneIdentifier(Identifier vanillaIdentifier) {
//        if (ETFDirectory.getDirectoryOf(vanillaIdentifier) == ETFDirectory.VANILLA) {
//            return vanillaIdentifier;
//        }
//
//        if (OPTIFINE_1_HAS_REPLACEMENT.containsKey(vanillaIdentifier)) {
//            if (OPTIFINE_1_HAS_REPLACEMENT.getBoolean(vanillaIdentifier)) {
//                Identifier identifierWithDirectory = ETFDirectory.getDirectoryVersionOf(vanillaIdentifier);
//                if (identifierWithDirectory != null) {
//                    return identifierWithDirectory;
//                }
//                ETFUtils.logError("texture has failed, and should not have, vanilla texture has been used as fallback");
//                return vanillaIdentifier;
//            } else {
//                return vanillaIdentifier;
//            }
//        } else {
//            //find out if we need to replace or return vanilla
//            Identifier identifierWithDirectory = ETFDirectory.getDirectoryVersionOf(vanillaIdentifier);
//            if (identifierWithDirectory != null) {
//                Optional<Resource> possibleReplacement = MinecraftClient.getInstance().getResourceManager().getResource(identifierWithDirectory);
//                if (possibleReplacement.isPresent()) {
//                    OPTIFINE_1_HAS_REPLACEMENT.put(vanillaIdentifier, true);
//                    return identifierWithDirectory;
//                } else {
//                    OPTIFINE_1_HAS_REPLACEMENT.put(vanillaIdentifier, false);
//                    return vanillaIdentifier;
//                }
//            }
//            ETFUtils.logError("texture assign has failed, and should not have, vanilla texture has been used as fallback");
//            return vanillaIdentifier;
//        }
    //}

    @NotNull
    private static ETFTexture getOrCreateETFTexture(Identifier vanillaIdentifier, Identifier variantIdentifier) {
        if (ETF_TEXTURE_CACHE.containsKey(variantIdentifier)) {
            //use cached ETFTexture
            ETFTexture cached = ETF_TEXTURE_CACHE.get(variantIdentifier);
            if (cached != null) {
                return cached;
            } else {
                ETFUtils2.logWarn("getOrCreateETFTexture found a null, this probably should not be happening");
                //texture doesn't exist
                cached = ETF_TEXTURE_CACHE.get(vanillaIdentifier);
                if (cached != null) {
                    return cached;
                }
            }
        } else {
            //create new ETFTexture and cache it
            ETFTexture foundTexture = new ETFTexture(variantIdentifier);
            ETF_TEXTURE_CACHE.put(variantIdentifier, foundTexture);
            return foundTexture;
        }
        ETFUtils2.logError("getOrCreateETFTexture and should not have");
        return ETF_ERROR_TEXTURE;
    }


    public enum TextureSource{
        ENTITY,
        BLOCK_ENTITY,
        ENTITY_FEATURE,
        GENERIC_DEBUG
    }
}

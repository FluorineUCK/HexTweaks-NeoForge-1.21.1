package net.walksanator.hextweaks.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.walksanator.hextweaks.HexTweaks;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Property-gated client resource and Patchouli integration checks. */
public final class HexTweaksClientProbe {
    private static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("hexcasting", "thehexbook");
    private static final ResourceLocation RGB_PIGMENT_ID =
            ResourceLocation.fromNamespaceAndPath("hextweaks", "rgb_pigment");
    private static final int VALIDATION_TICK = 160;
    private static final int WORLD_TIMEOUT_TICK = 1_200;

    private static int ticks;
    private static boolean registered;
    private static boolean baseChecksFinished;
    private static boolean finished;
    private static final List<String> FAILURES = new ArrayList<>();

    private HexTweaksClientProbe() {
    }

    public static void register() {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(HexTweaksClientProbe::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (finished) {
            return;
        }
        ticks++;

        if (!baseChecksFinished && ticks >= VALIDATION_TICK) {
            baseChecksFinished = true;
            runCheck("client_translations", HexTweaksClientProbe::checkTranslations, FAILURES);
            runCheck("client_item_model", HexTweaksClientProbe::checkItemModel, FAILURES);
        }

        if (baseChecksFinished && Minecraft.getInstance().level != null) {
            runCheck("patchouli_book", HexTweaksClientProbe::checkPatchouliBook, FAILURES);
            finish();
        } else if (ticks >= WORLD_TIMEOUT_TICK) {
            FAILURES.add("client_world");
            HexTweaks.LOGGER.error(
                    "[HEXTWEAKS-PROBE] client_world=FAIL no integrated world after {} ticks",
                    ticks
            );
            finish();
        }
    }

    private static void finish() {
        finished = true;
        if (FAILURES.isEmpty()) {
            HexTweaks.LOGGER.info(
                    "[HEXTWEAKS-PROBE] client_aggregate=PASS hexcasting=pre-39 translations=PASS model=PASS patchouli=PASS"
            );
        } else {
            HexTweaks.LOGGER.error(
                    "[HEXTWEAKS-PROBE] client_aggregate=FAIL failure_count={} failures={}",
                    FAILURES.size(),
                    String.join(",", FAILURES)
            );
        }

        if (Boolean.getBoolean("hextweaks.probe.exitAfterClientStartup")) {
            scheduleHardExit(FAILURES.isEmpty() ? 0 : 1);
            Minecraft.getInstance().stop();
        }
    }

    private static void scheduleHardExit(int exitCode) {
        Thread hardStop = new Thread(() -> {
            try {
                Thread.sleep(15_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(exitCode);
        }, "hextweaks-client-probe-hard-stop");
        hardStop.setDaemon(true);
        hardStop.start();
    }

    private static void runCheck(
            String name,
            ProbeCheck check,
            List<String> failures
    ) {
        try {
            String details = check.run();
            HexTweaks.LOGGER.info("[HEXTWEAKS-PROBE] {}=PASS {}", name, details);
        } catch (Throwable throwable) {
            failures.add(name);
            HexTweaks.LOGGER.error("[HEXTWEAKS-PROBE] {}=FAIL", name, throwable);
        }
    }

    private static String checkTranslations() throws Exception {
        JsonObject english = readLang("en_us");
        JsonObject chinese = readLang("zh_cn");
        Set<String> englishKeys = english.keySet();
        Set<String> chineseKeys = chinese.keySet();
        check(englishKeys.equals(chineseKeys), () -> {
            Set<String> onlyEnglish = new LinkedHashSet<>(englishKeys);
            onlyEnglish.removeAll(chineseKeys);
            Set<String> onlyChinese = new LinkedHashSet<>(chineseKeys);
            onlyChinese.removeAll(englishKeys);
            return "Language key mismatch: only_en=" + onlyEnglish + ", only_zh=" + onlyChinese;
        });

        List<String> blank = englishKeys.stream()
                .filter(key -> isBlank(english.get(key)) || isBlank(chinese.get(key)))
                .toList();
        check(blank.isEmpty(), () -> "Blank translations " + blank);

        List<String> missing = englishKeys.stream()
                .filter(key -> !I18n.exists(key))
                .toList();
        check(missing.isEmpty(), () -> "Missing active translations " + missing);

        Item pigment = BuiltInRegistries.ITEM.get(RGB_PIGMENT_ID);
        check(BuiltInRegistries.ITEM.getKey(pigment).equals(RGB_PIGMENT_ID),
                () -> "RGB pigment is not registered");
        check(I18n.exists(pigment.getDescriptionId()),
                () -> "Missing item key " + pigment.getDescriptionId());
        if (ModList.get().isLoaded("computercraft")) {
            check(HexTweaksForgeClient.isTurtleModellerRegistered(),
                    () -> "ComputerCraft turtle modeller event was not registered");
        }

        return "locale=" + Minecraft.getInstance().getLanguageManager().getSelected()
                + " keys=" + englishKeys.size()
                + " en_zh_parity=PASS"
                + (ModList.get().isLoaded("computercraft")
                ? " turtle_modeller=PASS"
                : " turtle_modeller=NOT_LOADED");
    }

    private static String checkItemModel() {
        Minecraft client = Minecraft.getInstance();
        Item pigment = BuiltInRegistries.ITEM.get(RGB_PIGMENT_ID);
        ItemStack stack = new ItemStack(pigment);
        var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
        check(model != client.getModelManager().getMissingModel(),
                () -> "Missing baked model for " + RGB_PIGMENT_ID);
        return "resolved=" + RGB_PIGMENT_ID;
    }

    private static String checkPatchouliBook() {
        Book book = BookRegistry.INSTANCE.books.get(BOOK_ID);
        check(book != null, () -> "Book not loaded: " + BOOK_ID);

        BookContents contents = book.getContents();
        check(contents != null, () -> "Book contents are null: " + BOOK_ID);
        if (contents.isErrored()) {
            IllegalStateException error = new IllegalStateException(
                    "Patchouli failed to build " + BOOK_ID,
                    contents.getException()
            );
            throw error;
        }

        HexTweaks.LOGGER.info(
                "[HEXTWEAKS-PROBE] patchouli_inventory categories={} addon_entries={}",
                contents.categories.keySet(),
                contents.entries.keySet().stream()
                        .filter(id -> id.getNamespace().equals("hextweaks")
                                || id.getPath().contains("hextweaks")
                                || id.getPath().contains("mindflay")
                                || id.getPath().contains("infusion")
                                || id.getPath().contains("slipways")
                                || id.getPath().contains("computercraft"))
                        .toList()
        );

        Set<ResourceLocation> expectedCategories = Set.of(
                bookId("patterns/grand"),
                bookId("patterns/grand/mindflay_rituals")
        );
        Set<ResourceLocation> expectedEntries = new LinkedHashSet<>(List.of(
                bookId("patterns/hextweaks_pe"),
                bookId("patterns/hextweaks_utility"),
                bookId("patterns/grand/explode"),
                bookId("patterns/grand/fireball"),
                bookId("patterns/grand/mindflay"),
                bookId("patterns/grand/mindflay_rituals/restock"),
                bookId("patterns/great_spells/infusion")
        ));
        if (ModList.get().isLoaded("computercraft")) {
            expectedEntries.add(bookId("interop/computercraft"));
        }
        if (ModList.get().isLoaded("hexal")) {
            expectedEntries.add(bookId("patterns/grand/mindflay_rituals/slipways"));
        }
        Set<ResourceLocation> missingCategories = new LinkedHashSet<>(expectedCategories);
        missingCategories.removeAll(contents.categories.keySet());
        check(missingCategories.isEmpty(), () -> "Missing categories " + missingCategories);

        Set<ResourceLocation> missingEntries = new LinkedHashSet<>(expectedEntries);
        missingEntries.removeAll(contents.entries.keySet());
        check(missingEntries.isEmpty(), () -> "Missing entries " + missingEntries);

        List<ResourceLocation> emptyEntries = expectedEntries.stream()
                .filter(id -> contents.entries.get(id).getPages().isEmpty())
                .toList();
        check(emptyEntries.isEmpty(), () -> "Entries without pages " + emptyEntries);

        return "book=" + BOOK_ID
                + " categories=" + expectedCategories.size()
                + " entries=" + expectedEntries.size()
                + " errored=false";
    }

    private static JsonObject readLang(String locale) throws Exception {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "hextweaks",
                "lang/" + locale + ".json"
        );
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        check(resource.isPresent(), () -> "Missing resource " + id);
        try (InputStream stream = resource.orElseThrow().open()) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static boolean isBlank(JsonElement value) {
        return value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank();
    }

    private static ResourceLocation bookId(String path) {
        return ResourceLocation.fromNamespaceAndPath("hexcasting", path);
    }

    private static void check(boolean condition, Message message) {
        if (!condition) {
            throw new IllegalStateException(message.get());
        }
    }

    @FunctionalInterface
    private interface ProbeCheck {
        String run() throws Exception;
    }

    @FunctionalInterface
    private interface Message {
        String get();
    }
}

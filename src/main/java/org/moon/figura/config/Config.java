package org.moon.figura.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ClientRegistry;
import org.moon.figura.FiguraMod;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.FiguraText;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

public enum Config {

    /**
     * Config Here!!!1!
     * (order matters)
     */

    Nameplate,
    SELF_NAMEPLATE(false),
    PREVIEW_NAMEPLATE(true),
    CHAT_NAMEPLATE(2, 3) {{
        String path = "config.nameplate_level";
        this.enumList = List.of(
                new FiguraText(path + ".1"),
                new FiguraText(path + ".2"),
                new FiguraText(path + ".3")
        );
    }},
    ENTITY_NAMEPLATE(2, 3) {{
        String path = "config.nameplate_level";
        this.enumList = List.of(
                new FiguraText(path + ".1"),
                new FiguraText(path + ".2"),
                new FiguraText(path + ".3")
        );
    }},
    LIST_NAMEPLATE(2, 3) {{
        String path = "config.nameplate_level";
        this.enumList = List.of(
                new FiguraText(path + ".1"),
                new FiguraText(path + ".2"),
                new FiguraText(path + ".3")
        );
    }},

    Script,
    LOG_LOCATION(0, 2),

    ActionWheel,
    //ACTION_WHEEL_BUTTON("key.keyboard.b"),

    Paperdoll,
    HAS_PAPERDOLL(false),
    PAPERDOLL_ALWAYS_ON(false),
    FIRST_PERSON_PAPERDOLL(true),
    PAPERDOLL_SCALE(1f, InputType.FLOAT),

    Misc,
    BUTTON_LOCATION(0, 5),
    EASTER_EGGS(true),

    Dev {{this.name = new FiguraText("config.dev").withStyle(ChatFormatting.RED);}},
    RELOAD_BUTTON("key.keyboard.unknown"),
    PANIC_BUTTON("key.keyboard.unknown"),
    //LOG_PINGS(0, 3);
    RENDER_DEBUG_PARTS_PIVOT(2, 3) {{
        String tooltip = "config.render_debug_parts_pivot.tooltip";
        this.tooltip = new FiguraText(tooltip,
                new FiguraText(tooltip + ".cubes").setStyle(ColorUtils.Colors.FRAN_PINK.style),
                new FiguraText(tooltip + ".groups").setStyle(ColorUtils.Colors.MAYA_BLUE.style));
    }},
    LOG_OTHERS(false),
    AVATARS_PATH("", InputType.FOLDER_PATH);


    /**
     * Static Properties
     */


    //mod config version
    //only change this if you rename old configs
    public static final int CONFIG_VERSION = 1;

    //config update hashmap; <version number, <actual config, old config name>>
    public static final HashMap<Integer, HashMap<Config, String>> CONFIG_UPDATES = new HashMap<>();


    /**
     * do not edit below this line :p
     */


    //values
    public Object value;
    public Object configValue;
    public final Object defaultValue;

    //metadata
    public Component name;
    public Component tooltip;
    public final ConfigType type;

    //special properties
    public List<Component> enumList;
    public ConfigKeyBind keyBind;
    public final InputType inputType;

    //type constructors
    Config() {
        this(ConfigType.CATEGORY, null, null, null, null);
    }
    Config(boolean defaultValue) {
        this(ConfigType.BOOLEAN, defaultValue, null, null, null);
    }
    Config(int defaultValue, Integer length) {
        this(ConfigType.ENUM, defaultValue, length, null, null);
    }
    Config(Object defaultValue, InputType inputType) {
        this(ConfigType.INPUT, defaultValue, null, null, inputType);
    }
    Config(String key) {
        this(ConfigType.KEYBIND, key, null, null, null);
        this.keyBind = new ConfigKeyBind(this.name.getString(), InputConstants.getKey(key), this);
    }

    //global constructor
    Config(ConfigType type, Object value, Integer length, ConfigKeyBind keyBind, InputType inputType) {
        //set values
        this.type = type;
        this.value = value;
        this.defaultValue = value;
        this.configValue = value;
        this.keyBind = keyBind;
        this.inputType = inputType;

        //generate names
        String name = "config." + this.name().toLowerCase();
        this.name = new FiguraText(name);
        this.tooltip = new FiguraText(name + ".tooltip");

        //generate enum list
        if (length != null) {
            ArrayList<Component> enumList = new ArrayList<>();
            for (int i = 1; i <= length; i++)
                enumList.add(new FiguraText(name + "." + i));
            this.enumList = enumList;
        }
    }

    public void setValue(String text) {
        boolean change = value.equals(configValue);

        try {
            if (value instanceof String)
                value = text;
            else if (value instanceof Boolean)
                value = Boolean.valueOf(text);
            else if (value instanceof Integer)
                value = Integer.valueOf(text);
            else if (value instanceof Float)
                value = Float.valueOf(text);
            else if (value instanceof Long)
                value = Long.valueOf(text);
            else if (value instanceof Double)
                value = Double.valueOf(text);
            else if (value instanceof Byte)
                value = Byte.valueOf(text);
            else if (value instanceof Short)
                value = Short.valueOf(text);

            if (enumList != null) {
                int length = enumList.size();
                value = ((Integer.parseInt(text) % length) + length) % length;
            }
        } catch (Exception e) {
            value = defaultValue;
        }

        configValue = value;
        if (change) runOnChange();
    }

    public void runOnChange() {}

    public enum ConfigType {
        CATEGORY,
        BOOLEAN,
        ENUM,
        INPUT,
        KEYBIND
    }

    public enum InputType {
        ANY(s -> true),
        INT(s -> s.matches("^[\\-+]?[0-9]*$")),
        FLOAT(s -> s.matches("[\\-+]?[0-9]*(\\.[0-9]+)?") || s.endsWith(".") || s.isEmpty()),
        HEX_COLOR(s -> s.matches("^[#]?[0-9A-Fa-f]{0,6}$")),
        FOLDER_PATH(s -> {
            if (!s.isBlank()) {
                try {
                    return Path.of(s.trim()).toFile().isDirectory();
                } catch (Exception ignored) {
                    return false;
                }
            }

            return true;
        });

        public final Predicate<String> validator;
        public final Component hint;
        InputType(Predicate<String> predicate) {
            this.validator = predicate;
            this.hint = new FiguraText("config.input." + this.name().toLowerCase());
        }
    }

    public static class ConfigKeyBind extends KeyMapping {
        private final Config config;

        public ConfigKeyBind(String translationKey, InputConstants.Key key, Config config) {
            super(translationKey, key.getType(), key.getValue(), FiguraMod.MOD_ID);
            this.config = config;
            ClientRegistry.registerKeyBinding(this);
        }

        @Override
        public void setKey(InputConstants.Key boundKey) {
            super.setKey(boundKey);

            config.setValue(this.saveString());
            ConfigManager.saveConfig();

            Options options = Minecraft.getInstance().options;
            if (options != null) options.save();
            KeyMapping.resetMapping();
        }
    }
}
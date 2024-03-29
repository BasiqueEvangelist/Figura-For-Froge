package org.moon.figura.lua.api;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatars.Avatar;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.api.entity.EntityWrapper;
import org.moon.figura.lua.api.world.ItemStackWrapper;
import org.moon.figura.lua.docs.LuaFunctionOverload;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.utils.LuaUtils;
import org.moon.figura.utils.TextUtils;
import org.terasology.jnlua.LuaRuntimeException;

@LuaWhitelist
@LuaTypeDoc(
        name = "HostAPI",
        description = "host"
)
public class HostAPI {

    private final Avatar owner;
    private final Minecraft minecraft;

    public HostAPI(Avatar owner) {
        this.owner = owner;
        this.minecraft = Minecraft.getInstance();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = HostAPI.class,
                    argumentNames = "host"
            ),
            description = "host.is_host"
    )
    public static boolean isHost(@LuaNotNil HostAPI api) {
        return api.owner.owner.compareTo(FiguraMod.getLocalPlayerUUID()) == 0;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = HostAPI.class,
                    argumentNames = "host"
            ),
            description = "host.get_targeted_entity"
    )
    public static EntityWrapper<?> getTargetedEntity(@LuaNotNil HostAPI api) {
        if (!isHost(api)) return null;
        Entity entity = api.minecraft.crosshairPickEntity;

        if (entity != null && Minecraft.getInstance().player != null && !entity.isInvisibleTo(Minecraft.getInstance().player))
            return EntityWrapper.fromEntity(entity);

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {HostAPI.class, FiguraVec3.class},
                            argumentNames = {"host", "timesData"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {HostAPI.class, Integer.class, Integer.class, Integer.class},
                            argumentNames = {"host", "fadeInTime", "stayTime", "fadeOutTime"}
                    )
            },
            description = "host.set_title_times"
    )
    public static void setTitleTimes(@LuaNotNil HostAPI api, Object x, Double y, Double z) {
        if (!isHost(api)) return;
        FiguraVec3 times = LuaUtils.parseVec3("setTitleTimes", x, y, z);
        api.minecraft.gui.setTimes((int) times.x, (int) times.y, (int) times.z);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = HostAPI.class,
                    argumentNames = "host"
            ),
            description = "host.clear_title"
    )
    public static void clearTitle(@LuaNotNil HostAPI api) {
        if (!isHost(api)) return;
        api.minecraft.gui.clear();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {HostAPI.class, String.class},
                    argumentNames = {"host", "text"}
            ),
            description = "host.set_title"
    )
    public static void setTitle(@LuaNotNil HostAPI api, @LuaNotNil String text) {
        if (!isHost(api)) return;
        api.minecraft.gui.setTitle(TextUtils.tryParseJson(text));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {HostAPI.class, String.class},
                    argumentNames = {"host", "text"}
            ),
            description = "host.set_subtitle"
    )
    public static void setSubtitle(@LuaNotNil HostAPI api, @LuaNotNil String text) {
        if (!isHost(api)) return;
        api.minecraft.gui.setSubtitle(TextUtils.tryParseJson(text));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {HostAPI.class, String.class},
                            argumentNames = {"host", "text"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {HostAPI.class, String.class, Boolean.class},
                            argumentNames = {"host", "text", "animated"}
                    )
            },
            description = "host.set_actionbar"
    )
    public static void setActionbar(@LuaNotNil HostAPI api, @LuaNotNil String text, Boolean animated) {
        if (!isHost(api)) return;
        if (animated == null) animated = false;
        api.minecraft.gui.setOverlayMessage(TextUtils.tryParseJson(text), animated);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {HostAPI.class, String.class},
                    argumentNames = {"host", "text"}
            ),
            description = "host.send_chat_message"
    )
    public static void sendChatMessage(@LuaNotNil HostAPI api, @LuaNotNil String text) {
        if (!isHost(api)) return;
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.chat(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {HostAPI.class, Boolean.class},
                    argumentNames = {"host", "offhand"}
            ),
            description = "host.swing_arm"
    )
    public static void swingArm(@LuaNotNil HostAPI api, Boolean offhand) {
        if (!isHost(api)) return;
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.swing(offhand == null || !offhand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = HostAPI.class,
                    argumentNames = "host"
            ),
            description = "host.is_first_person"
    )
    public static Boolean isFirstPerson(@LuaNotNil HostAPI api) {
        if (!isHost(api)) return false;
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = HostAPI.class,
                    argumentNames = "host"
            ),
            description = "host.is_camera_backwards"
    )
    public static Boolean isCameraBackwards(@LuaNotNil HostAPI api) {
        if (!isHost(api)) return false;
        return Minecraft.getInstance().options.getCameraType().isMirrored();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {HostAPI.class, String.class},
                    argumentNames = {"host", "slot"}
            ),
            description = "host.get_slot"
    )
    public static ItemStackWrapper getSlot(@LuaNotNil HostAPI api, @LuaNotNil String slot) {
        if (!isHost(api)) return null;
        try {
            Entity e = EntityWrapper.getEntity(api.owner.luaState.entity);
            Integer index = SlotArgument.slot().parse(new StringReader(slot));
            return ItemStackWrapper.verify(e.getSlot(index).get());
        } catch (Exception e) {
            throw new LuaRuntimeException("Unable to get slot \"" + slot + "\"");
        }
    }

    @LuaWhitelist
    public static void setBadge(@LuaNotNil HostAPI api, @LuaNotNil Integer index, @LuaNotNil Boolean value) {
        if (!isHost(api)) return;
        if (!FiguraMod.DEBUG_MODE)
            throw new LuaRuntimeException("Congrats, you found this debug easter egg!");
        api.owner.badges.set(index, value);
    }

    @Override
    public String toString() {
        return "HostAPI";
    }
}

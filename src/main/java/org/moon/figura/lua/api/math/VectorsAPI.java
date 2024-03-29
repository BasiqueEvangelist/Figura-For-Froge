package org.moon.figura.lua.api.math;

import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.docs.LuaFunctionOverload;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.vector.*;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.LuaUtils;
import org.terasology.jnlua.LuaRuntimeException;

@LuaWhitelist
@LuaTypeDoc(
        name = "VectorsAPI",
        description = "vectors"
)
public class VectorsAPI {

    public static final VectorsAPI INSTANCE = new VectorsAPI();

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class},
                            argumentNames = {"x", "y"},
                            returnType = FiguraVec2.class
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"},
                            returnType = FiguraVec3.class
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z", "w"},
                            returnType = FiguraVec4.class
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z", "w", "t"},
                            returnType = FiguraVec5.class
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z", "w", "t", "h"},
                            returnType = FiguraVec6.class
                    )
            },
            description = "vectors.vec"
    )
    public static Object vec(Double x, Double y, Double z, Double w, Double t, Double h) {
        if (h != null)
            return vec6(x, y, z, w, t, h);
        if (t != null)
            return vec5(x, y, z, w, t);
        if (w != null)
            return vec4(x, y, z, w);
        if (z != null)
            return vec3(x, y, z);
        if (y != null)
            return vec2(x, y);
        throw new LuaRuntimeException("Invalid arguments to vec(), needs at least 2 numbers!");
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {Double.class, Double.class},
                    argumentNames = {"x", "y"}
            ),
            description = "vectors.vec2"
    )
    public static FiguraVec2 vec2(Double x, Double y) {
        if (x == null) x = 0d;
        if (y == null) y = 0d;
        return FiguraVec2.of(x, y);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {Double.class, Double.class, Double.class},
                    argumentNames = {"x", "y", "z"}
            ),
            description = "vectors.vec3"
    )
    public static FiguraVec3 vec3(Double x, Double y, Double z) {
        if (x == null) x = 0d;
        if (y == null) y = 0d;
        if (z == null) z = 0d;
        return FiguraVec3.of(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {Double.class, Double.class, Double.class, Double.class},
                    argumentNames = {"x", "y", "z", "w"}
            ),
            description = "vectors.vec4"
    )
    public static FiguraVec4 vec4(Double x, Double y, Double z, Double w) {
        if (x == null) x = 0d;
        if (y == null) y = 0d;
        if (z == null) z = 0d;
        if (w == null) w = 0d;
        return FiguraVec4.of(x, y, z, w);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class},
                    argumentNames = {"x", "y", "z", "w", "t"}
            ),
            description = "vectors.vec5"
    )
    public static FiguraVec5 vec5(Double x, Double y, Double z, Double w, Double t) {
        if (x == null) x = 0d;
        if (y == null) y = 0d;
        if (z == null) z = 0d;
        if (w == null) w = 0d;
        if (t == null) t = 0d;
        return FiguraVec5.of(x, y, z, w, t);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                    argumentNames = {"x", "y", "z", "w", "t", "h"}
            ),
            description = "vectors.vec6"
    )
    public static FiguraVec6 vec6(Double x, Double y, Double z, Double w, Double t, Double h) {
        if (x == null) x = 0d;
        if (y == null) y = 0d;
        if (z == null) z = 0d;
        if (w == null) w = 0d;
        if (t == null) t = 0d;
        if (h == null) h = 0d;
        return FiguraVec6.of(x, y, z, w, t, h);
    }

    // -- colors -- //

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rgb"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            description = "vectors.rgb_to_int"
    )
    public static Integer rgbToINT(Object r, Double g, Double b) {
        FiguraVec3 rgb = LuaUtils.parseVec3("rgbToINT", r, g, b);
        return ColorUtils.rgbToInt(rgb);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Integer.class,
                    argumentNames = "color"
            ),
            description = "vectors.int_to_rgb"
    )
    public static FiguraVec3 intToRGB(@LuaNotNil Integer color) {
        return ColorUtils.intToRGB(color);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = String.class,
                    argumentNames = "hex"
            ),
            description = "vectors.hex_to_rgb"
    )
    public static FiguraVec3 hexToRGB(@LuaNotNil String hex) {
        return ColorUtils.hexStringToRGB(hex);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "hsv"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"h", "s", "v"}
                    )
            },
            description = "vectors.hsv_to_rgb"
    )
    public static FiguraVec3 hsvToRGB(Object h, Double s, Double v) {
        FiguraVec3 hsv = LuaUtils.parseVec3("hsvToRGB", h, s, v);
        return ColorUtils.hsvToRGB(hsv);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "rgb"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            description = "vectors.rgb_to_hsv"
    )
    public static FiguraVec3 rgbToHSV(Object r, Double g, Double b) {
        FiguraVec3 rgb = LuaUtils.parseVec3("rgbToHSV", r, g, b);
        return ColorUtils.rgbToHSV(rgb);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "speed"
            ),
            description = "vectors.rainbow"
    )
    public static FiguraVec3 rainbow(Double speed) {
        if (speed == null) speed = 1d;
        return ColorUtils.intToRGB(ColorUtils.rainbow(speed));
    }

    @Override
    public String toString() {
        return "VectorsAPI";
    }
}

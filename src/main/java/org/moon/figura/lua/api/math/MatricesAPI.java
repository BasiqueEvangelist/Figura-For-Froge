package org.moon.figura.lua.api.math;

import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.docs.LuaFunctionOverload;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.math.matrix.FiguraMat2;
import org.moon.figura.math.matrix.FiguraMat3;
import org.moon.figura.math.matrix.FiguraMat4;
import org.moon.figura.math.vector.FiguraVec2;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.math.vector.FiguraVec4;
import org.moon.figura.utils.LuaUtils;
import org.terasology.jnlua.LuaRuntimeException;

@LuaWhitelist
@LuaTypeDoc(
        name = "MatricesAPI",
        description = "matrices"
)
public class MatricesAPI {

    public static final MatricesAPI INSTANCE = new MatricesAPI();

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraVec2.class, FiguraVec2.class},
                            argumentNames = {"col1", "col2"}
                    )
            },
            description = "matrices.mat2"
    )
    public static FiguraMat2 mat2(FiguraVec2 col1, FiguraVec2 col2) {
        if (col1 == null && col2 == null)
            return FiguraMat2.of();
        if (col1 == null || col2 == null)
            throw new LuaRuntimeException("Invalid arguments to mat2(), needs 0 or 2 arguments!");
        return FiguraMat2.of(
                col1.x, col1.y,
                col2.x, col2.y
        );
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraVec3.class, FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"col1", "col2", "col3"}
                    )
            },
            description = "matrices.mat3"
    )
    public static FiguraMat3 mat3(FiguraVec3 col1, FiguraVec3 col2, FiguraVec3 col3) {
        if (col1 == null && col2 == null && col3 == null)
            return FiguraMat3.of();
        if (col1 == null || col2 == null || col3 == null)
            throw new LuaRuntimeException("Invalid arguments to mat3(), needs 0 or 3 arguments!");
        return FiguraMat3.of(
                col1.x, col1.y, col1.z,
                col2.x, col2.y, col2.z,
                col3.x, col3.y, col3.z
        );
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraVec4.class, FiguraVec4.class, FiguraVec4.class, FiguraVec4.class},
                            argumentNames = {"col1", "col2", "col3", "col4"}
                    )
            },
            description = "matrices.mat4"
    )
    public static FiguraMat4 mat4(FiguraVec4 col1, FiguraVec4 col2, FiguraVec4 col3, FiguraVec4 col4) {
        if (col1 == null && col2 == null && col3 == null && col4 == null)
            return FiguraMat4.of();
        if (col1 == null || col2 == null || col3 == null || col4 == null)
            throw new LuaRuntimeException("Invalid arguments to mat4(), needs 0 or 4 arguments!");
        return FiguraMat4.of(
                col1.x, col1.y, col1.z, col1.w,
                col2.x, col2.y, col2.z, col2.w,
                col3.x, col3.y, col3.z, col3.w,
                col4.x, col4.y, col4.z, col4.w
        );
    }

    //-- ROTATION MATRICES --//
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.rotation2"
    )
    public static FiguraMat2 rotation2(@LuaNotNil Double degrees) {
        return FiguraMat2.createRotationMatrix(degrees);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            description = "matrices.rotation3"
    )
    public static FiguraMat3 rotation3(Object x, Double y, Double z) {
        FiguraVec3 angles = LuaUtils.parseVec3("rotation3", x, y, z);
        FiguraMat3 result = FiguraMat3.createZYXRotationMatrix(angles.x, angles.y, angles.z);
        angles.free();
        return result;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.x_rotation3"
    )
    public static FiguraMat3 xRotation3(@LuaNotNil Double degrees) {
        return FiguraMat3.createXRotationMatrix(degrees);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.y_rotation3"
    )
    public static FiguraMat3 yRotation3(@LuaNotNil Double degrees) {
        return FiguraMat3.createYRotationMatrix(degrees);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.z_rotation3"
    )
    public static FiguraMat3 zRotation3(@LuaNotNil Double degrees) {
        return FiguraMat3.createZRotationMatrix(degrees);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            description = "matrices.rotation4"
    )
    public static FiguraMat4 rotation4(Object x, Double y, Double z) {
        FiguraVec3 angles = LuaUtils.parseVec3("rotation4", x, y, z);
        FiguraMat4 result = FiguraMat4.createZYXRotationMatrix(angles.x, angles.y, angles.z);
        angles.free();
        return result;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.x_rotation4"
    )
    public static FiguraMat4 xRotation4(@LuaNotNil Double degrees) {
        return FiguraMat4.createXRotationMatrix(degrees);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.y_rotation4"
    )
    public static FiguraMat4 yRotation4(@LuaNotNil Double degrees) {
        return FiguraMat4.createYRotationMatrix(degrees);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = Double.class,
                    argumentNames = "angle"
            ),
            description = "matrices.z_rotation4"
    )
    public static FiguraMat4 zRotation4(@LuaNotNil Double degrees) {
        return FiguraMat4.createZRotationMatrix(degrees);
    }

    //-- SCALE MATRICES --//
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec2.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class},
                            argumentNames = {"x", "y"}
                    )
            },
            description = "matrices.scale2"
    )
    public static FiguraMat2 scale2(Object x, Double y) {
        FiguraVec2 vec = LuaUtils.parseVec2("scale2", x, y, 1, 1);
        FiguraMat2 result = FiguraMat2.createScaleMatrix(vec.x, vec.y);
        vec.free();
        return result;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            description = "matrices.scale3"
    )
    public static FiguraMat3 scale3(Object x, Double y, Double z) {
        FiguraVec3 scale = LuaUtils.parseVec3("scale3", x, y, z, 1, 1, 1);
        FiguraMat3 result = FiguraMat3.createScaleMatrix(scale.x, scale.y, scale.z);
        scale.free();
        return result;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            description = "matrices.scale4"
    )
    public static FiguraMat4 scale4(Object x, Double y, Double z) {
        FiguraVec3 scale = LuaUtils.parseVec3("scale4", x, y, z, 1, 1, 1);
        FiguraMat4 result = FiguraMat4.createScaleMatrix(scale.x, scale.y, scale.z);
        scale.free();
        return result;
    }

    //-- TRANSLATION MATRICES --//
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec2.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class},
                            argumentNames = {"x", "y"}
                    )
            },
            description = "matrices.translate3"
    )
    public static FiguraMat3 translate3(Object x, Double y) {
        FiguraVec2 offset = LuaUtils.parseVec2("translate3", x, y);
        FiguraMat3 result = FiguraMat3.createTranslationMatrix(offset.x, offset.y);
        offset.free();
        return result;
    }
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "vec"
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"x", "y", "z"}
                    )
            },
            description = "matrices.translate4"
    )
    public static FiguraMat4 translate4(Object x, Double y, Double z) {
        FiguraVec3 offset = LuaUtils.parseVec3("translate4", x, y, z);
        FiguraMat4 result = FiguraMat4.createTranslationMatrix(offset.x, offset.y, offset.z);
        offset.free();
        return result;
    }

    @Override
    public String toString() {
        return "MatricesAPI";
    }
}

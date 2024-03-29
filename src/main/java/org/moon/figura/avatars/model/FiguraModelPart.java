package org.moon.figura.avatars.model;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.moon.figura.avatars.model.rendering.FiguraImmediateBuffer;
import org.moon.figura.avatars.model.rendering.ImmediateAvatarRenderer;
import org.moon.figura.avatars.model.rendering.texture.FiguraTextureSet;
import org.moon.figura.avatars.vanilla.VanillaPartOffsetManager;
import org.moon.figura.lua.LuaNotNil;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.docs.LuaFieldDoc;
import org.moon.figura.lua.docs.LuaFunctionOverload;
import org.moon.figura.lua.docs.LuaMethodDoc;
import org.moon.figura.lua.docs.LuaTypeDoc;
import org.moon.figura.lua.types.LuaTable;
import org.moon.figura.math.matrix.FiguraMat3;
import org.moon.figura.math.matrix.FiguraMat4;
import org.moon.figura.math.vector.FiguraVec2;
import org.moon.figura.math.vector.FiguraVec3;
import org.moon.figura.math.vector.FiguraVec4;
import org.moon.figura.mixin.render.elytra.ElytraModelAccessor;
import org.moon.figura.utils.LuaUtils;
import org.terasology.jnlua.LuaRuntimeException;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "ModelPart",
        description = "model_part"
)
public class FiguraModelPart {

    @LuaWhitelist
    @LuaFieldDoc(description = "model_part.name")
    public final String name;
    public FiguraModelPart parent;

    public final PartCustomization customization;
    public ParentType parentType = ParentType.None;
    public final int index;

    private final Map<String, FiguraModelPart> childCache = new HashMap<>();
    public final List<FiguraModelPart> children;

    private List<Integer> facesByTexture;

    private int textureWidth, textureHeight; //If the part has multiple textures, then these are -1.

    public void pushVerticesImmediate(ImmediateAvatarRenderer avatarRenderer, int[] remainingComplexity) {
        for (int i = 0; i < facesByTexture.size(); i++) {
            if (remainingComplexity[0] <= 0)
                return;
            remainingComplexity[0] -= facesByTexture.get(i);
            avatarRenderer.pushFaces(i, facesByTexture.get(i) + Math.min(remainingComplexity[0], 0), remainingComplexity);
        }
    }

    public void applyVanillaTransforms(EntityModel<?> vanillaModel) {
        if (!parentType.vanilla) return;
        if (vanillaModel instanceof HumanoidModel<?> humanoid) {
            applyVanillaTransform(vanillaModel, parentType, switch (parentType) {
                case Head -> humanoid.head;
                case Body -> humanoid.body;
                case LeftArm -> humanoid.leftArm;
                case RightArm -> humanoid.rightArm;
                case LeftLeg -> humanoid.leftLeg;
                case RightLeg -> humanoid.rightLeg;
                default -> null;
            });
        }
        else if (vanillaModel instanceof ElytraModel<?> elytra) {
            applyVanillaTransform(vanillaModel, parentType, switch (parentType) {
                case LeftElytra -> ((ElytraModelAccessor) elytra).getLeftWing();
                case RightElytra -> ((ElytraModelAccessor) elytra).getRightWing();
                default -> null;
            });
        }
    }

    @Override
    public String toString() {
        return name + " (ModelPart)";
    }

    public void applyVanillaTransform(EntityModel<?> vanillaModel, ParentType parentType, ModelPart part) {
        if (part == null)
            return;

        FiguraVec3 defaultPivot = VanillaPartOffsetManager.getVanillaOffset(vanillaModel, parentType);
        defaultPivot.subtract(part.x, part.y, part.z);
        defaultPivot.multiply(1, 1, -1);

        customization.setBonusPivot(defaultPivot);
        customization.setBonusPos(defaultPivot);

        //customization.setBonusPivot(pivot);
        customization.setBonusRot(Math.toDegrees(-part.xRot), Math.toDegrees(-part.yRot), Math.toDegrees(part.zRot));

        defaultPivot.free();
    }

    public void resetVanillaTransforms() {
        if (parentType.vanilla) {
            customization.setBonusPivot(0, 0, 0);
            customization.setBonusPos(0, 0, 0);
            customization.setBonusRot(0, 0, 0);
        }
    }

    public void clean() {
        customization.free();
        for (FiguraModelPart child : children)
            child.clean();
    }

    public enum ParentType {
        None(false, "NONE"),

        Head(true, "HEAD"),
        Body(true, "BODY"),
        LeftArm(true, "LEFT_ARM"),
        RightArm(true, "RIGHT_ARM"),
        LeftLeg(true,"LEFT_LEG"),
        RightLeg(true,"RIGHT_LEG"),

        LeftElytra(true, "LeftElytron, LEFT_ELYTRON, LEFT_ELYTRA"),
        RightElytra(true, "RightElytron, RIGHT_ELYTRON, RIGHT_ELYTRA"),

        World(false, "WORLD");

        public ArrayList<String> aliases;
        public boolean vanilla;

        ParentType(Boolean vanilla, String... aliases) {
            this.vanilla = vanilla;
            this.aliases = new ArrayList<>(aliases.length+1);
            this.aliases.add(name());
            this.aliases.addAll(Arrays.asList(aliases));
        }

        public static ParentType getForString(String str) {
            if (str != null)
                for (ParentType type : values())
                    for (String s : type.aliases)
                        if (str.startsWith(s))
                            return type;
            return None;
        }
    }

    //-- LUA BUSINESS --//

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_parent"
    )
    public static FiguraModelPart getParent(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.parent;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_children"
    )
    public static LuaTable getChildren(@LuaNotNil FiguraModelPart modelPart) {
        LuaTable table = new LuaTable();
        int i = 1;
        for (FiguraModelPart child : modelPart.children)
            table.put(i++, child);
        return table;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_pos"
    )
    public static FiguraVec3 getPos(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getPos();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "pos"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "x", "y", "z"}
                    )
            },
            description = "model_part.set_pos"
    )
    public static void setPos(@LuaNotNil FiguraModelPart modelPart, Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setPos", x, y, z);
        modelPart.customization.setPos(vec);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_rot"
    )
    public static FiguraVec3 getRot(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getRot();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "rot"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "x", "y", "z"}
                    )
            },
            description = "model_part.set_rot"
    )
    public static void setRot(@LuaNotNil FiguraModelPart modelPart, Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setRot", x, y, z);
        modelPart.customization.setRot(vec);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_bonus_rot"
    )
    public static FiguraVec3 getBonusRot(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getBonusRot();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "bonusRot"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "x", "y", "z"}
                    )
            },
            description = "model_part.set_bonus_rot"
    )
    public static void setBonusRot(@LuaNotNil FiguraModelPart modelPart, Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setBonusRot", x, y, z);
        modelPart.customization.setBonusRot(vec);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_scale"
    )
    public static FiguraVec3 getScale(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getScale();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "scale"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "x", "y", "z"}
                    )
            },
            description = "model_part.set_scale"
    )
    public static void setScale(@LuaNotNil FiguraModelPart modelPart, Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setScale", x, y, z, 1, 1, 1);
        modelPart.customization.setScale(vec);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_pivot"
    )
    public static FiguraVec3 getPivot(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getPivot();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "pivot"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "x", "y", "z"}
                    )
            },
            description = "model_part.set_pivot"
    )
    public static void setPivot(@LuaNotNil FiguraModelPart modelPart, Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setPivot", x, y, z);
        modelPart.customization.setPivot(vec);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_bonus_pivot"
    )
    public static FiguraVec3 getBonusPivot(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getBonusPivot();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "bonusPivot"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "x", "y", "z"}
                    )
            },
            description = "model_part.set_bonus_pivot"
    )
    public static void setBonusPivot(@LuaNotNil FiguraModelPart modelPart, Object x, Double y, Double z) {
        FiguraVec3 vec = LuaUtils.parseVec3("setBonusPivot", x, y, z);
        modelPart.customization.setBonusPivot(vec);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_position_matrix"
    )
    public static FiguraMat4 getPositionMatrix(@LuaNotNil FiguraModelPart modelPart) {
        modelPart.customization.recalculate();
        return modelPart.customization.getPositionMatrix();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_position_matrix_raw"
    )
    public static FiguraMat4 getPositionMatrixRaw(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getPositionMatrix();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_normal_matrix"
    )
    public static FiguraMat3 getNormalMatrix(@LuaNotNil FiguraModelPart modelPart) {
        modelPart.customization.recalculate();
        return modelPart.customization.getNormalMatrix();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_normal_matrix_raw"
    )
    public static FiguraMat3 getNormalMatrixRaw(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.getNormalMatrix();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, FiguraMat4.class},
                    argumentNames = {"modelPart", "matrix"}
            ),
            description = "model_part.set_matrix"
    )
    public static void setMatrix(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil FiguraMat4 matrix) {
        modelPart.customization.setMatrix(matrix);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_visible"
    )
    public static boolean getVisible(@LuaNotNil FiguraModelPart modelPart) {
        while (modelPart != null && modelPart.customization.visible == null)
            modelPart = modelPart.parent;
        return modelPart == null || modelPart.customization.visible;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, Boolean.class},
                    argumentNames = {"modelPart", "visible"}
            ),
            description = "model_part.set_visible"
    )
    public static void setVisible(@LuaNotNil FiguraModelPart modelPart, Boolean bool) {
        modelPart.customization.visible = bool;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.reset_visible"
    )
    public static void resetVisible(@LuaNotNil FiguraModelPart modelPart) {
        modelPart.customization.visible = null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_primary_render_type"
    )
    public static String getPrimaryRenderType(@LuaNotNil FiguraModelPart modelPart) {
        FiguraTextureSet.RenderTypes renderType = modelPart.customization.getPrimaryRenderType();
        return renderType == null ? null : renderType.name();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_secondary_render_type"
    )
    public static String getSecondaryRenderType(@LuaNotNil FiguraModelPart modelPart) {
        FiguraTextureSet.RenderTypes renderType = modelPart.customization.getSecondaryRenderType();
        return renderType == null ? null : renderType.name();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, String.class},
                    argumentNames = {"modelPart", "renderType"}
            ),
            description = "model_part.set_primary_render_type"
    )
    public static void setPrimaryRenderType(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil String type) {
        try {
            modelPart.customization.setPrimaryRenderType(FiguraTextureSet.RenderTypes.valueOf(type));
        } catch (Exception ignored) {
            throw new LuaRuntimeException("Illegal RenderType: \"" + type + "\".");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, String.class},
                    argumentNames = {"modelPart", "renderType"}
            ),
            description = "model_part.set_secondary_render_type"
    )
    public static void setSecondaryRenderType(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil String type) {
        try {
            modelPart.customization.setSecondaryRenderType(FiguraTextureSet.RenderTypes.valueOf(type));
        } catch (Exception ignored) {
            throw new LuaRuntimeException("Illegal RenderType: \"" + type + "\".");
        }
    }

    public final FiguraMat4 savedPartToWorldMat = FiguraMat4.of();
    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.part_to_world_matrix"
    )
    public static FiguraMat4 partToWorldMatrix(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.savedPartToWorldMat.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_texture_size"
    )
    public static FiguraVec2 getTextureSize(@LuaNotNil FiguraModelPart modelPart) {
        if (modelPart.textureWidth == -1 || modelPart.textureHeight == -1)
            throw new LuaRuntimeException("Cannot get texture size of part, it has multiple different-sized textures!");
        return FiguraVec2.of(modelPart.textureWidth, modelPart.textureHeight);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec2.class},
                            argumentNames = {"modelPart", "uv"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "u", "v"}
                    )
            },
            description = "model_part.set_uv"
    )
    public static void setUV(@LuaNotNil FiguraModelPart modelPart, Object x, Double y) {
        modelPart.customization.uvMatrix.reset();
        FiguraVec2 uv = LuaUtils.parseVec2("setUV", x, y);
        modelPart.customization.uvMatrix.translate(uv.x, uv.y);
        uv.free();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec2.class},
                            argumentNames = {"modelPart", "uv"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "u", "v"}
                    )
            },
            description = "model_part.set_uv_pixels"
    )
    public static void setUVPixels(@LuaNotNil FiguraModelPart modelPart, Object x, Double y) {
        if (modelPart.textureWidth == -1 || modelPart.textureHeight == -1)
            throw new LuaRuntimeException("Cannot call setUVPixels on a part with multiple texture sizes!");

        modelPart.customization.uvMatrix.reset();
        FiguraVec2 uv = LuaUtils.parseVec2("setUVPixels", x, y);
        uv.divide(modelPart.textureWidth, modelPart.textureHeight);
        modelPart.customization.uvMatrix.translate(uv.x, uv.y);
        uv.free();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, FiguraMat3.class},
                    argumentNames = {"modelPart", "matrix"}
            ),
            description = "model_part.set_uv_matrix"
    )
    public static void setUVMatrix(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil FiguraMat3 matrix) {
        modelPart.customization.uvMatrix.set(matrix);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec3.class},
                            argumentNames = {"modelPart", "color"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Double.class, Double.class, Double.class},
                            argumentNames = {"modelPart", "r", "g", "b"}
                    )
            },
            description = "model_part.set_color"
    )
    public static void setColor(@LuaNotNil FiguraModelPart modelPart, Object r, Double g, Double b) {
        modelPart.customization.color = LuaUtils.parseVec3("setColor", r, g, b, 1, 1, 1);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_color"
    )
    public static FiguraVec3 getColor(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.color.copy();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, Float.class},
                    argumentNames = {"modelPart", "opacity"}
            ),
            description = "model_part.set_opacity"
    )
    public static void setOpacity(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil Float opacity) {
        modelPart.customization.alpha = opacity;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_opacity"
    )
    public static Float getOpacity(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.alpha;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, FiguraVec2.class},
                            argumentNames = {"modelPart", "light"}
                    ),
                    @LuaFunctionOverload(
                            argumentTypes = {FiguraModelPart.class, Integer.class, Integer.class},
                            argumentNames = {"modelPart", "blockLight", "skyLight"}
                    )
            },
            description = "model_part.set_light"
    )
    public static void setLight(@LuaNotNil FiguraModelPart modelPart, Object light, Double skyLight) {
        FiguraVec2 lightVec = LuaUtils.parseVec2("setLight", light, skyLight);
        modelPart.customization.light = LightTexture.pack((int) lightVec.x, (int) lightVec.y);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_light"
    )
    public static FiguraVec2 getLight(@LuaNotNil FiguraModelPart modelPart) {
        int light = modelPart.customization.light;
        return FiguraVec2.of(LightTexture.block(light), LightTexture.sky(light));
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = {FiguraModelPart.class, String.class},
                    argumentNames = {"modelPart", "parentType"}
            ),
            description = "model_part.set_parent_type"
    )
    public static void setParentType(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil String parent) {
        try {
            modelPart.parentType = ParentType.valueOf(parent);
        } catch (Exception ignored) {
            throw new LuaRuntimeException("Illegal ParentType: \"" + parent + "\".");
        }
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_parent_type"
    )
    public static String getParentType(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.parentType == null ? null : modelPart.parentType.name();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaFunctionOverload(
                    argumentTypes = FiguraModelPart.class,
                    argumentNames = "modelPart"
            ),
            description = "model_part.get_type"
    )
    public static String getType(@LuaNotNil FiguraModelPart modelPart) {
        return modelPart.customization.partType.name();
    }

    //-- METAMETHODS --//
    @LuaWhitelist
    public static Object __index(@LuaNotNil FiguraModelPart modelPart, @LuaNotNil String key) {
        if (modelPart.childCache.containsKey(key))
            return modelPart.childCache.get(key);
        for (FiguraModelPart child : modelPart.children) {
            if (child.name.equals(key)) {
                modelPart.childCache.put(key, child);
                return child;
            }
        }
        modelPart.childCache.put(key, null);
        return null;
    }

    //-- READING METHODS FROM NBT --//

    private FiguraModelPart(String name, PartCustomization customization, int index, List<FiguraModelPart> children) {
        this.name = name;
        this.customization = customization;
        this.index = index;
        this.children = children;
        for (FiguraModelPart child : children)
            child.parent = this;
    }

    public static FiguraModelPart read(CompoundTag partCompound, List<FiguraImmediateBuffer.Builder> bufferBuilders, List<FiguraTextureSet> textureSets) {
        return read(partCompound, bufferBuilders, new int[] {0}, textureSets);
    }

    private static FiguraModelPart read(CompoundTag partCompound, List<FiguraImmediateBuffer.Builder> bufferBuilders, int[] index, List<FiguraTextureSet> textureSets) {
        //Read name
        String name = partCompound.getString("name");

        //Read transformation
        PartCustomization customization = PartCustomization.of();
        FiguraVec3 target = FiguraVec3.of();
        readVec3(target, partCompound, "rot");
        customization.setRot(target);
        readVec3(target, partCompound, "piv");
        customization.setPivot(target);
        target.free();
        if (partCompound.contains("primary")) {
            try {
                customization.setPrimaryRenderType(FiguraTextureSet.RenderTypes.valueOf(partCompound.getString("primary")));
            } catch (Exception ignored) {}
        }
        if (partCompound.contains("secondary")) {
            try {
                customization.setSecondaryRenderType(FiguraTextureSet.RenderTypes.valueOf(partCompound.getString("secondary")));
            } catch (Exception ignored) {}
        }
        if (partCompound.contains("vsb"))
            customization.visible = partCompound.getBoolean("vsb");

        customization.needsMatrixRecalculation = true;

        //Read vertex data
        int newIndex = -1;
        List<Integer> facesByTexture = new ArrayList<>(0);
        if (hasCubeData(partCompound)) {
            readCuboid(facesByTexture, bufferBuilders, partCompound);
            customization.partType = PartCustomization.PartType.CUBE;
            newIndex = index[0]++;
        } else if (hasMeshData(partCompound)) {
            //TODO: smooth normals
            readMesh(facesByTexture, bufferBuilders, partCompound);
            customization.partType = PartCustomization.PartType.MESH;
            newIndex = index[0]++;
        }

        //Read children
        ArrayList<FiguraModelPart> children = new ArrayList<>(0);
        if (partCompound.contains("chld")) {
            ListTag listTag = partCompound.getList("chld", Tag.TAG_COMPOUND);
            for (Tag tag : listTag)
                children.add(read((CompoundTag) tag, bufferBuilders, index, textureSets));
        }

        FiguraModelPart result = new FiguraModelPart(name, customization, newIndex, children);
        result.facesByTexture = facesByTexture;
        storeTexSize(result, textureSets);
        if (partCompound.contains("pt"))
            result.parentType = ParentType.valueOf(partCompound.getString("pt"));

        return result;
    }

    /**
     * There's a lot of obscure cases to test this on, so... something might go wrong with it, and I can't test everything.
     * Obviously I *think* it should work, and it has so far, but I still might be missing something.
     */
    private static void storeTexSize(FiguraModelPart modelPart, List<FiguraTextureSet> textureSets) {
        int w = -1, h = -1;
        for (FiguraModelPart child : modelPart.children) {
            //If any child has multiple textures, then we know this parent must as well.
            if (child.textureWidth == -1) {
                modelPart.textureWidth = -1;
                modelPart.textureHeight = -1;
                return;
            }
            //If any child has a texture different than one we've already seen, this parent must have multiple textures.
            if (child.textureWidth != w || child.textureHeight != h) {
                if (w != -1) {
                    modelPart.textureWidth = -1;
                    modelPart.textureHeight = -1;
                    return;
                }
                w = child.textureWidth;
                h = child.textureHeight;
            }
        }
        if (modelPart.index != -1) {
            int i = -1;
            for (int j = 0; j < modelPart.facesByTexture.size(); j++) {
                if (modelPart.facesByTexture.get(j) > 0) {
                    int realTexWidth = textureSets.get(j).getWidth();
                    int realTexHeight = textureSets.get(j).getHeight();
                    if ((w != -1 && w != realTexWidth) || (h != -1 && h != realTexHeight)) {
                        modelPart.textureWidth = -1;
                        modelPart.textureHeight = -1;
                        return;
                    }
                    if (i != -1) {
                        modelPart.textureWidth = -1;
                        modelPart.textureHeight = -1;
                        return;
                    }
                    i = j;
                    w = realTexWidth;
                    h = realTexHeight;
                }
            }
        }
        modelPart.textureWidth = w;
        modelPart.textureHeight = h;
    }

    private static void readVec3(FiguraVec3 target, CompoundTag tag, String name) {
        readVec3(target, tag, name, 0, 0, 0);
    }

    private static void readVec3(FiguraVec3 target, CompoundTag tag, String name, double defX, double defY, double defZ) {
        if (tag.contains(name)) {
            ListTag list = (ListTag) tag.get(name);
            switch (list.getElementType()) {
                case Tag.TAG_FLOAT -> target.set(list.getFloat(0), list.getFloat(1), list.getFloat(2));
                case Tag.TAG_INT -> target.set(list.getInt(0), list.getInt(1), list.getInt(2));
                case Tag.TAG_SHORT -> target.set(list.getShort(0), list.getShort(1), list.getShort(2));
                case Tag.TAG_BYTE -> target.set(
                        ((ByteTag) list.get(0)).getAsByte(),
                        ((ByteTag) list.get(1)).getAsByte(),
                        ((ByteTag) list.get(2)).getAsByte()
                );
            }
        } else {
            target.set(defX, defY, defZ);
        }
    }

    private static void readVec4(FiguraVec4 target, CompoundTag tag, String name) {
        if (tag.contains(name)) {
            ListTag list = (ListTag) tag.get(name);
            switch (list.getElementType()) {
                case Tag.TAG_FLOAT -> target.set(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
                case Tag.TAG_INT -> target.set(list.getInt(0), list.getInt(1), list.getInt(2), list.getInt(3));
                case Tag.TAG_SHORT -> target.set(list.getShort(0), list.getShort(1), list.getShort(2), list.getShort(3));
                case Tag.TAG_BYTE -> target.set(
                        ((ByteTag) list.get(0)).getAsByte(),
                        ((ByteTag) list.get(1)).getAsByte(),
                        ((ByteTag) list.get(2)).getAsByte(),
                        ((ByteTag) list.get(3)).getAsByte()
                );
            }
        } else {
            target.set(0, 0, 0, 0);
        }
    }

    private static boolean hasCubeData(CompoundTag partCompound) {
        if (partCompound.contains("cube_data", Tag.TAG_COMPOUND))
            return !partCompound.getCompound("cube_data").isEmpty();
        return false;
    }

    private static boolean hasMeshData(CompoundTag partCompound) {
        if (partCompound.contains("mesh_data", Tag.TAG_COMPOUND))
            return !partCompound.getCompound("mesh_data").isEmpty();
        return false;
    }

    private static final Map<String, FiguraVec3[]> faceData = new ImmutableMap.Builder<String, FiguraVec3[]>()
            .put("n", new FiguraVec3[] {
                    FiguraVec3.of(1, 0, 0),
                    FiguraVec3.of(0, 0, 0),
                    FiguraVec3.of(0, 1, 0),
                    FiguraVec3.of(1, 1, 0),
                    FiguraVec3.of(0, 0, -1)
            })
            .put("s", new FiguraVec3[] {
                    FiguraVec3.of(0, 0, 1),
                    FiguraVec3.of(1, 0, 1),
                    FiguraVec3.of(1, 1, 1),
                    FiguraVec3.of(0, 1, 1),
                    FiguraVec3.of(0, 0, 1)
            })
            .put("e", new FiguraVec3[] {
                    FiguraVec3.of(1, 0, 1),
                    FiguraVec3.of(1, 0, 0),
                    FiguraVec3.of(1, 1, 0),
                    FiguraVec3.of(1, 1, 1),
                    FiguraVec3.of(1, 0, 0)
            })
            .put("w", new FiguraVec3[] {
                    FiguraVec3.of(0, 0, 0),
                    FiguraVec3.of(0, 0, 1),
                    FiguraVec3.of(0, 1, 1),
                    FiguraVec3.of(0, 1, 0),
                    FiguraVec3.of(-1, 0, 0)
            })
            .put("u", new FiguraVec3[] {
                    FiguraVec3.of(0, 1, 1),
                    FiguraVec3.of(1, 1, 1),
                    FiguraVec3.of(1, 1, 0),
                    FiguraVec3.of(0, 1, 0),
                    FiguraVec3.of(0, 1, 0)
            })
            .put("d", new FiguraVec3[] {
                    FiguraVec3.of(0, 0, 0),
                    FiguraVec3.of(1, 0, 0),
                    FiguraVec3.of(1, 0, 1),
                    FiguraVec3.of(0, 0, 1),
                    FiguraVec3.of(0, -1, 0)
            }).build();

    private static final FiguraVec2[] uvValues = new FiguraVec2[] {
            FiguraVec2.of(0, 1),
            FiguraVec2.of(1, 1),
            FiguraVec2.of(1, 0),
            FiguraVec2.of(0, 0)
    };

    private static final FiguraVec3 from = FiguraVec3.of();
    private static final FiguraVec3 to = FiguraVec3.of();
    private static final FiguraVec3 ftDiff = FiguraVec3.of();

    private static void readCuboid(List<Integer> facesByTexture, List<FiguraImmediateBuffer.Builder> builders, CompoundTag data) {
        //Read from and to
        readVec3(from, data, "f");
        readVec3(to, data, "t");

        //Read inflate
        double inflate = 0;
        if (data.contains("inf"))
            inflate = data.getFloat("inf");
        from.add(-inflate, -inflate, -inflate);
        to.add(inflate, inflate, inflate);

        //Cache difference between from and to
        ftDiff.set(to);
        ftDiff.subtract(from);

        //Iterate over faces, add them
        for (String direction : faceData.keySet())
            readFace(data.getCompound("cube_data"), facesByTexture, builders, direction);
    }

    private static final FiguraVec3 tempPos = FiguraVec3.of();
    private static final FiguraVec4 uv = FiguraVec4.of();

    private static void readFace(CompoundTag faces, List<Integer> facesByTexture, List<FiguraImmediateBuffer.Builder> builders, String direction) {
        if (faces.contains(direction)) {
            CompoundTag face = faces.getCompound(direction);
            short texId = face.getShort("tex");
            while (texId >= facesByTexture.size())
                facesByTexture.add(0);
            while (texId >= builders.size())
                builders.add(FiguraImmediateBuffer.builder());
            facesByTexture.set(texId, facesByTexture.get(texId) + 1);

            FiguraVec3 normal = faceData.get(direction)[4];
            int rotation = (int) (face.getFloat("rot") / 90f);
            readVec4(uv, face, "uv");
            for (int i = 0; i < 4; i++) {
                tempPos.set(ftDiff);
                tempPos.multiply(faceData.get(direction)[i]);
                tempPos.add(from);

                FiguraVec2 normalizedUv = uvValues[(i + rotation)%4];

                builders.get(texId).vertex(
                        (float) tempPos.x, (float) tempPos.y, (float) tempPos.z,
                        (float) Mth.lerp(normalizedUv.x, uv.x, uv.z),
                        (float) Mth.lerp(normalizedUv.y, uv.y, uv.w),
                        (float) normal.x, (float) normal.y, (float) normal.z
                );
            }
        }
    }

    private static void readMesh(List<Integer> facesByTexture, List<FiguraImmediateBuffer.Builder> builders, CompoundTag data) {
        boolean useSmoothShading = false;

        CompoundTag meshData = data.getCompound("mesh_data");
        //mesh_data:
        //"vtx": List<Float>, xyz
        //"tex": List<Short>, (texID << 4) + numVerticesInFace
        //"fac": List<Byte, Short, or Int>, just the indices of various vertices
        //"uvs": List<Float>, uv for each vertex

        if (useSmoothShading)
            readMeshSmooth(facesByTexture, builders, meshData);
        else
            readMeshRegular(facesByTexture, builders, meshData);
    }

    private static final FiguraVec3 p1 = FiguraVec3.of(), p2 = FiguraVec3.of(), p3 = FiguraVec3.of();

    private static void readMeshRegular(List<Integer> facesByTexture, List<FiguraImmediateBuffer.Builder> builders, CompoundTag meshData) {
        ListTag verts = meshData.getList("vtx", Tag.TAG_FLOAT);
        ListTag uvs = meshData.getList("uvs", Tag.TAG_FLOAT);
        ListTag tex = meshData.getList("tex", Tag.TAG_SHORT);

        int bestType = 0; //byte
        if (verts.size() > 255 * 3) bestType = 1; //short
        if (verts.size() > 32767 * 3) bestType = 2; //int

        ListTag fac = switch (bestType) {
            case 0 -> meshData.getList("fac", Tag.TAG_BYTE);
            case 1 -> meshData.getList("fac", Tag.TAG_SHORT);
            default -> meshData.getList("fac", Tag.TAG_INT);
        };

        int vi = 0, uvi = 0;

        float[] posArr = new float[12];
        float[] uvArr = new float[8];

        for (int ti = 0; ti < tex.size(); ti++) {
            short packed = tex.getShort(ti);
            int texId = packed >> 4;
            int numVerts = packed & 0xf;
            while (texId >= facesByTexture.size())
                facesByTexture.add(0);
            while (texId >= builders.size())
                builders.add(FiguraImmediateBuffer.builder());
            facesByTexture.set(texId, facesByTexture.get(texId) + 1);

            for (int j = 0; j < numVerts; j++) {
                int vid = switch (bestType) {
                    case 0 -> ((ByteTag) fac.get(vi + j)).getAsByte() & 0xff;
                    case 1 -> fac.getShort(vi + j) & 0xffff;
                    default -> fac.getInt(vi + j);
                };
                posArr[3*j] = verts.getFloat(3*vid);
                posArr[3*j+1] = verts.getFloat(3*vid+1);
                posArr[3*j+2] = verts.getFloat(3*vid+2);

                uvArr[2*j] = uvs.getFloat(uvi + 2*j);
                uvArr[2*j+1] = uvs.getFloat(uvi + 2*j + 1);
            }

            p1.set(posArr[0], posArr[1], posArr[2]);
            p2.set(posArr[3], posArr[4], posArr[5]);
            p3.set(posArr[6], posArr[7], posArr[8]);
            p3.subtract(p2);
            p1.subtract(p2);
            p3.cross(p1);
            p3.normalize();
            //p3 now contains the normal vector

            for (int j = 0; j < numVerts; j++)
                builders.get(texId).vertex(
                        posArr[3*j], posArr[3*j+1], posArr[3*j+2],
                        uvArr[2*j], uvArr[2*j+1],
                        (float) p3.x, (float) p3.y, (float) p3.z
                );
            if (numVerts == 3)
                builders.get(texId).vertex(
                        posArr[6], posArr[7], posArr[8],
                        uvArr[4], uvArr[5],
                        (float) p3.x, (float) p3.y, (float) p3.z
                );

            vi += numVerts;
            uvi += 2*numVerts;
        }
    }

    private static void readMeshSmooth(List<Integer> facesByTexture, List<FiguraImmediateBuffer.Builder> builders, CompoundTag meshData) {

    }

}

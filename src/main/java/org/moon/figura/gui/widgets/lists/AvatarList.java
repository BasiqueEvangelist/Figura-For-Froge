package org.moon.figura.gui.widgets.lists;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatars.providers.LocalAvatarFetcher;
import org.moon.figura.gui.widgets.TextField;
import org.moon.figura.gui.widgets.TexturedButton;
import org.moon.figura.gui.widgets.avatar.AbstractAvatarWidget;
import org.moon.figura.gui.widgets.avatar.AvatarFolderWidget;
import org.moon.figura.gui.widgets.avatar.AvatarWidget;
import org.moon.figura.utils.FiguraIdentifier;
import org.moon.figura.utils.FiguraText;
import org.moon.figura.utils.ui.UIHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AvatarList extends AbstractList {

    // -- Variables -- //
    private final HashMap<Path, AbstractAvatarWidget> avatars = new HashMap<>();
    private final HashSet<Path> missingPaths = new HashSet<>();
    private final ArrayList<AbstractAvatarWidget> avatarList = new ArrayList<>();

    private final AvatarWidget unselect;

    private int totalHeight = 0;
    private String filter = "";

    public static AvatarWidget selectedEntry;

    // -- Constructors -- //

    public AvatarList(int x, int y, int width, int height) {
        super(x, y, width, height);

        unselect = new AvatarWidget(0, this.width - 22, null, this) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                boolean bool = super.mouseClicked(mouseX, mouseY, button);
                context.setVisible(false);
                return bool;
            }

            @Override
            public Component getName() {
                return new FiguraText("gui.wardrobe.unselect").withStyle(ChatFormatting.GRAY);
            }
        };

        children.add(new TextField(x + 4, y + 4, width - 32, 20, new FiguraText("gui.search"), s -> filter = s));
        children.add(new TexturedButton(
                x + width - 24, y + 4,
                20, 20, 20, 0, 20,
                new FiguraIdentifier("textures/gui/folder.png"),
                40, 40,
                new FiguraText("gui.wardrobe.folder.tooltip"),
                button -> Util.getPlatform().openFile(LocalAvatarFetcher.getLocalAvatarDirectory().toFile()))
        );

        //scrollbar
        this.scrollBar.y = y + 28;
        this.scrollBar.setHeight(height - 32);

        //scissors
        this.updateScissors(1, 24, -2, -27);

        //initial load
        LocalAvatarFetcher.load();
    }

    // -- Functions -- //
    @Override
    public void tick() {
        //update list
        if (FiguraMod.ticks % 20 == 0)
            LocalAvatarFetcher.load();
        loadContents();
        super.tick();
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float delta) {
        //background and scissors
        UIHelper.renderSliced(stack, x, y, width, height, UIHelper.OUTLINE);
        UIHelper.setupScissor(x + scissorsX, y + scissorsY, width + scissorsWidth, height + scissorsHeight);

        //scrollbar
        totalHeight = -1;
        for (AbstractAvatarWidget avatar : avatarList)
            totalHeight += avatar.getHeight() + 2;
        int entryHeight = avatarList.isEmpty() ? 0 : totalHeight / avatarList.size();

        scrollBar.visible = totalHeight > height - 32;
        scrollBar.setScrollRatio(entryHeight, totalHeight - (height - 34));

        //render list
        int xOffset = scrollBar.visible ? 4 : 11;
        int yOffset = scrollBar.visible ? (int) -(Mth.lerp(scrollBar.getScrollProgress(), -32, totalHeight - height)) : 32;
        boolean hidden = false;

        for (AbstractAvatarWidget avatar : avatarList) {
            if (hidden) continue;

            avatar.setPos(x + xOffset, y + yOffset);

            if (avatar.y + avatar.getHeight() > y + scissorsY)
                avatar.render(stack, mouseX, mouseY, delta);

            yOffset += avatar.getHeight() + 2;
            if (yOffset > height)
                hidden = true;
        }

        //reset scissor
        RenderSystem.disableScissor();

        //render children
        super.render(stack, mouseX, mouseY, delta);
    }

    private void loadContents() {
        missingPaths.clear();
        missingPaths.addAll(avatars.keySet());

        // Load avatars //
        List<LocalAvatarFetcher.AvatarPath> foundAvatars = LocalAvatarFetcher.ALL_AVATARS;

        for (LocalAvatarFetcher.AvatarPath avatar : foundAvatars) {
            Path path = avatar.getPath();
            String name = path.getFileName().toString();

            //filter
            if (!name.toLowerCase().contains(filter.toLowerCase()))
                continue;

            missingPaths.remove(path);
            avatars.computeIfAbsent(path, p -> {
                int width = this.width - 22;
                AbstractAvatarWidget entry = avatar.hasAvatar() ? new AvatarWidget(0, width, avatar.getPath(), this) : new AvatarFolderWidget(0, width, avatar, this);

                avatarList.add(entry);
                children.add(entry);

                return entry;
            });
        }

        //Remove missing avatars
        for (Path missingPath : missingPaths) {
            AbstractAvatarWidget obj = avatars.remove(missingPath);
            avatarList.remove(obj);
            children.remove(obj);
        }

        //sort list
        avatarList.sort(AbstractAvatarWidget::compareTo);
        children.sort((children1, children2) -> {
            if (children1 instanceof AbstractAvatarWidget avatar1 && children2 instanceof AbstractAvatarWidget avatar2)
                return avatar1.compareTo(avatar2);
            return 0;
        });

        if (filter.isEmpty()) {
            avatars.computeIfAbsent(Path.of(""), path -> {
                avatarList.add(0, unselect);
                children.add(2, unselect); //after field and scrollbar
                return unselect;
            });
        }
    }

    public void updateScroll() {
        //store old scroll pos
        float pastScroll = (totalHeight - height) * scrollBar.getScrollProgress();

        //get new height
        totalHeight = -1;
        for (AbstractAvatarWidget avatar : avatarList)
            totalHeight += avatar.getHeight() + 2;

        //set new scroll percentage
        scrollBar.setScrollProgress(pastScroll / (totalHeight - height));
    }

    @Override
    public List<? extends GuiEventListener> contents() {
        return avatarList;
    }
}

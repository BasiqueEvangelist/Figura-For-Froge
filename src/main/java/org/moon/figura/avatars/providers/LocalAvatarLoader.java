package org.moon.figura.avatars.providers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatars.AvatarManager;
import org.moon.figura.parsers.AvatarMetadataParser;
import org.moon.figura.parsers.BlockbenchModelParser;
import org.moon.figura.parsers.LuaScriptParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * class used to load avatars from a file
 * and used for hotswapping
 */
public class LocalAvatarLoader {

    private static Path lastLoadedPath;

    private static WatchService watcher;
    private final static HashMap<Path, WatchKey> keys = new HashMap<>();
    static {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to initialize the watcher service", e);
        }
    }

    /**
     * Loads an NbtCompound from the specified path
     * @param path - the file/folder for loading the avatar
     * @return the NbtCompound from this path
     */
    public static CompoundTag loadAvatar(Path path) throws IOException {
        lastLoadedPath = path;
        resetWatchKeys();
        addWatchKey(path);

        if (path == null)
            return null;

        //load as nbt (.moon)
        if (path.toString().endsWith(".moon")) {
            //NbtIo already closes the file stream
            return NbtIo.readCompressed(new FileInputStream(path.toFile()));
        }

        //load as folder
        CompoundTag nbt = new CompoundTag();

        //Load metadata first!
        String metadata = readFile(path.resolve("avatar.json").toFile());
        nbt.put("metadata", AvatarMetadataParser.parse(metadata, path.getFileName().toString()));

        //scripts
        List<File> scripts = getFilesByExtension(path, ".lua", true);
        if (scripts.size() > 0) {
            CompoundTag scriptsNbt = new CompoundTag();
            String pathRegex = Pattern.quote(path + File.separator);
            for (File script : scripts) {
                String pathStr = script.toPath().toString();
                String name = pathStr.replaceFirst(pathRegex, "");
                name = name.replace(File.separatorChar, '/');
                scriptsNbt.put(name.substring(0, name.length() - 4), LuaScriptParser.parse(readFile(script)));
            }

            nbt.put("scripts", scriptsNbt);

            //sounds
            //avatar needs a script to load custom sounds
            List<File> sounds = getFilesByExtension(path, ".ogg", false);
            if (sounds.size() > 0) {
                CompoundTag soundsNbt = new CompoundTag();
                for (File sound : sounds) {
                    String name = sound.getName();
                    soundsNbt.putByteArray(name.substring(0, name.length() - 4), readFile(sound).getBytes());
                }

                nbt.put("sounds", soundsNbt);
            }
        }

        //models
        //Recursion not yet implemented
        List<File> models = getFilesByExtension(path, ".bbmodel", false);

        //if no model is found we can return the avatar here
        if (models.size() == 0)
            return nbt;

        CompoundTag modelRoot = new CompoundTag();
        modelRoot.putString("name", "models");

        ListTag children = new ListTag();
        ListTag textures = new ListTag();
        ListTag animations = new ListTag();

        BlockbenchModelParser parser = new BlockbenchModelParser();
        for (File model : models) {
            String name = model.getName();
            BlockbenchModelParser.ModelData data = parser.parseModel(readFile(model), name.substring(0, name.length() - 8));
            children.add(data.modelNbt());
            textures.addAll(data.textureList());
            animations.addAll(data.animationList());
        }

        modelRoot.put("chld", children);

        AvatarMetadataParser.injectToModels(metadata, modelRoot);

        //return :3
        nbt.put("models", modelRoot);
        nbt.put("textures", textures);
        nbt.put("animations", animations);

        return nbt;
    }

    /**
     * Saves the loaded NBT into a folder inside the avatar list
     */
    public static void saveNbt(CompoundTag nbt) {
        Path directory = LocalAvatarFetcher.getLocalAvatarDirectory().resolve("[§9Figura§r] Cached Avatars");
        Path file = directory.resolve("cache-" + new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(new Date()) + ".moon");
        try {
            Files.createDirectories(directory);
            NbtIo.writeCompressed(nbt, new FileOutputStream(file.toFile()));
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save avatar: " + file.getFileName().toString(), e);
        }
    }

    /**
     * Tick the watched key for hotswapping avatars
     */
    public static void tickWatchedKey() {
        WatchEvent<?> event = null;
        boolean reload = false;

        for (Map.Entry<Path, WatchKey> entry : keys.entrySet()) {
            WatchKey key = entry.getValue();
            if (!key.isValid())
                continue;

            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                if (watchEvent.kind() == StandardWatchEventKinds.OVERFLOW)
                    continue;

                event = watchEvent;
                File file = entry.getKey().resolve(((WatchEvent<Path>) event).context()).toFile();

                if (file.isDirectory() && (file.isHidden() || file.getName().startsWith(".")))
                    continue;

                reload = true;
                break;
            }

            if (reload)
                break;
        }

        //reload avatar
        if (reload) {
            FiguraMod.LOGGER.debug("Local avatar files changed - Reloading!");
            FiguraMod.LOGGER.debug(event.context().toString());
            AvatarManager.loadLocalAvatar(lastLoadedPath);
        }
    }

    private static void resetWatchKeys() {
        for (WatchKey key : keys.values())
            key.cancel();
        keys.clear();
    }

    /**
     * register new watch keys
     * @param path the path to register the watch key
     */
    private static void addWatchKey(Path path) {
        if (watcher == null || path == null)
            return;

        File file = path.toFile();
        if (!file.isDirectory() || file.isHidden() || file.getName().startsWith("."))
            return;

        try {
            WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            keys.put(path, key);

            File[] children = file.listFiles();
            if (children == null)
                return;

            for (File child : children)
                addWatchKey(child.toPath());
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to register watcher for " + path, e);
        }
    }

    // -- helper functions -- //

    public static List<File> getFilesByExtension(Path root, String extension, boolean recurse) {
        List<File> result = new ArrayList<>();
        File rf = root.toFile();
        File[] children = rf.listFiles();
        if (children == null) return result;
        for (File child : children) {
            if (recurse && child.isDirectory() && !child.isHidden() && !child.getName().startsWith("."))
                result.addAll(getFilesByExtension(child.toPath(), extension, true));
            else if (child.toString().toLowerCase().endsWith(extension.toLowerCase()))
                result.add(child);
        }
        return result;
    }

    public static String readFile(File file) throws IOException {
        try {
            FileInputStream stream = new FileInputStream(file);
            String fileContent = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            stream.close();
            return fileContent;
        } catch (IOException e) {
            FiguraMod.LOGGER.error("Failed to read File: " + file);
            throw e;
        }
    }

    public static Path getLastLoadedPath() {
        return lastLoadedPath;
    }
}

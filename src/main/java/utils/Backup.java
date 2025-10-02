package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import core.Nat20bot;

public class Backup {

    private static <K, V> void saveBackup(Map<K, V> savableFile)
            throws IOException, NoSuchFieldException, SecurityException {
        File backupDir = new File("backup/");
        if (!backupDir.exists()) {
            backupDir.mkdirs(); // crea cartella se non c’è
        }

        String name = createName(savableFile);
        try (FileOutputStream fos = new FileOutputStream(new File(backupDir, name + ".dnd"));
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(savableFile);
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> ConcurrentHashMap<K, V> openLatest() throws IOException, ClassNotFoundException {
        File backupDir = new File("backup/");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
            return new ConcurrentHashMap<>();
        }

        File[] files = backupDir.listFiles((d, n) -> n.endsWith(".dnd"));
        if (files == null || files.length == 0) {
            return new ConcurrentHashMap<>();
        }

        // Find the most recently modified backup
        File latest = Arrays.stream(files)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);

        if (latest == null) {
            return new ConcurrentHashMap<>();
        }

        // Delete all other backups, keep only the latest
        for (File f : files) {
            if (!f.equals(latest)) {
                f.delete();
            }
        }

        // set most recent file
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(latest))) {
            return (ConcurrentHashMap<K, V>) ois.readObject();
        }
    }

    private static String createName(Map<?, ?> savableFile) {
        return "game_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    public static void save(Nat20bot bot) throws IOException, NoSuchFieldException, SecurityException {
        saveBackup(bot.get_players());
    }

    public static void open(Nat20bot bot)
            throws IOException, NoSuchFieldException, SecurityException, ClassNotFoundException {
        bot.set_players(openLatest());
    }
}

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import core.Nat20bot;

public class Backup {

	private static <K, V> void saveBackup(Map<K, V> savableFile) throws IOException, NoSuchFieldException, SecurityException {
		String name = createName(savableFile);
		FileOutputStream fos = new FileOutputStream("backup/" + name + ".dnd");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(savableFile);
		oos.close();
	}
	@SuppressWarnings("unchecked")
	private static <K, V> ConcurrentHashMap<K, V> openLatest() throws IOException, ClassNotFoundException {
		File latest = Arrays.stream(new File("backup/").listFiles((d, n) -> n.endsWith(".dnd")))
			.max(Comparator.comparingLong(File::lastModified)).orElse(null);
		if (latest == null) return new ConcurrentHashMap<>();
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

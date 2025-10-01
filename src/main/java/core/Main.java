package core;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import utils.Backup;
import utils.GitHubStorage;

public class Main {
	// TODO Improve the backup system in future releases
	public static void main(String... Args)
			throws NoSuchFieldException, SecurityException, IOException, ClassNotFoundException, TelegramApiException,
			InterruptedException {

		// TODO dummy webserver to keep bot online, to remove in future deployment
		WebStub.start();

		// Initialize Telegram API
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

		// Register bot
		Nat20bot bot = new Nat20bot();
		botsApi.registerBot(bot);

		// Open saved states (if any)
		try {
			File backupFile = GitHubStorage.downloadLatest();
			if (backupFile.exists()) {
				Backup.open(bot);
			}
		} catch (Exception e) {
			System.err.println("WARNING: Failed to load backup, starting with empty state: " + e.getMessage());
		}

		// Save states on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				System.out.println("Shutdown detected, saving state...");
				Backup.save(bot);
				File today = new File("backup/game_" +
						LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".dnd");
				GitHubStorage.upload(today);
				System.out.println("State saved successfully on shutdown");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		// Local fast checkpoint thread
		new Thread(() -> {
			while (true) {
				try {
					TimeUnit.SECONDS.sleep(15);
					Backup.save(bot); // saves locally with timestamp
				} catch (Exception e) {
					e.printStackTrace(); // log and continue
				}
			}
		}).start();

		// // Remote upload thread: runs every minute to avoid exceeding GitHub API rate limits
		new Thread(() -> {
			while (true) {
				try {
					TimeUnit.MINUTES.sleep(1);
					File today = new File("backup/game_" +
							LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".dnd");
					if (today.exists()) {
						GitHubStorage.upload(today);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}

 package core;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import utils.Backup;

public class Main {


	public static void main(String... Args) throws NoSuchFieldException, SecurityException, IOException, ClassNotFoundException, TelegramApiException {
		/*Logger logger = Logger.getLogger("Main.class.getName()");
		Properties prop =  .getProperties();
		prop.setProperty("java.util.logging.config.file", "log/conf.txt");
		LogManager.getLogManager().readConfiguration()*/
			
		// api initialize
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

		// bot registration api
		Nat20bot bot = new Nat20bot();
		try {
			botsApi.registerBot(bot);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}

		// open saved states
		Backup.open(bot);

		// save states
		// on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					Backup.save(bot);
					System.out.println("states saved");
				} catch (SecurityException | NoSuchFieldException | IOException e) {
					e.printStackTrace();
				}
			}
		});

	// every 15 seconds
		new Thread() {
			public void run() {
				while (true) {
					try {
						TimeUnit.SECONDS.sleep(15);
						Backup.save(bot);
					} catch (IOException | InterruptedException | NoSuchFieldException | SecurityException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

	}
}

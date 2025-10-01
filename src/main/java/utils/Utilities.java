package utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Utilities {
    public static void makeRemovable(TelegramLongPollingBot bot, String chatId, Integer messageId,
            int seconds) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                bot.execute(new DeleteMessage(chatId, messageId));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, seconds, TimeUnit.SECONDS);
    }

}

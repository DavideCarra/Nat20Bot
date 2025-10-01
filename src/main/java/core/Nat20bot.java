package core;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.github.cdimascio.dotenv.Dotenv;

import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import utils.Env;
import utils.Pair;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

public class Nat20bot extends TelegramLongPollingBot {
    private ConcurrentHashMap<Long, Player> _players;
    private ExecutorService exec;
    private final String IMGBB_KEY;
    private final Long ADMIN_USER_ID;
    private final String TOKEN;

    public Nat20bot() {

        _players = new ConcurrentHashMap<Long, Player>();
        exec = Executors.newCachedThreadPool();
        _players.put(0L, new Player());
        IMGBB_KEY = Env.getRequired("IMGBB_KEY");
        ADMIN_USER_ID = Env.getRequiredLong("ADMIN_USER_ID");
        TOKEN = Env.getRequired("BOT_TOKEN");

   
    }

    public String getBotUsername() {
        return "Nat_20bot";
    }

    public String getBotToken() {
        // read token from envs in docker-compose
        return TOKEN;
    }

    public void onUpdateReceived(final Update update) {
        try {
            this.switchUpdate(update);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public boolean[] buildFlags() {
        final boolean[] temp = { false, false };
        return temp;
    }

    private void switchUpdate(final Update update) throws TelegramApiException {
        if (update.hasMessage() && update.getMessage().hasText()) {
            _players.computeIfAbsent(update.getMessage().getFrom().getId(),
                    key -> new Player(update.getMessage().getFrom().getId()));
        } else if (update.hasCallbackQuery()) {
            _players.computeIfAbsent((long) update.getCallbackQuery().getFrom().getId(),
                    key -> new Player((long) update.getCallbackQuery().getFrom().getId()));
        } else if (update.hasInlineQuery()) {
            _players.computeIfAbsent((long) update.getInlineQuery().getFrom().getId(),
                    key -> new Player((long) update.getInlineQuery().getFrom().getId()));
        }
        exec.submit((Runnable) new ExecThread(this, update));

    }

    public void set_players(ConcurrentHashMap<Long, Player> _players) {
        this._players = _players;
    }

    public ConcurrentHashMap<Long, Player> get_players() {
        return _players;
    }

    public Long getADMIN_USER_ID() {
        return ADMIN_USER_ID;
    }

    public String getIMGBB_KEY() {
        return IMGBB_KEY;
    }

}

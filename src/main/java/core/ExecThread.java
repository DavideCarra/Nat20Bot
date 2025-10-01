package core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultPhoto;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.InlineQueryResultCachedPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import state_machine.api.TransitionNotFoundException;
import state_machine.core.Event;
import state_machine.core.FiniteStateMachine;
import utils.Utilities;

public class ExecThread extends Thread {

	private ConcurrentHashMap<Long, Player> _players = new ConcurrentHashMap<>();
	private Player player;
	private Nat20bot bot;
	private long userId;
	private long chatId;
	private Integer messageId;
	private String queryText = "";
	private String iqueryText = "", iQid = "";
	private String messageText = "";
	private Long targetUserId = 0l;
	private Integer targetMessageId = 0;
	private String fileId = "";
	private boolean toRemove = false;
	private String botUsername;
	private final String IMGBB_KEY;
	private final Long ADMIN_USER_ID;

	public ExecThread(Nat20bot bot, Update update) throws TelegramApiRequestException {

		this.bot = bot;
		botUsername = bot.getBotUsername();
		this.IMGBB_KEY = bot.getIMGBB_KEY();
		this.ADMIN_USER_ID = bot.getADMIN_USER_ID();
		if (update.hasMessage()) {
			userId = update.getMessage().getFrom().getId();
			chatId = update.getMessage().getChatId();
			messageId = update.getMessage().getMessageId();
			if (update.getMessage().hasText()) {
				messageText = update.getMessage().getText();
				messageText = messageText.split("@")[0];
				if (update.getMessage().isReply() && messageText.equals("/player")) {
					targetUserId = update.getMessage()
							.getReplyToMessage()
							.getFrom()
							.getId();
					targetMessageId = update.getMessage()
							.getReplyToMessage()
							.getMessageId();
				}
			} else if (update.getMessage().hasPhoto()) {
				List<PhotoSize> photos = update.getMessage().getPhoto();
				fileId = photos.get(photos.size() - 1).getFileId();
				// take the last one which is the largest image
			}

		} else if (update.hasCallbackQuery()) {
			queryText = update.getCallbackQuery().getData();
			userId = update.getCallbackQuery().getFrom().getId();
		} else if (update.hasInlineQuery()) {
			iqueryText = update.getInlineQuery().getQuery();
			userId = update.getInlineQuery().getFrom().getId();
			iQid = update.getInlineQuery().getId();
		}

		_players = bot.get_players();
		player = _players.get(userId);
	}

	public void run() {
		try {

			// handle the message received
			try {
				chose();
			} catch (TransitionNotFoundException e) {
				e.printStackTrace();
			}
			if (!iQid.isEmpty()) {
				List<InlineQueryResult> results = setResults();
				AnswerInlineQuery answer = new AnswerInlineQuery();
				answer.setInlineQueryId(iQid);
				answer.setResults(results);
				answer.setCacheTime(0);

				bot.execute(answer);
			}
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}

	}

	public List<InlineQueryResult> setResults() throws TelegramApiException {
		String nameText = (player != null && player.getName() != null && !player.getName().isBlank())
				? player.getName()
				: "Nessun nome impostato";

		String bgText = (player != null && player.getBackground() != null && !player.getBackground().isBlank())
				? player.getBackground()
				: "Nessun background impostato";

		// final results list
		List<InlineQueryResult> results = new ArrayList<>();

		// see player name
		InlineQueryResultArticle result = new InlineQueryResultArticle();
		result.setId("1");
		result.setTitle("Nome player");
		result.setDescription("Manda in chat il tuo nome");
		result.setThumbUrl("https://t3.ftcdn.net/jpg/02/81/42/82/360_F_281428216_YWRTOqeBWBmtuWxBci02ClnEnI22Fh7e.jpg");
		result.setInputMessageContent(
				new InputTextMessageContent(nameText));
		results.add(result);

		// see player bg
		result = new InlineQueryResultArticle();
		result.setId("2");
		result.setTitle("Background player");
		result.setDescription("Manda in chat il tuo background");
		result.setThumbUrl("https://themantovanis.blog/wp-content/uploads/2019/02/ambientazione-fantasy.jpg?w=640");
		result.setInputMessageContent(
				new InputTextMessageContent(bgText));
		results.add(result);

		// see player image (only if present)
		if (player != null && player.getImage() != null && !player.getImage().isEmpty()) {
			try {
				String url = player.getImgbbUrl();
				if (url == null || url.isEmpty()) {
					// download image and convert in byte array
					GetFile gf = new GetFile();
					gf.setFileId(player.getImage());
					File f = bot.execute(gf);
					InputStream in = new URL(
							"https://api.telegram.org/file/bot" + bot.getBotToken() + "/" + f.getFilePath())
							.openStream();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] b = new byte[8192];
					int n;
					while ((n = in.read(b)) > 0)
						out.write(b, 0, n);
					in.close();

					// Upload on ImgBB
					HttpURLConnection c = (HttpURLConnection) new URL("https://api.imgbb.com/1/upload?key=" + IMGBB_KEY)
							.openConnection();
					c.setRequestMethod("POST");
					c.setDoOutput(true);
					c.getOutputStream()
							.write(("image="
									+ URLEncoder.encode(Base64.getEncoder().encodeToString(out.toByteArray()), "UTF-8"))
									.getBytes());
					Scanner s = new Scanner(c.getInputStream());
					String resp = "";
					while (s.hasNext())
						resp += s.nextLine();
					s.close();
					url = new JSONObject(resp).getJSONObject("data").getString("url");

					// Save url
					player.setImgbbUrl(url);
				}

				InlineQueryResultPhoto r3 = new InlineQueryResultPhoto();
				r3.setId("3");
				r3.setTitle("Immagine player");
				r3.setDescription("Flexa in chat il tuo incredibile personaggio!");
				r3.setCaption(player.getCaptionGenerator().getRandom());
				r3.setPhotoUrl(url);
				r3.setThumbUrl(url);
				results.add(r3);
			} catch (Exception e) {
				// If download fails, use cached image
				InlineQueryResultCachedPhoto fb = new InlineQueryResultCachedPhoto();
				fb.setId("3");
				fb.setPhotoFileId(player.getImage());
				results.add(fb);
			}
		}

		return results;
	}

	private void chose() throws TelegramApiException, TransitionNotFoundException {
		if (!iQid.isEmpty()) {
			return;
		}
		SendMessage message = SendMessage.builder().text("").chatId(userId).build();

		// create state machines
		FiniteStateMachine nameStateMachine = player.getNameStateMachine();
		FiniteStateMachine backgroundStateMachine = player.getBackgroundStateMachine();
		FiniteStateMachine imageStateMachine = player.getImageStateMachine();
		FiniteStateMachine allLineStateMachine = player.allLinesStateMachine();
		FiniteStateMachine singeLineStateMachine = player.singeLineStateMachine();

		// create states
		state_machine.core.State not_setted = new state_machine.core.State("not_setted");
		state_machine.core.State setting = new state_machine.core.State("setting");
		state_machine.core.State setted = new state_machine.core.State("setted");

		// create events

		Event setEvent = new Event("setEvent");
		Event resetEvent = new Event("resetEvent");
		Event settingEvent = new Event("settingEvent");

		if (messageText.equals("/setname") || nameStateMachine.isCurrentState(setting)) {
			if (nameStateMachine.isCurrentState(not_setted)) {
				nameStateMachine.change(settingEvent);
				message.setText("Invia il tuo nome appena sei pronto");
			} else if (nameStateMachine.isCurrentState(setting) && player.getName().isEmpty()) {
				player.setName(messageText);
				nameStateMachine.change(setEvent);
				message.setText("Nome impostato");
			} else {
				message.setText("Nome già impostato per modificarlo digitare /removename e aggiungerlo nuovamente");
			}
		} else if (messageText.equals("/setbg") || backgroundStateMachine.isCurrentState(setting)) {
			if (backgroundStateMachine.isCurrentState(not_setted)) {
				backgroundStateMachine.change(settingEvent);
				message.setText("Invia il tuo background appena sei pronto");
			} else if (backgroundStateMachine.isCurrentState(setting) && player.getBackground().isEmpty()) {
				player.setBackground(messageText);
				backgroundStateMachine.change(setEvent);
				message.setText("Background impostato");
			} else {
				message.setText("Background già impostato per modificarlo digitare /removebg e aggiungerlo nuovamente");
			}
		} else if (messageText.equals("/setimage") || imageStateMachine.isCurrentState(setting)) {
			if (imageStateMachine.isCurrentState(not_setted)) {
				imageStateMachine.change(settingEvent);
				message.setText("Invia l'immagine desiderata");
			} else if (imageStateMachine.isCurrentState(setting) && !fileId.isEmpty()) {
				player.setImage(fileId);
				imageStateMachine.change(setEvent);
				message.setText("Immagine impostata");
			} else if (imageStateMachine.isCurrentState(setting)) {
				message.setText("In attesa dell'immagine");
			} else {
				message.setText(
						"Immagine già impostata per modificarla digitare /removeimage e aggiungerlo nuovamente");
			}
		} else if (messageText.equals("/setlines") || allLineStateMachine.isCurrentState(setting)) {
			if (messageText.equals("/setlines") && allLineStateMachine.isCurrentState(setted)) {
				allLineStateMachine.change(resetEvent);
			}
			if (allLineStateMachine.isCurrentState(not_setted)) {
				allLineStateMachine.change(settingEvent);
				message.setText(
						"Puoi mandare una lista di frasi in formato:\n\n - frase1\n - frase2\n - frase3\n\nOppure\n\n- «frase1» \n- «frase2» \n- «frase3»\n\nSostituirà la lista corrente. Puoi visualizzare le frasi impostate con /seelines");
			} else if (allLineStateMachine.isCurrentState(setting)
					&& player.getCaptionGenerator().validateCustomCaptions(messageText)) {
				player.getCaptionGenerator().setCustomCaptions(messageText);
				allLineStateMachine.change(setEvent);
				message.setText("Frasi impostate");
			} else if (allLineStateMachine.isCurrentState(setting)) {
				message.setText("In attesa della lista");
			}
		} else if (messageText.equals("/addline") || singeLineStateMachine.isCurrentState(setting)) {
			if (messageText.equals("/addline") && singeLineStateMachine.isCurrentState(setted)) {
				singeLineStateMachine.change(resetEvent);
			}
			if (singeLineStateMachine.isCurrentState(not_setted)) {
				singeLineStateMachine.change(settingEvent);
				message.setText(
						"Scrivi la frase che vuoi aggiungere, fai attenzione, non è possibile eliminare le frasi inserite se non ricaricando la lista precedente, ti consiglio di tenerne una copia ;)");
			} else if (singeLineStateMachine.isCurrentState(setting)) {
				player.getCaptionGenerator().addCaption(messageText);
				singeLineStateMachine.change(setEvent);
				message.setText("Frase aggiunta");
			}
		} else if (messageText.equals("/removename")) {
			if (!player.getName().isBlank()) {
				player.setName("");
				nameStateMachine.change(resetEvent);
				message.setText("Nome resettato");
			} else {
				message.setText(
						"Nome non presente usare il comando /setname per impostarlo");
			}
		} else if (messageText.equals("/removebg")) {
			if (!player.getBackground().isBlank()) {
				player.setBackground("");
				backgroundStateMachine.change(resetEvent);
				message.setText("Background resettato");
			} else {
				message.setText(
						"Background non presente usare il comando /setbg per impostarlo");
			}
		} else if (messageText.equals("/removeimage")) {
			if (!player.getImage().isBlank()) {
				player.setImage("");
				player.setImgbbUrl("");
				imageStateMachine.change(resetEvent);
				message.setText("immagine resettata");
			} else {
				message.setText(
						"Immagine non presente usare il comando /setimage per impostarla");
			}
		} else if (messageText.equals("/player")) {
			player = _players.get(targetUserId);
			message.setChatId(chatId);
			message.setReplyToMessageId(targetMessageId);
			toRemove = true;
			message.setText("Il giocatore è: " + player.getName());
		} else if (messageText.equals("/seelines")) {
			message.setText(player.getCaptionGenerator().getAllFormatted());
		} else if (messageText.equals("/resetlines")) {
			player.getCaptionGenerator().resetToDefault();
			message.setText("Le frasi sono state reimpostate a quelle di default");
		} else if (messageText.equals("/exit") && userId == ADMIN_USER_ID) {
			message.setText("Server chiuso, stati salvati");
			System.exit(0);
		}

		bot.set_players(_players);
		Message messageSent = null;
		if (message.getText() != null && !message.getText().trim().isEmpty()) {
			messageSent = bot.execute(message);
		} 
		if (toRemove && messageSent!=null) {
			Utilities.makeRemovable(bot, chatId + "", messageSent.getMessageId(), 10);
			Utilities.makeRemovable(bot, chatId + "", messageId, 10); // TODO da capire se ha senso rimuovere sia la
																		// risposta sia la richiesta
			toRemove = false;
		}

	}

}
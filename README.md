# Nat20Bot

Nat20Bot is a Telegram bot built in **Java** with the [rubenlagus/TelegramBots](https://github.com/rubenlagus/TelegramBots) library.  
It is designed as a companion tool for tabletop role-playing sessions, focusing on **character management** and **group immersion**.

---

## Features

- **Character Management**  
  - Set and update your character information (name, background, profile picture).  
  - Retrieve character sheets or info at any time.  

- **Group Awareness**  
  - View the list of other players’ character names in group chats.  
  - Easily identify who is playing which character.  

- **Role-Play Enhancements**  
  - Customize phrases your character says (e.g., catchphrases, quotes).  
  - Automatically speak as your character instead of your personal Telegram account.  

- **Additional Utilities**  
  - Lightweight and fast, built for quick interactions during gameplay.  
  - Modular design, making it easy to expand with new RPG-related commands.  

---

## Example Usage

In a group or private chat with Nat20Bot:

- `/setname` → sets your character’s name  
- `/setbg` → sets a character backstory  
- `/setimage` → upload a photo to be used as your character’s avatar  
- `/setlines"` → sends the phrase as if spoken by your character  

---

## 🛠️ Technology

- **Language:** Java 16  
- **Framework:** rubenlagus/TelegramBots  
- **Build tools:** Maven  

---

## 🧩 Core Classes

- **core/Main.java** — Application entry point; initializes and registers the bot (reads `BOT_TOKEN` env var).  
- **core/Nat20bot.java** — The bot itself (extends `TelegramLongPollingBot`); parses updates, routes commands, enforces group/DM behavior.  
- **core/Player.java** — Domain model for player/character data (e.g., name, background, photo, custom lines).  
- **core/ExecThread.java** — Lightweight executor/queue for async actions (e.g., sending messages, background tasks).  
- **state_machines/StateMachines.java** — Conversational flows for multi-step commands (set name/background/photo/lines).  
- **utils/Backup.java** — Simple file-based backup/restore of in-memory data (periodic or on demand).  
- **utils/Utilities.java** — Shared helpers (parsing, validation, formatting).  

---

## 🔑 Environment Variables

- `BOT_TOKEN` → the Telegram API token provided by [@BotFather](https://t.me/BotFather)  
- `IMGBB_KEY` → API key for [ImgBB](https://api.imgbb.com/) used to upload and store character images  
- `ADMIN_USER_ID` → the Telegram user ID of the bot administrator (used for privileged commands or maintenance)
---

## 📜 License

MIT – free to use, modify, and distribute.  

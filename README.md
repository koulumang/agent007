# Agent007 - AI Personal Assistant Bot

A powerful Telegram bot powered by a local LLM (Ollama) with persistent memory, productivity features, weather updates, stock market tracking, expense logging, and more.

## Features

### 🤖 AI Conversation
- Natural language chat powered by Ollama (Llama 3.2)
- Persistent conversation memory across sessions
- Learns facts about you over time using AI-powered extraction
- Privacy-first — everything runs locally

### ⏰ Smart Reminders
- Natural language parsing — just say "remind me in 30 minutes to call mom"
- **Recurring reminders** — daily and weekly repeats
- **Snooze** — every reminder includes inline "Snooze 15 min" / "Snooze 1 hour" buttons
- View and cancel upcoming reminders with `/reminders`
- Examples:
  - `remind me tomorrow at 3pm about the meeting`
  - `remind me every day at 8am to drink water`
  - `remind me every monday at 9am about standup`
  - `remind me every week to review code`

### 📋 Task Management
- Add, complete, and delete tasks
- View pending or all tasks
- Simple task tracking with checkboxes

### 📝 Notes
- Save notes with titles and tags
- Search notes by tag or **full-text keyword search**
- Organize your thoughts and information

### 🌤️ Automatic Weather Updates
- Updates every 2 hours
- Default location: Atlanta
- AI-generated witty and practical weather tips
- Current conditions + 24-hour forecast
- Can be toggled on/off

### 📈 Stock Market Tracking
- Hourly updates during NYSE trading hours (9:30 AM – 4:00 PM ET, Mon–Fri)
- Default stocks: RBLX, TEAM, AMZN, NVDA
- Add/remove stocks from your watchlist
- Real-time price, change, and percentage
- **Price alerts** — get notified when a stock crosses your target

### 💸 Expense Tracker
- Log expenses with a description
- View monthly spending summaries
- Filter by any month (e.g. `/expenses 2026-03`)

### 🍅 Pomodoro Timer
- Start a focus session with `/pomodoro`
- Default 25 minutes; set a custom duration
- Fires a Telegram reminder when your session ends

### 🌅 Daily Digest
- Automatic morning summary at 8 AM
- Includes weather, pending tasks, today's reminders, stock prices, and monthly spend
- Also available on demand with `/digest`

## Prerequisites

1. **Java 21** — JDK 21 or higher
2. **Maven** — For building the project
3. **Ollama** — For running the LLM locally

## Setup

### 1. Install Ollama

```bash
# macOS/Linux
curl -fsSL https://ollama.com/install.sh | sh

# Pull the model
ollama pull llama3.2
```

### 2. Create Telegram Bot

1. Open Telegram and search for `@BotFather`
2. Send `/newbot` and follow the instructions
3. Copy the bot token you receive

### 3. Set Environment Variable

```bash
export TELEGRAM_BOT_TOKEN="your_token_here"
```

### 4. Build and Run

```bash
mvn clean compile exec:java -Dexec.mainClass="com.agent007.Main"
```

Or build a JAR:

```bash
mvn clean package
java -jar target/agent007-1.0-SNAPSHOT.jar
```

## Commands

### 📋 Tasks
- `/addtask <task>` — Add a new task
- `/tasks` — Show pending tasks
- `/alltasks` — Show all tasks
- `/done <id>` — Mark task as complete
- `/deltask <id>` — Delete a task

### ⏰ Reminders
- `/remind in 30 minutes to call mom`
- `/remind tomorrow at 3pm about meeting`
- `/remind at 5pm to go to gym`
- `/remind on monday at 9am about standup`
- `/remind every day at 8am to drink water`
- `/remind every monday at 9am to standup`
- `/remind every week to review code`
- `/reminders` — View upcoming reminders
- `/delreminder <id>` — Delete a reminder
- Or just say "remind me..." in conversation!

### 📝 Notes
- `/addnote <title> | <content> | <tags>`
- `/notes` — Show all notes
- `/notes <tag>` — Filter by tag
- `/search <keyword>` — Full-text search across all notes
- `/delnote <id>` — Delete a note

### 🌤️ Weather
- `/setlocation <city>` — Set your location (default: Atlanta)
- `/weather` — Check current weather
- `/weatheroff` — Disable auto updates
- `/weatheron` — Enable auto updates
- `/hourlyoff` — Disable hourly updates
- `/hourlyon` — Enable hourly updates

### 📈 Stocks
- `/addstock <SYMBOL>` — Add stock to watchlist
- `/stocks` — List tracked stocks
- `/delstock <SYMBOL>` — Remove a stock
- `/stocknow` — Get current prices
- `/alert <SYMBOL> above/below <price>` — Set a price alert
- `/alerts` — View active alerts
- `/delalert <id>` — Delete an alert

### 💸 Expenses
- `/expense <amount> <description>` — Log an expense
- `/expenses` — View this month's spending
- `/expenses 2026-03` — View a specific month
- `/delexpense <id>` — Delete an expense

### 🍅 Pomodoro
- `/pomodoro` — Start a 25-minute focus timer
- `/pomodoro <minutes>` — Start a custom-length timer

### 🌅 Digest
- `/digest` — Get your morning summary on demand

### 💬 General
- `/help` — Show all commands
- Chat normally for AI conversation!

## How It Works

1. You send a message on Telegram
2. Agent007 checks if it's a command or a reminder request
3. For commands: executes the action immediately
4. For conversations: retrieves history + facts, sends to Ollama, replies with AI response
5. Background schedulers handle:
   - **Reminders** — checked every 30 seconds; recurring ones re-schedule automatically
   - **Weather** — every 2 hours with AI-generated tips
   - **Stocks** — hourly during NYSE trading hours + real-time price alerts
   - **Digest** — daily at 8 AM

## Data Storage

All data is stored locally in SQLite (`agent007.db`) using WAL mode for reliability:
- Conversation history and learned facts
- Tasks, reminders (with recurrence), and notes
- User preferences (location, settings)
- Stock watchlists and price alerts
- Expense log

## Customization

- Change LLM model in `OllamaClient.java`
- Adjust weather update frequency in `WeatherScheduler.java`
- Modify stock update timing in `StockScheduler.java`
- Change digest time in `DigestScheduler.java` (default: 8 AM)
- Add more natural language patterns in `NaturalReminderParser.java`

## Privacy & Security

- All AI processing happens locally via Ollama
- No cloud APIs for conversations
- Your data never leaves your machine
- Stock and weather data fetched from public APIs

## Tech Stack

- Java 21
- Telegram Bots API 6.8
- Ollama (Llama 3.2)
- SQLite (WAL mode)
- Jackson (JSON)
- Yahoo Finance API (stocks)
- Open-Meteo API (weather)

## License

MIT License — feel free to use and modify!

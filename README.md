# Agent007 - AI Personal Assistant Bot

A powerful Telegram bot powered by local LLM (Ollama) with persistent memory, productivity features, weather updates, and stock market tracking.

## Features

### 🤖 AI Conversation
- Natural language chat powered by Ollama (Mistral)
- Persistent conversation memory
- Learns facts about you over time
- Privacy-first - everything runs locally

### ⏰ Smart Reminders
- Natural language parsing - just say "remind me in 30 minutes to call mom"
- Supports: minutes, hours, days, specific times, days of week
- Examples:
  - "remind me tomorrow at 3pm about the meeting"
  - "remind me on monday at 9am about standup"
  - "remind me next week to review code"

### 📋 Task Management
- Add, complete, and delete tasks
- View pending or all tasks
- Simple task tracking with checkboxes

### 📝 Notes
- Save notes with titles and tags
- Search notes by tags
- Organize your thoughts and information

### 🌤️ Automatic Weather Updates
- Updates every 2 hours
- Default location: Atlanta
- AI-generated witty and practical weather tips
- Current conditions + 24-hour forecast
- Can be toggled on/off

### 📈 Stock Market Tracking
- Hourly updates during NYSE trading hours (9:30 AM - 4:00 PM ET, Mon-Fri)
- Default stocks: RBLX, TEAM, AMZN, NVDA
- Add/remove stocks from your watchlist
- Real-time price, change, and percentage
- Market open/closed detection

## Prerequisites

1. **Java 21** - JDK 21 or higher
2. **Maven** - For building the project
3. **Ollama** - For running the LLM locally

## Setup

### 1. Install Ollama

```bash
# macOS/Linux
curl -fsSL https://ollama.com/install.sh | sh

# Pull the Mistral model
ollama pull mistral
```

### 2. Create Telegram Bot

1. Open Telegram and search for `@BotFather`
2. Send `/newbot` and follow instructions
3. Copy the bot token you receive

### 3. Set Environment Variable

```bash
export TELEGRAM_BOT_TOKEN="your_token_here"
```

### 4. Build and Run

```bash
# Build and run
mvn clean compile exec:java -Dexec.mainClass="com.agent007.Main"
```

Or build a JAR:

```bash
mvn clean package
java -jar target/agent007-1.0-SNAPSHOT.jar
```

## Commands

### 📋 Tasks
- `/addtask <task>` - Add a new task
- `/tasks` - Show pending tasks
- `/alltasks` - Show all tasks
- `/done <id>` - Mark task as complete
- `/deltask <id>` - Delete a task

### ⏰ Reminders
- `/remind in 30 minutes to call mom`
- `/remind tomorrow at 3pm about meeting`
- `/remind at 5pm to go to gym`
- `/remind on monday at 9am about standup`
- Or just say "remind me..." in conversation!

### 📝 Notes
- `/addnote <title> | <content> | <tags>`
- `/notes` - Show all notes
- `/notes <tag>` - Filter by tag
- `/delnote <id>` - Delete a note

### 🌤️ Weather
- `/setlocation <city>` - Set your location (default: Atlanta)
- `/weather` - Check current weather
- `/weatheroff` - Disable auto updates
- `/weatheron` - Enable auto updates
- `/hourlyoff` - Disable hourly updates
- `/hourlyon` - Enable hourly updates

### 📈 Stocks
- `/addstock <SYMBOL>` - Add stock to watchlist
- `/stocks` - List tracked stocks
- `/delstock <SYMBOL>` - Remove stock
- `/stocknow` - Get current prices

### 💬 General
- `/help` - Show all commands
- Chat normally for AI conversation!

## How It Works

1. You send a message on Telegram
2. Agent007 checks if it's a command or reminder request
3. For commands: executes the action immediately
4. For conversations: retrieves history + facts, sends to Ollama, replies with AI response
5. Background schedulers handle:
   - Reminders (checked every 30 seconds)
   - Weather updates (every 2 hours with AI-generated tips)
   - Stock updates (hourly during NYSE trading hours)

## Data Storage

All data is stored locally in SQLite (`agent007.db`):
- Conversation history and learned facts
- Tasks, reminders, and notes
- User preferences (location, settings)
- Stock watchlists

## Customization

- Change LLM model in `OllamaClient.java`
- Adjust weather update frequency in `WeatherScheduler.java`
- Modify stock update timing in `StockScheduler.java`
- Add more natural language patterns in `NaturalReminderParser.java`

## Privacy & Security

- All AI processing happens locally via Ollama
- No cloud APIs for conversations
- Your data never leaves your machine
- Stock and weather data fetched from public APIs

## Tech Stack

- Java 21
- Telegram Bots API
- Ollama (Mistral LLM)
- SQLite
- Jackson (JSON)
- Yahoo Finance API (stocks)
- Open-Meteo API (weather)

## License

MIT License - feel free to use and modify!

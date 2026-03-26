# Agent007 - Personal AI Assistant

A Telegram bot powered by local LLM (Ollama) with persistent memory.

## Prerequisites

1. **Java 21** - Make sure you have JDK 21 installed
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
# Build
mvn clean package

# Run
java -jar target/agent007-1.0-SNAPSHOT.jar
```

Or simply:

```bash
mvn clean compile exec:java -Dexec.mainClass="com.agent007.Main"
```

## Features

- **Conversational AI** - Chat naturally with local LLM
- **Short-term memory** - Remembers conversation context
- **Long-term memory** - Learns facts about you over time
- **Privacy-first** - Everything runs locally, no cloud APIs

## How It Works

1. You send a message on Telegram
2. Agent007 retrieves conversation history + learned facts
3. Sends context to Ollama (local LLM)
4. Replies back with AI-generated response
5. Stores the conversation in SQLite

## Memory System

- **Messages table** - Stores all conversation history
- **Facts table** - Extracts and stores personal information
- Facts are automatically detected from phrases like "my name is", "I like", "I prefer"

## Customization

- Change LLM model in `OllamaClient.java` (line 12)
- Adjust conversation history limit in `MemoryStore.java` (line 62)
- Modify fact extraction logic in `extractAndSaveFacts()` method

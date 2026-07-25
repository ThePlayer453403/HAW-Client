package com.haw.client.object;

public class CommandSuggestionRequest {
    public String chatCommand;
    public boolean outputToChat;
    public long timestamp;

    public CommandSuggestionRequest(String command) {
        this.chatCommand = formatCommmand(command);
        this.outputToChat = false;
    }

    public CommandSuggestionRequest(String command, boolean outputToChat) {
        this.chatCommand = formatCommmand(command);
        this.outputToChat = outputToChat;
    }

    public static String formatCommmand(String command) {
        if (!command.startsWith("/")) {command = "/" + command;}
        if (!command.endsWith(" ")) {command = command + " ";}
        return command;
    }
}

package backend.academy.linktracker.bot.command;

public enum CommandInfo {
    START("/start", "Запуск бота"),
    HELP("/help", "Вывод списка доступных команд");

    private final String command;
    private final String description;

    CommandInfo(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public String command() {
        return command;
    }

    public String description() {
        return description;
    }
}

package org.example.server.commands;

import org.example.authentication.UserManager;
import org.example.network.Request;
import org.example.network.Response;

/**
 * Команда для регистрации нового пользователя.
 * Не требует предварительной аутентификации.
 */
public class RegisterCommand extends Command {
    private final UserManager userManager;

    public RegisterCommand(UserManager userManager) {
        super("register", "Команда для регистрации нового пользователя.");
        this.userManager = userManager;
    }

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public String getDescription() {
        return "регистрирует нового пользователя: register <username> <password>";
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) {
        // Для команды register authenticatedUsername всегда будет null на этом этапе,
        // так как аутентификация происходит после получения запроса.
        // username и password уже доступны в объекте Request.
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return new Response("Ошибка: Для регистрации требуются имя пользователя и пароль.");
        }

        if (userManager.registerUser(username, password)) {
            return new Response("Пользователь '" + username + "' успешно зарегистрирован. Войдите в систему с помощью login.");
        } else {
            return new Response("Ошибка регистрации: Пользователь с таким именем уже существует или произошла внутренняя ошибка.");
        }
    }
}
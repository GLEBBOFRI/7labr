package org.example.server.commands;

import org.example.authentication.UserManager;
import org.example.network.Request;
import org.example.network.Response;

/**
 * Команда для входа пользователя в систему.
 * Не требует предварительной аутентификации, сама её выполняет.
 */
public class LoginCommand extends Command {
    private final UserManager userManager;

    public LoginCommand(UserManager userManager) {
        super("login", "Команда для входа пользователя в систему.");
        this.userManager = userManager;
    }

    @Override
    public String getName() {
        return "login";
    }

    @Override
    public String getDescription() {
        return "выполняет вход в систему: login <username> <password>";
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) {
        // Для команды login authenticatedUsername всегда будет null на этом этапе,
        // так как аутентификация происходит после получения запроса.
        // username и password уже доступны в объекте Request.
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return new Response("Ошибка: Для входа требуются имя пользователя и пароль.");
        }

        if (userManager.authenticateUser(username, password)) {
            return new Response("Успешный вход в систему. Добро пожаловать, " + username + "!");
        } else {
            return new Response("Ошибка входа: Неверное имя пользователя или пароль.");
        }
    }
}

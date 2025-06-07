package org.example.authentication;

import org.example.database.DatabaseManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Управляет регистрацией и аутентификацией пользователей, используя SHA-224 для хэширования паролей.
 */
public class UserManager {
    private static final Logger logger = Logger.getLogger(UserManager.class.getName());
    private final DatabaseManager databaseManager;

    public UserManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Хэширует пароль с использованием алгоритма SHA-224.
     * @param password Пароль для хэширования.
     * @return Хэшированный пароль в виде шестнадцатеричной строки.
     * @throws NoSuchAlgorithmException Если алгоритм SHA-224 недоступен.
     */
    public String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-224");
        byte[] hashBytes = md.digest(password.getBytes());

        // Преобразование байтов в шестнадцатеричную строку
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Регистрирует нового пользователя.
     * @param username Имя пользователя.
     * @param password Пароль пользователя (будет хэширован).
     * @return true, если регистрация прошла успешно, false, если пользователь с таким именем уже существует.
     */
    public boolean registerUser(String username, String password) {
        try {
            String hashedPassword = hashPassword(password);
            return databaseManager.registerUser(username, hashedPassword);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при регистрации пользователя: " + e.getMessage(), e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Ошибка алгоритма хэширования: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Аутентифицирует пользователя.
     * @param username Имя пользователя.
     * @param password Пароль для проверки.
     * @return true, если учетные данные верны, false в противном случае.
     */
    public boolean authenticateUser(String username, String password) {
        try {
            String hashedPassword = hashPassword(password);
            return databaseManager.checkUserCredentials(username, hashedPassword);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при аутентификации пользователя: " + e.getMessage(), e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Ошибка алгоритма хэширования: " + e.getMessage(), e);
            return false;
        }
    }
}

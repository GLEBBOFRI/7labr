package org.example.database;

import org.example.database.models.*;
import java.sql.*;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private static final String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "db";
    private static final String DB_PASSWORD = "bNVGnX8JfDmFB9Xg";
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":5432/studs";
    private static final String DB_USER = "s465457";

    private static final String CREATE_USERS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(255) PRIMARY KEY,
                password_hash VARCHAR(255) NOT NULL
            );
            """;

    private static final String CREATE_CITIES_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS cities (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                area INTEGER NOT NULL CHECK (area > 0),
                population BIGINT NOT NULL CHECK (population > 0),
                meters_above_sea_level REAL,
                climate VARCHAR(50) NOT NULL,
                government VARCHAR(50),
                standard_of_living VARCHAR(50) NOT NULL,
                owner_id VARCHAR(255) NOT NULL REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE
            );
            """;

    private static final String CREATE_COORDINATES_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS coordinates (
                city_id INTEGER PRIMARY KEY REFERENCES cities(id) ON DELETE CASCADE,
                x INTEGER NOT NULL CHECK (x > -81),
                y BIGINT NOT NULL
            );
            """;

    private static final String CREATE_GOVERNORS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS governors (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            );
            """;

    private static final String ADD_GOVERNOR_FK_TO_CITIES_SQL = """
            ALTER TABLE cities
            ADD COLUMN IF NOT EXISTS governor_id INTEGER REFERENCES governors(id) ON DELETE SET NULL;
            """;

    private Connection connection;

    public DatabaseManager() throws SQLException {
        connect();
        createTablesIfNotExist();
    }

    private void connect() throws SQLException {
        try {
            logger.info("Попытка подключения к БД: URL=" + DB_URL + ", USER=" + DB_USER + ", PASSWORD_LENGTH=" + DB_PASSWORD.length());
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);
            logger.info("Успешное подключение к базе данных PostgreSQL.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка подключения к базе данных: " + e.getMessage(), e);
            throw e;
        }
    }

    public void createTablesIfNotExist() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_USERS_TABLE_SQL);
            stmt.execute(CREATE_CITIES_TABLE_SQL);
            stmt.execute(CREATE_COORDINATES_TABLE_SQL);
            stmt.execute(CREATE_GOVERNORS_TABLE_SQL);
            stmt.execute(ADD_GOVERNOR_FK_TO_CITIES_SQL);
            connection.commit();
            logger.info("Таблицы базы данных успешно созданы или уже существуют.");
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при создании таблиц: " + e.getMessage(), e);
            throw e;
        }
    }

    public Hashtable<Integer, City> loadCities() throws SQLException {
        Hashtable<Integer, City> cities = new Hashtable<>();
        String selectSql = """
            SELECT
                c.id, c.name, c.creation_date, c.area, c.population, c.meters_above_sea_level,
                c.climate, c.government, c.standard_of_living, c.owner_id,
                coord.x, coord.y,
                g.name AS governor_name
            FROM cities c
            JOIN coordinates coord ON c.id = coord.city_id
            LEFT JOIN governors g ON c.governor_id = g.id
            ORDER BY c.id;
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String name = rs.getString("name");
                Date creationDate = rs.getTimestamp("creation_date");
                Integer area = rs.getInt("area");
                Long population = rs.getLong("population");
                Float metersAboveSeaLevel = rs.getObject("meters_above_sea_level", Float.class);
                Climate climate = Climate.valueOf(rs.getString("climate"));
                Government government = rs.getString("government") != null ? Government.valueOf(rs.getString("government")) : null;
                StandardOfLiving standardOfLiving = StandardOfLiving.valueOf(rs.getString("standard_of_living"));
                String ownerId = rs.getString("owner_id");

                Integer coordX = rs.getInt("x");
                Long coordY = rs.getObject("y", Long.class);
                Coordinates coordinates = new Coordinates(coordX, coordY);

                Human governor = rs.getString("governor_name") != null ? new Human(rs.getString("governor_name")) : null;

                City city = new City(id, name, coordinates, creationDate, area, population,
                        metersAboveSeaLevel, climate, government, standardOfLiving, governor, ownerId);
                cities.put(id, city);
            }
            logger.info("Коллекция городов успешно загружена из базы данных.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при загрузке городов из БД: " + e.getMessage(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Ошибка при парсинге ENUM из БД: " + e.getMessage(), e);
            throw new SQLException("Некорректное значение ENUM в базе данных.", e);
        }
        return cities;
    }

    public int insertCity(City city, String ownerId) throws SQLException {
        String insertCitySql = """
            INSERT INTO cities (name, area, population, meters_above_sea_level, climate, government, standard_of_living, owner_id, governor_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;
            """;
        String insertCoordSql = "INSERT INTO coordinates (city_id, x, y) VALUES (?, ?, ?);";
        String insertGovernorSql = "INSERT INTO governors (name) VALUES (?) RETURNING id;";

        int cityId = -1;
        Integer governorId = null;

        try (PreparedStatement pstmtCity = connection.prepareStatement(insertCitySql);
             PreparedStatement pstmtCoord = connection.prepareStatement(insertCoordSql);
             PreparedStatement pstmtGovernor = connection.prepareStatement(insertGovernorSql)) {

            if (city.getGovernor() != null) {
                pstmtGovernor.setString(1, city.getGovernor().getName());
                try (ResultSet rs = pstmtGovernor.executeQuery()) {
                    if (rs.next()) {
                        governorId = rs.getInt(1);
                    }
                }
            }

            pstmtCity.setString(1, city.getName());
            pstmtCity.setInt(2, city.getArea());
            pstmtCity.setLong(3, city.getPopulation());
            if (city.getMetersAboveSeaLevel() != null) {
                pstmtCity.setFloat(4, city.getMetersAboveSeaLevel());
            } else {
                pstmtCity.setNull(4, java.sql.Types.REAL);
            }
            pstmtCity.setString(5, city.getClimate().name());
            if (city.getGovernment() != null) {
                pstmtCity.setString(6, city.getGovernment().name());
            } else {
                pstmtCity.setNull(6, java.sql.Types.VARCHAR);
            }
            pstmtCity.setString(7, city.getStandardOfLiving().name());
            pstmtCity.setString(8, ownerId);
            if (governorId != null) {
                pstmtCity.setInt(9, governorId);
            } else {
                pstmtCity.setNull(9, java.sql.Types.INTEGER);
            }

            try (ResultSet rs = pstmtCity.executeQuery()) {
                if (rs.next()) {
                    cityId = rs.getInt(1);
                }
            }

            pstmtCoord.setInt(1, cityId);
            pstmtCoord.setInt(2, city.getCoordinates().getX());
            if (city.getCoordinates().getY() != null) {
                pstmtCoord.setLong(3, city.getCoordinates().getY());
            } else {
                pstmtCoord.setNull(3, java.sql.Types.BIGINT);
            }
            pstmtCoord.executeUpdate();

            connection.commit();
            logger.info("Город '" + city.getName() + "' успешно вставлен в БД с ID: " + cityId);
            return cityId;
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при вставке города в БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean updateCity(City city, String ownerId) throws SQLException {
        String updateCitySql = """
            UPDATE cities SET
                name = ?, area = ?, population = ?, meters_above_sea_level = ?,
                climate = ?, government = ?, standard_of_living = ?, governor_id = ?
            WHERE id = ? AND owner_id = ?;
            """;
        String updateCoordSql = """
            UPDATE coordinates SET
                x = ?, y = ?
            WHERE city_id = ?;
            """;
        String insertGovernorSql = "INSERT INTO governors (name) VALUES (?) RETURNING id;";
        String updateGovernorSql = "UPDATE governors SET name = ? WHERE id = ?;";

        Integer governorId = null;
        Integer existingGovernorId = null;

        try (PreparedStatement pstmtCity = connection.prepareStatement(updateCitySql);
             PreparedStatement pstmtCoord = connection.prepareStatement(updateCoordSql);
             PreparedStatement pstmtInsertGovernor = connection.prepareStatement(insertGovernorSql);
             PreparedStatement pstmtUpdateGovernor = connection.prepareStatement(updateGovernorSql);
             PreparedStatement pstmtGetGovernorId = connection.prepareStatement("SELECT governor_id FROM cities WHERE id = ?")) {

            pstmtGetGovernorId.setInt(1, city.getId());
            try (ResultSet rs = pstmtGetGovernorId.executeQuery()) {
                if (rs.next()) {
                    existingGovernorId = rs.getObject("governor_id", Integer.class);
                }
            }

            if (city.getGovernor() != null) {
                if (existingGovernorId != null) {
                    pstmtUpdateGovernor.setString(1, city.getGovernor().getName());
                    pstmtUpdateGovernor.setInt(2, existingGovernorId);
                    pstmtUpdateGovernor.executeUpdate();
                    governorId = existingGovernorId;
                } else {
                    pstmtInsertGovernor.setString(1, city.getGovernor().getName());
                    try (ResultSet rs = pstmtInsertGovernor.executeQuery()) {
                        if (rs.next()) {
                            governorId = rs.getInt(1);
                        }
                    }
                }
            } else {
                if (existingGovernorId != null) {
                    try (PreparedStatement pstmtDeleteGovernor = connection.prepareStatement("DELETE FROM governors WHERE id = ?")) {
                        pstmtDeleteGovernor.setInt(1, existingGovernorId);
                        pstmtDeleteGovernor.executeUpdate();
                    }
                }
                governorId = null;
            }

            pstmtCity.setString(1, city.getName());
            pstmtCity.setInt(2, city.getArea());
            pstmtCity.setLong(3, city.getPopulation());
            if (city.getMetersAboveSeaLevel() != null) {
                pstmtCity.setFloat(4, city.getMetersAboveSeaLevel());
            } else {
                pstmtCity.setNull(4, java.sql.Types.REAL);
            }
            pstmtCity.setString(5, city.getClimate().name());
            if (city.getGovernment() != null) {
                pstmtCity.setString(6, city.getGovernment().name());
            } else {
                pstmtCity.setNull(6, java.sql.Types.VARCHAR);
            }
            pstmtCity.setString(7, city.getStandardOfLiving().name());
            if (governorId != null) {
                pstmtCity.setInt(8, governorId);
            } else {
                pstmtCity.setNull(8, java.sql.Types.INTEGER);
            }
            pstmtCity.setInt(9, city.getId());
            pstmtCity.setString(10, ownerId);

            int affectedRows = pstmtCity.executeUpdate();
            if (affectedRows == 0) {
                connection.rollback();
                logger.warning("Не удалось обновить город с ID " + city.getId() + " для владельца " + ownerId + ". Возможно, город не найден или вы не являетесь владельцем.");
                return false;
            }

            pstmtCoord.setInt(1, city.getCoordinates().getX());
            if (city.getCoordinates().getY() != null) {
                pstmtCoord.setLong(2, city.getCoordinates().getY());
            } else {
                pstmtCoord.setNull(2, java.sql.Types.BIGINT);
            }
            pstmtCoord.setInt(3, city.getId());
            pstmtCoord.executeUpdate();

            connection.commit();
            logger.info("Город с ID " + city.getId() + " успешно обновлен в БД.");
            return true;
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при обновлении города в БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean deleteCity(int cityId, String ownerId) throws SQLException {
        String deleteSql = "DELETE FROM cities WHERE id = ? AND owner_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setInt(1, cityId);
            pstmt.setString(2, ownerId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                connection.commit();
                logger.info("Город с ID " + cityId + " успешно удален из БД.");
                return true;
            } else {
                connection.rollback();
                logger.warning("Не удалось удалить город с ID " + cityId + " для владельца " + ownerId + ". Возможно, город не найден или вы не являетесь владельцем.");
                return false;
            }
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при удалении города из БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public int clearCities(String ownerId) throws SQLException {
        String deleteSql = "DELETE FROM cities WHERE owner_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setString(1, ownerId);
            int affectedRows = pstmt.executeUpdate();
            connection.commit();
            logger.info(affectedRows + " городов удалено из БД для владельца " + ownerId + ".");
            return affectedRows;
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при очистке городов для владельца " + ownerId + " из БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public int deleteCitiesGreaterThanKey(Integer key, String ownerId) throws SQLException {
        String deleteSql = "DELETE FROM cities WHERE id > ? AND owner_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setInt(1, key);
            pstmt.setString(2, ownerId);
            int affectedRows = pstmt.executeUpdate();
            connection.commit();
            logger.info(affectedRows + " городов с ключом > " + key + " удалено из БД для владельца " + ownerId + ".");
            return affectedRows;
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при удалении городов с ключом > " + key + " для владельца " + ownerId + " из БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public int deleteCitiesLowerThanKey(Integer key, String ownerId) throws SQLException {
        String deleteSql = "DELETE FROM cities WHERE id < ? AND owner_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setInt(1, key);
            pstmt.setString(2, ownerId);
            int affectedRows = pstmt.executeUpdate();
            connection.commit();
            logger.info(affectedRows + " городов с ключом < " + key + " удалено из БД для владельца " + ownerId + ".");
            return affectedRows;
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при удалении городов с ключом < " + key + " для владельца " + ownerId + " из БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public int deleteCitiesByStandardOfLiving(StandardOfLiving standardOfLiving, String ownerId) throws SQLException {
        String deleteSql = "DELETE FROM cities WHERE standard_of_living = ? AND owner_id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setString(1, standardOfLiving.name());
            pstmt.setString(2, ownerId);
            int affectedRows = pstmt.executeUpdate();
            connection.commit();
            logger.info(affectedRows + " городов со standardOfLiving " + standardOfLiving + " удалено из БД для владельца " + ownerId + ".");
            return affectedRows;
        } catch (SQLException e) {
            connection.rollback();
            logger.log(Level.SEVERE, "Ошибка при удалении городов по standardOfLiving для владельца " + ownerId + " из БД: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean registerUser(String username, String passwordHash) throws SQLException {
        String insertUserSql = "INSERT INTO users (username, password_hash) VALUES (?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUserSql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
            connection.commit();
            logger.info("Пользователь '" + username + "' успешно зарегистрирован.");
            return true;
        } catch (SQLException e) {
            connection.rollback();
            if (e.getSQLState().equals("23505")) {
                logger.warning("Пользователь с именем '" + username + "' уже существует.");
                return false;
            }
            logger.log(Level.SEVERE, "Ошибка при регистрации пользователя '" + username + "': " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean checkUserCredentials(String username, String passwordHash) throws SQLException {
        String selectSql = "SELECT password_hash FROM users WHERE username = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    return storedHash.equals(passwordHash);
                }
            }
            return false;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при проверке учетных данных пользователя '" + username + "': " + e.getMessage(), e);
            throw e;
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Соединение с базой данных закрыто.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Ошибка при закрытии соединения с базой данных: " + e.getMessage(), e);
            }
        }
    }
}

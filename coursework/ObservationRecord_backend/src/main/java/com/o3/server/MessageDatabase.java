package com.o3.server;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.sql.PreparedStatement;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class MessageDatabase {
    private static MessageDatabase dbInstance = null;
    private Connection dbConnection = null;
    private SecureRandom secureRandom = new SecureRandom();

    public static synchronized MessageDatabase getInstance() {
		if (null == dbInstance) {
			dbInstance = new MessageDatabase();
		}
        return dbInstance;
    }

    private MessageDatabase() {}

    public void open(String dbName) throws SQLException {
        // Open the database
        boolean dbExists = false;
        File dbFile = new File(dbName);
        if (dbFile.exists() && !dbFile.isDirectory()) {
            dbExists = true;
        }

        String url = "jdbc:sqlite:" + dbName;

        dbConnection = DriverManager.getConnection(url);

        if (!dbExists) {
            initializeDatabase(dbConnection);
        }
    }

    private boolean initializeDatabase(Connection dbConnection) throws SQLException {
        // Create the user registration table
        if (dbConnection != null) {
        String createUserTable = "CREATE TABLE users (" +
                     "username TEXT PRIMARY KEY, " +
                     "password TEXT NOT NULL, " +
                     "email TEXT NOT NULL, " +
                     "userNickname TEXT)";

        String createMessageTable = "CREATE TABLE messages (" +
                    "recordId INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                    "username TEXT, " +
                    "recordIdentifier TEXT NOT NULL, " +
                    "recordDescription TEXT, " +
                    "recordPayload TEXT, " +
                    "recordRightAscension TEXT,  " +
                    "recordDeclination TEXT, " +
                    "recordOwner TEXT, " +
                    "recordTimeReceived INTEGER NOT NULL, " +
                    "updateReason TEXT DEFAULT 'N/A', " +
                    "modified INTEGER, " +
                    "decipherShift INTEGER DEFAULT 0, " +
                    "view INTEGER DEFAULT 0, " +
                    "averageRating DOUBLE DEFAULT -1, " +
                    "FOREIGN KEY (username) REFERENCES users(username))";

        String createObservatoryTable = "CREATE TABLE observatories (" +
            "recordId INTEGER, " +
            "observatoryName TEXT, " +
            "latitude DOUBLE, " +
            "longitude DOUBLE, " +
            "temperatureInKelvins FLOAT, " +
            "cloudinessPercentance FLOAT, " +
            "bagroundLightVolume DOUBLE, "+
            "FOREIGN KEY (recordId) REFERENCES messages(recordId) ON DELETE CASCADE)";

        String createCommentTable = "CREATE TABLE comments (" +
            "commentId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "recordId INTEGER NOT NULL, " +
            "username TEXT NOT NULL, " +
            "userNickname TEXT NOT NULL, " +
            "commentText TEXT NOT NULL, " +
            "commentTime INTEGER NOT NULL, " +
            "FOREIGN KEY (recordId) REFERENCES messages(recordId) ON DELETE CASCADE, " +
            "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE)";

        String createRatingTable = "CREATE TABLE ratings (" +
            "ratingId INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "recordId INTEGER NOT NULL, " +
            "username TEXT NOT NULL, " +
            "rating INTEGER CHECK (rating >= 1 AND rating <= 5), " +  // Rating scale 1 to 5
            "FOREIGN KEY (recordId) REFERENCES messages(recordId) ON DELETE CASCADE, " +
            "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE)";

        Statement createStatement = dbConnection.createStatement();
        createStatement.executeUpdate(createUserTable);
        createStatement.executeUpdate(createMessageTable);
        createStatement.executeUpdate(createObservatoryTable);
        createStatement.executeUpdate(createCommentTable);
        createStatement.executeUpdate(createRatingTable);

        createStatement.close();
        System.out.println("DB successfully created");
        return true;
        }
        return false;
    }


    public boolean setUser(User user) throws SQLException {

        if(checkIfUserExists(user.getUsername())){
            return false;
        }

        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes); 

        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes; 
        String hashedPassword = Crypt.crypt(user.getPassword(), salt);
        
		String setUserString = "insert into users " +
					"VALUES('" + user.getUsername()+ "','" + hashedPassword + "','" + user.getEmail() + "','" + user.getUserNickname() + "')"; 
		Statement createStatement;
		createStatement = dbConnection.createStatement();
		createStatement.executeUpdate(setUserString);
		createStatement.close();

        return true;
    }

    public boolean checkIfUserExists(String givenUserName) throws SQLException{

        Statement queryStatement = null;
        ResultSet rs;

        String checkUser = "select username from users where username = '" + givenUserName + "'";
        System.out.println("checking user");

        
        queryStatement = dbConnection.createStatement();
		rs = queryStatement.executeQuery(checkUser);
        
        if(rs.next()){
            System.out.println("user exists");
            return true;
        }else{
            return false;
        }
    }

    public boolean authenticateUser(String givenUserName, String givenPassword) throws SQLException {

        Statement queryStatement = null;
        ResultSet rs;

        String getMessagesString = "select username, password from users where username = '" + givenUserName + "'";


        queryStatement = dbConnection.createStatement();
		rs = queryStatement.executeQuery(getMessagesString);

        if(rs.next() == false){

            System.out.println("cannot find such user");
            return false;

        }else{

            String hashedPassword = rs.getString("password");

            if (hashedPassword.equals(Crypt.crypt(givenPassword, hashedPassword))) {
                return true; // user authenticated
            } else {
                return false; // user not authenticated
            }

        }

    }

    public String getUserNickname(String username) throws SQLException {
        String getUserNicknameString = "SELECT userNickname FROM users WHERE username = ?";
        PreparedStatement queryStatement = dbConnection.prepareStatement(getUserNicknameString);
        queryStatement.setString(1, username);
        ResultSet rs = queryStatement.executeQuery();

        String userNickname = null;
        if (rs.next()) {
            userNickname = rs.getString("userNickname");
        }

        rs.close();
        queryStatement.close();
        return userNickname;
    }

    public boolean setMessage(ObservationRecord message, String username, int decipherShift) throws SQLException {
        String setMessageString = "INSERT INTO messages (username, recordIdentifier, recordDescription, recordPayload, recordRightAscension, recordDeclination, recordOwner, recordTimeReceived, decipherShift) " +
            "VALUES (?,?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement createStatement = dbConnection.prepareStatement(setMessageString);
        createStatement.setString(1, username);
        createStatement.setString(2, message.getRecordIdentifier());
        createStatement.setString(3, message.getRecordDescription());
        createStatement.setString(4, message.getRecordPayload());
        createStatement.setString(5, message.getRecordRightAscension());
        createStatement.setString(6, message.getRecordDeclination());
        createStatement.setString(7, message.getRecordOwner());
        createStatement.setLong(8, message.dateAsInt());
        createStatement.setInt(9, decipherShift);
        createStatement.executeUpdate();
        createStatement.close();

        //get the last recordId
        String getLastRecordIdString = "SELECT recordId FROM messages ORDER BY recordId DESC LIMIT 1";
        Statement lastRecordIdStatement = dbConnection.createStatement();
        ResultSet lastRecordIdRS = lastRecordIdStatement.executeQuery(getLastRecordIdString);
        lastRecordIdRS.next();
        int lastRecordId = lastRecordIdRS.getInt("recordId");
        lastRecordIdRS.close();
        lastRecordIdStatement.close();

        if (message.getObservatory() != null) {
            JSONArray observatories = new JSONArray(message.getObservatory());
            JSONArray observatoryWeather = new JSONArray(message.getObservatoryWeather() != null ? message.getObservatoryWeather() : "[]");
            for (int i = 0; i < observatories.length(); i++) {
                JSONObject observatory = observatories.getJSONObject(i);

                String setObservatoryString = "INSERT INTO observatories (recordId, observatoryName, latitude, longitude, temperatureInKelvins, cloudinessPercentance, bagroundLightVolume) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement observatoryStatement = dbConnection.prepareStatement(setObservatoryString);
                observatoryStatement.setInt(1, lastRecordId );
                observatoryStatement.setString(2, observatory.getString("observatoryName"));
                observatoryStatement.setDouble(3, observatory.getDouble("latitude"));
                observatoryStatement.setDouble(4, observatory.getDouble("longitude"));

                if(observatoryWeather != null && observatoryWeather.length() > 0) {
                    JSONObject weather = observatoryWeather.getJSONObject(i);
                    observatoryStatement.setDouble(5, weather.getDouble("temperatureInKelvins"));
                    observatoryStatement.setDouble(6, weather.getDouble("cloudinessPercentance"));
                    observatoryStatement.setDouble(7, weather.getDouble("bagroundLightVolume"));
                }
                else {
                    observatoryStatement.setNull(5, java.sql.Types.DOUBLE);
                    observatoryStatement.setNull(6, java.sql.Types.DOUBLE);
                    observatoryStatement.setNull(7, java.sql.Types.DOUBLE);
                }
                observatoryStatement.executeUpdate();
                observatoryStatement.close();
            }
        }
        System.out.println("Message added to DB");

        return true;
    }

    public JSONArray getMessages(String username) throws SQLException {
        Statement queryStatement = null;
        ResultSet rs;

        String getMessagesString = "select * from messages";
        queryStatement = dbConnection.createStatement();
        rs = queryStatement.executeQuery(getMessagesString);

        JSONArray responseMessages = getObservatories(rs, username);
        rs.close();
        queryStatement.close();
        return responseMessages;
    }

    public boolean updateMessage(int recordId,String username, String updateReason, ObservationRecord message) throws SQLException {
        // Get the username associated with the recordId
        String getUsernameString = "SELECT username FROM messages WHERE recordId = ?";
        PreparedStatement getUsernameStatement = dbConnection.prepareStatement(getUsernameString);
        getUsernameStatement.setInt(1, recordId);
        ResultSet usernameRS = getUsernameStatement.executeQuery();

        if (usernameRS.next()) {
            String dbUsername = usernameRS.getString("username");
            if (!dbUsername.equals(username)) {
                System.out.println("Username does not match. Update not executed.");
                usernameRS.close();
                getUsernameStatement.close();
                throw new SQLException("Username does not match. Update not executed.");
            }
        } else {
            System.out.println("Record not found. Update not executed.");
            usernameRS.close();
            getUsernameStatement.close();
            throw new SQLException("Record not found. Update not executed.");
        }
        usernameRS.close();
        getUsernameStatement.close();

        // Build the dynamic SQL update query
        StringBuilder queryBuilder = new StringBuilder("UPDATE messages SET ");
        List<Object> params = new ArrayList<>();

        if (message.getRecordDescription() != null) {
            queryBuilder.append("recordDescription = ?, ");
            params.add(message.getRecordDescription());
        }
        if (message.getRecordPayload() != null) {
            queryBuilder.append("recordPayload = ?, ");
            params.add(message.getRecordPayload());
        }
        if (message.getRecordRightAscension() != null) {
            queryBuilder.append("recordRightAscension = ?, ");
            params.add(message.getRecordRightAscension());
        }
        if (message.getRecordDeclination() != null) {
            queryBuilder.append("recordDeclination = ?, ");
            params.add(message.getRecordDeclination());
        }
        if (message.getRecordOwner() != null) {
            queryBuilder.append("recordOwner = ?, ");
            params.add(message.getRecordOwner());
        }
        if (message.getRecordIdentifier() != null) {
            queryBuilder.append("recordIdentifier = ?, ");
            params.add(message.getRecordIdentifier());
        }

        // Always update updateReason and modified timestamp
        queryBuilder.append("updateReason = ?, modified = ?, ");
        params.add(updateReason != null ? updateReason : "N/A");
        params.add(System.currentTimeMillis());

        // Remove the last comma and space
        queryBuilder.setLength(queryBuilder.length() - 2);

        // Append WHERE clause
        queryBuilder.append(" WHERE recordId = ?");
        params.add(recordId);

        // Prepare and execute the update statement
        PreparedStatement updateStatement = dbConnection.prepareStatement(queryBuilder.toString());
        for (int i = 0; i < params.size(); i++) {
            updateStatement.setObject(i + 1, params.get(i));
        }
        updateStatement.executeUpdate();
        updateStatement.close();


        // Insert updated observatories (excluding weather details)
        if (message.getObservatory() != null) {
            // Delete old observatory records associated with this message
            String deleteObservatoryString = "DELETE FROM observatories WHERE recordId = ?";
            PreparedStatement deleteStatement = dbConnection.prepareStatement(deleteObservatoryString);
            deleteStatement.setInt(1, recordId);
            deleteStatement.executeUpdate();
            deleteStatement.close();
            JSONArray observatories = new JSONArray(message.getObservatory());
            for (int i = 0; i < observatories.length(); i++) {
                JSONObject observatory = observatories.getJSONObject(i);

                String insertObservatoryString = "INSERT INTO observatories (recordId, observatoryName, latitude, longitude) " +
                        "VALUES (?, ?, ?, ?)";
                PreparedStatement observatoryStatement = dbConnection.prepareStatement(insertObservatoryString);
                observatoryStatement.setInt(1, recordId);
                observatoryStatement.setString(2, observatory.getString("observatoryName"));
                observatoryStatement.setDouble(3, observatory.getDouble("latitude"));
                observatoryStatement.setDouble(4, observatory.getDouble("longitude"));
                observatoryStatement.executeUpdate();
                observatoryStatement.close();
            }
        }

        System.out.println("Message updated in DB");
        return true;
    }
    
    // Search for messages based on the given parameters except username
    //username is used to check if the user is the owner of the message
    public JSONArray search(String nickname, String observationId, Long fromDate, Long toDate, String username) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM messages WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (nickname != null) {
            query.append(" AND recordOwner = ?");
            params.add(nickname);
        }
        if (observationId != null) {
            query.append(" AND recordIdentifier = ?");
            params.add(observationId);
        }
        if (fromDate != null) {
            query.append(" AND recordTimeReceived >= ?");
            params.add(fromDate);
        }
        if (toDate != null) {
            query.append(" AND recordTimeReceived <= ?");
            params.add(toDate);
        }

        PreparedStatement statement = dbConnection.prepareStatement(query.toString());
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }

        ResultSet rs = statement.executeQuery();
        JSONArray responseMessages = getObservatories(rs,username);

        rs.close();
        statement.close();
        return responseMessages;
    }

    // Get observatories for each message
    private JSONArray getObservatories(ResultSet rs, String username) throws SQLException {

        JSONArray responseMessages = new JSONArray();
        while (rs.next()) {
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("id", rs.getInt("recordId"));
            jsonMessage.put("recordIdentifier", rs.getString("recordIdentifier"));
            jsonMessage.put("recordDescription", rs.getString("recordDescription"));
            if(rs.getInt("decipherShift") != 0 && rs.getString("username").equals(username)){
                String decipheredText = getDecipherText(rs.getString("recordPayload"), rs.getInt("decipherShift"));
                jsonMessage.put("recordPayload", decipheredText);
            }else{
                jsonMessage.put("recordPayload", rs.getString("recordPayload"));
            }
            jsonMessage.put("recordRightAscension", rs.getString("recordRightAscension"));
            jsonMessage.put("recordDeclination", rs.getString("recordDeclination"));
            jsonMessage.put("recordOwner", rs.getString("recordOwner"));
            jsonMessage.put("recordTimeReceived",ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("recordTimeReceived")), ZoneOffset.UTC).toString());
            jsonMessage.put("view", rs.getInt("view"));
            jsonMessage.put("averageRating", rs.getDouble("averageRating"));
            if (rs.getLong("modified") != 0) {
                jsonMessage.put("modified", ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("modified")), ZoneOffset.UTC).toString());
                jsonMessage.put("updateReason", rs.getString("updateReason"));
            }

            String getObservatoriesString = "select * from observatories where recordId = '" + rs.getString("recordId") + "'";
            Statement observatoryStatement = dbConnection.createStatement();
            ResultSet observatoryRS = observatoryStatement.executeQuery(getObservatoriesString);       
            JSONArray observatories = new JSONArray();
            JSONArray observatoryWeather = new JSONArray();
                while (observatoryRS.next()) {
                    JSONObject observatory = new JSONObject();
                    observatory.put("observatoryName", observatoryRS.getString("observatoryName"));
                    observatory.put("latitude", observatoryRS.getDouble("latitude"));
                    observatory.put("longitude", observatoryRS.getDouble("longitude"));
                    observatories.put(observatory);

                    // Check if weather data is available
                     if (!observatoryRS.wasNull()) {
                    Double temperature = observatoryRS.getDouble("temperatureInKelvins");
                    Double cloudiness = observatoryRS.getDouble("cloudinessPercentance");
                    Double backgroundLight = observatoryRS.getDouble("bagroundLightVolume");

                    if (!observatoryRS.wasNull()) {
                        JSONObject weather = new JSONObject();
                        weather.put("temperatureInKelvins", temperature);
                        weather.put("cloudinessPercentage", cloudiness);
                        weather.put("backgroundLightVolume", backgroundLight);
                        observatoryWeather.put(weather);
                }
            }
        }

            // Add observatories if available
            if (observatories.length() > 0) {
                jsonMessage.put("observatory", observatories);
            }
            // Add observatoryWeather only if it contains valid data
            if (observatoryWeather.length() > 0) {
                jsonMessage.put("observatoryWeather", observatoryWeather);
            }
                observatoryRS.close();
                observatoryStatement.close();
            responseMessages.put(jsonMessage);
        }
        return responseMessages;
    }

    private String getDecipherText(String cipherText, int shift) {
        // Implement this function to decipher the text
        try {
            URI uri = new URI("http://127.0.0.1:4002/decipher?shift=" + shift);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String jsonInputString = cipherText;

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            String responseString = response.toString();
            System.out.println("Deciphered text: " + responseString);
            return responseString;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteMessage(int recordId) throws SQLException {
        
    String deleteObservatoriesSQL = "DELETE FROM observatories WHERE recordId = ?";
    String deleteCommentsSQL = "DELETE FROM comments WHERE recordId = ?";
    String deleteRatingsSQL = "DELETE FROM ratings WHERE recordId = ?";
    String deleteMessageSQL = "DELETE FROM messages WHERE recordId = ?";
    PreparedStatement observatoryStmt = null;
    PreparedStatement commentsStmt = null;
    PreparedStatement ratingsStmt = null;
    PreparedStatement messageStmt = null;

    try {
        // Start transaction
        dbConnection.setAutoCommit(false);

        // Delete from observatories first to maintain foreign key integrity
        observatoryStmt = dbConnection.prepareStatement(deleteObservatoriesSQL);
        observatoryStmt.setInt(1, recordId);
        observatoryStmt.executeUpdate();

        // Delete comments associated with the record
        commentsStmt = dbConnection.prepareStatement(deleteCommentsSQL);
        commentsStmt.setInt(1, recordId);
        commentsStmt.executeUpdate();

        // Delete ratings associated with the record
        ratingsStmt = dbConnection.prepareStatement(deleteRatingsSQL);
        ratingsStmt.setInt(1, recordId);
        ratingsStmt.executeUpdate();

        // Then delete from messages
        messageStmt = dbConnection.prepareStatement(deleteMessageSQL);
        messageStmt.setInt(1, recordId);
        int rowsAffected = messageStmt.executeUpdate();

        dbConnection.commit(); // Commit if all deletions succeed

        return rowsAffected > 0;
    } catch (SQLException e) {
        dbConnection.rollback(); // Rollback on error
        throw e;
    } finally {
        if (observatoryStmt != null) observatoryStmt.close();
        if (commentsStmt != null) commentsStmt.close();
        if (ratingsStmt != null) ratingsStmt.close();
        if (messageStmt != null) messageStmt.close();
        dbConnection.setAutoCommit(true); // Restore default
    }
}

    public boolean saveComment(int recordId,String username, String userNickname, String commentText) throws SQLException {
        String saveCommentSQL = "INSERT INTO comments (recordId, username, userNickname, commentText, commentTime) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement statement = null;

        try {
            statement = dbConnection.prepareStatement(saveCommentSQL);
            statement.setInt(1, recordId);
            statement.setString(2, username);
            statement.setString(3, userNickname);
            statement.setString(4, commentText);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
            return true;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    public boolean saveRating(int recordId, String username, int rating) throws SQLException {
        String checkRatingSQL = "SELECT ratingId FROM ratings WHERE recordId = ? AND username = ?";
        PreparedStatement checkStatement = null;

        try {
            checkStatement = dbConnection.prepareStatement(checkRatingSQL);
            checkStatement.setInt(1, recordId);
            checkStatement.setString(2, username);
            ResultSet rs = checkStatement.executeQuery();

            if (rs.next()) {
                // User has already rated this record, update the rating
                String updateRatingSQL = "UPDATE ratings SET rating = ? WHERE ratingId = ?";
                PreparedStatement updateStatement = null;
                try {
                    updateStatement = dbConnection.prepareStatement(updateRatingSQL);
                    updateStatement.setInt(1, rating);
                    updateStatement.setInt(2, rs.getInt("ratingId"));
                    updateStatement.executeUpdate();
                    return true;
                } finally {
                    if (updateStatement != null) {
                        updateStatement.close();
                    }
                }
            } else{
                System.out.println("User has not rated this record, inserting new rating");
                String saveRatingSQL = "INSERT INTO ratings (recordId, username, rating) VALUES (?, ?, ?)";
                PreparedStatement statement = null;

                try {
                    statement = dbConnection.prepareStatement(saveRatingSQL);
                    statement.setInt(1, recordId);
                    statement.setString(2, username);
                    statement.setInt(3, rating);
                    statement.executeUpdate();
                    return true;
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }}
            } finally {
                if (checkStatement != null) {
                    checkStatement.close();
                }
                // Recalculate the average rating and update it in the record
                double averageRating = getAverageRating(recordId);
                String updateAverageRatingSQL = "UPDATE messages SET averageRating = ? WHERE recordId = ?";
                PreparedStatement updateAverageRatingStatement = null;

                try {
                    updateAverageRatingStatement = dbConnection.prepareStatement(updateAverageRatingSQL);
                    updateAverageRatingStatement.setDouble(1, averageRating);
                    updateAverageRatingStatement.setInt(2, recordId);
                    updateAverageRatingStatement.executeUpdate();
                } finally {
                    if (updateAverageRatingStatement != null) {
                        updateAverageRatingStatement.close();
                    }
                }
            }
    }

    public void incrementViewCount(int recordId) throws SQLException {
        String incrementViewSQL = "UPDATE messages SET view = view + 1 WHERE recordId = ?";
        PreparedStatement statement = null;

        try {
            statement = dbConnection.prepareStatement(incrementViewSQL);
            statement.setInt(1, recordId);
            statement.executeUpdate();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    public JSONArray getComments(int recordId) throws SQLException {
        String getCommentsSQL = "SELECT * FROM comments WHERE recordId = ?";
        PreparedStatement statement = null;
        ResultSet rs = null;
        JSONArray comments = new JSONArray();
        try {
            statement = dbConnection.prepareStatement(getCommentsSQL);
            statement.setInt(1, recordId);
            rs = statement.executeQuery();
            while (rs.next()) {
                JSONObject comment = new JSONObject();
                comment.put("commentId", rs.getInt("commentId"));
                comment.put("recordId", rs.getInt("recordId"));
                comment.put("userNickname", rs.getString("userNickname"));
                comment.put("commentText", rs.getString("commentText"));
                comment.put("commentTime", ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(rs.getLong("commentTime")), ZoneOffset.UTC).toString());
                comments.put(comment);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return comments;
    }

    public JSONArray getTopMostViewedRecords(int top,String username) throws SQLException {
        String getTopSQL = "SELECT * FROM messages ORDER BY view DESC LIMIT " + top;
        PreparedStatement statement = null;
        ResultSet rs = null;
        JSONArray topRecords = new JSONArray();

        try {
            statement = dbConnection.prepareStatement(getTopSQL);
            rs = statement.executeQuery();

            topRecords = getObservatories(rs, username);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return topRecords;
    }

    public double getAverageRating(int recordId) throws SQLException {
        String getAverageRatingSQL = "SELECT AVG(rating) AS averageRating FROM ratings WHERE recordId = ?";
        PreparedStatement statement = null;
        ResultSet rs = null;
        double averageRating = 0.0;

        try {
            statement = dbConnection.prepareStatement(getAverageRatingSQL);
            statement.setInt(1, recordId);
            rs = statement.executeQuery();

            if (rs.next()) {
                averageRating = rs.getDouble("averageRating");
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return averageRating;
    }

    public JSONArray getTopHighestRatedRecords(int top, String username) throws SQLException {
        String getTopRatedSQL = "SELECT * FROM messages ORDER BY averageRating DESC LIMIT ?";
        PreparedStatement statement = null;
        ResultSet rs = null;
        JSONArray topRatedRecords = new JSONArray();

        try {
            statement = dbConnection.prepareStatement(getTopRatedSQL);
            statement.setInt(1, top);
            rs = statement.executeQuery();

            topRatedRecords = getObservatories(rs, username);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return topRatedRecords;
    }

    public JSONArray getTopMostCommentedRecords(int top, String username) throws SQLException {
        String getTopCommentedSQL = "SELECT m.*, COUNT(c.commentId) AS commentCount " +
                                    "FROM messages m " +
                                    "LEFT JOIN comments c ON m.recordId = c.recordId " +
                                    "GROUP BY m.recordId " +
                                    "ORDER BY commentCount DESC " +
                                    "LIMIT ?";
        PreparedStatement statement = null;
        ResultSet rs = null;
        JSONArray topCommentedRecords = new JSONArray();

        try {
            statement = dbConnection.prepareStatement(getTopCommentedSQL);
            statement.setInt(1, top);
            rs = statement.executeQuery();

            topCommentedRecords = getObservatories(rs, username);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return topCommentedRecords;
    }

    public JSONArray getRandomRecords(int count, String username) throws SQLException {
        String getRandomRecordsSQL = "SELECT * FROM messages ORDER BY RANDOM() LIMIT ?";
        PreparedStatement statement = null;
        ResultSet rs = null;
        JSONArray randomRecords = new JSONArray();

        try {
            statement = dbConnection.prepareStatement(getRandomRecordsSQL);
            statement.setInt(1, count);
            rs = statement.executeQuery();

            randomRecords = getObservatories(rs, username);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return randomRecords;
    }

}


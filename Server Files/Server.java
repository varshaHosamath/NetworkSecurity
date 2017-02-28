import javax.crypto.KeyGenerator;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;

public class Server {
    static public volatile HashMap<String, HashMap<String, Object>> userDB;

    static void initDB(){
        userDB = new HashMap<String, HashMap<String, Object>>();
        HashMap<String, Object> userRow1 = new HashMap<String, Object>();
        userRow1.put("hostname", "dc01.utdallas.edu");
        userRow1.put("passwordHash", hash("test"));
        userRow1.put("userOnline", new Boolean(false));
        userRow1.put("lastTimestamp", new Long(0));
        userDB.put("varsha", userRow1);

        HashMap<String, Object> userRow2 = new HashMap<String, Object>();
        userRow2.put("hostname", "dc03.utdallas.edu");
        userRow2.put("passwordHash", hash("test"));
        userRow2.put("userOnline", new Boolean(false));
        userRow2.put("lastTimestamp", new Long(0));
        userDB.put("praveen", userRow2);

        HashMap<String, Object> userRow3 = new HashMap<String, Object>();
        userRow3.put("hostname", "dc04.utdallas.edu");
        userRow3.put("passwordHash", hash("test"));
        userRow3.put("userOnline", new Boolean(false));
        userRow3.put("lastTimestamp", new Long(0));
        userDB.put("raksha", userRow3);

    }

    static public HashMap<String, HashMap<String, Object>> returnUserDB(){
        return userDB;
    }

    static boolean userOnline(String username){
        Boolean userOnlineObject = (Boolean)userDB.get(username).get("userOnline");
        if(userOnlineObject.booleanValue())
            return true;
        else
            return false;
    }

    static boolean userInDB(String username) {
        if(userDB.containsKey(username))
            return true;
        else
            return false;
    }

    static void addOnlineUser(String username, String hostname, Key checksumKey, Key messageKey, long timestamp) {
        userDB.get(username).put("hostname", hostname);
        userDB.get(username).put("checksumKey", checksumKey);
        userDB.get(username).put("messageKey", messageKey);
        userDB.get(username).put("lastTimestamp", new Long(timestamp));
        userDB.get(username).put("userOnline", new Boolean(true));
    }

    static boolean userAuthenticated(String username, byte[] passwordHash) {
        if(userInDB(username) && Arrays.equals((byte[])userDB.get(username).get("passwordHash"), passwordHash))
            return true;
        else
            return false;
    }

    static byte[] hash(String stringToHash){
        try {
            byte[] passwordBytes = stringToHash.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(passwordBytes);
            return digest;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void handleLogin(Object message, ObjectOutputStream outputStream) {
        HashMap<String, Object> inputMessage = (HashMap<String, Object>)CryptoFunctions.RSADecrypt((byte[])message);
        System.out.println(inputMessage);
        String username = (String)inputMessage.get("username");
        byte[] passwordHash = hash((String)inputMessage.get("password"));
        HashMap<String, String> result = new HashMap<String, String>();
        if(userAuthenticated(username, passwordHash)) {
            String userHostname = (String)inputMessage.get("hostName");
            Key checksumKey = (Key)inputMessage.get("checksumKey");
            Key messageKey = (Key)inputMessage.get("messageKey");
            long timestamp = System.currentTimeMillis();
            addOnlineUser(username, userHostname, checksumKey, messageKey, timestamp);
            result.put("result", "success");
        } else
            result.put("result", "failure");
        try {
            outputStream.writeObject(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Key keyGenerator(String keyType) {
        try {
            if (keyType.equals("checksum")) {
                KeyGenerator keyGen = KeyGenerator.getInstance("DES");
                keyGen.init(56);
                return keyGen.generateKey();
            }
            else {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                return keyGen.generateKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Key getUserKey(String username, String keyType){
        HashMap<String, Object> userRow = userDB.get(username);
        if(keyType.equals("messageKey")) {
            return (Key)userRow.get("messageKey");
        }
        else {
            return (Key)userRow.get("checksumKey");
        }
    }

    static HashMap<String, byte[]> generateTicket(String targetUser, String currentUser, Key sharedCKey, Key sharedMKey){
        HashMap<String, Object> ticket = new HashMap<String, Object>();
        ticket.put("fromUser", currentUser);
        ticket.put("sharedChecksumKey", sharedCKey);
        ticket.put("sharedMessageKey", sharedMKey);
        Key targetUserMKey = getUserKey(targetUser, "messageKey");
        Key targetUserCKey = getUserKey(targetUser, "checksumKey");
        return CryptoFunctions.encrypt(ticket, targetUserCKey, targetUserMKey);
    }

    static void updateTimestamp(String user, long timestamp) {
        userDB.get(user).put("lastTimestamp", new Long(timestamp));
    }

    static String getUserHostname(String user) {
        HashMap<String, Object> userRow = (HashMap<String, Object>)userDB.get(user);
        return (String)userRow.get("hostname");
    }

    static void newSession(Object inputMessage, ObjectOutputStream outStream){
        try{
            HashMap<String, Object> message = (HashMap<String, Object>)inputMessage;
            String targetUser = (String)message.get("targetUser");
            String currentUser = (String)message.get("username");
            String nonce = (String)message.get("nonce");
            String targetUserHostname = getUserHostname(targetUser);
            updateTimestamp(currentUser, System.currentTimeMillis());
            if(!userOnline(targetUser) || !userOnline(currentUser)) {
                outStream.writeObject(null);
            }
            else {
                Key sharedChecksumKey = keyGenerator("checksum");
                Key sharedMessageKey = keyGenerator("message");
                Key currentUserCKey = getUserKey(currentUser, "checksumKey");
                Key currentUserMKey = getUserKey(currentUser, "messageKey");
                HashMap<String, byte[]> ticket = generateTicket(targetUser, currentUser, sharedChecksumKey, sharedMessageKey);
                HashMap<String, Object> returnMessage = new HashMap<String, Object>();
                returnMessage.put("user", targetUser);
                returnMessage.put("nonce", nonce);
                returnMessage.put("checksumKey", sharedChecksumKey);
                returnMessage.put("messageKey", sharedMessageKey);
                returnMessage.put("userHostname", targetUserHostname);
                returnMessage.put("ticket", ticket);
                outStream.writeObject(CryptoFunctions.encrypt(returnMessage, currentUserCKey, currentUserMKey));
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    static void heartbeat(Object message, String username){
        HashMap<String, byte[]> recastMessage = (HashMap<String, byte[]>)message;
        Key currentUserCKey = getUserKey(username, "checksumKey");
        Key currentUserMKey = getUserKey(username, "messageKey");
        Object decryptedMessage = CryptoFunctions.decrypt(recastMessage, currentUserCKey, currentUserMKey);
        Long timestamp = (Long)decryptedMessage;
        updateTimestamp(username, timestamp.longValue());
    }

    public static void main(String[] args) {
        initDB();
        HeartbeatMonitor monitor = new HeartbeatMonitor();
        Thread heartbeatMonitorThread = new Thread(monitor);
        heartbeatMonitorThread.start();
        try {
            ServerSocket serverSocket = new ServerSocket(4444);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection accepted");
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
                Object input = inputStream.readObject();
                HashMap<String, Object> inputMessage = (HashMap<String, Object>)input;
                System.out.println((String)inputMessage.get("messageType"));
                System.out.println("Input: " + input);
                if (inputMessage != null) {
                    String messageType = (String) inputMessage.get("messageType");
                    Object message = inputMessage.get("message");

                    if (messageType.equals("login")) {
                        handleLogin(message, outputStream);
                    }
                    if (messageType.equals("newSession")) {
                        newSession(message, outputStream);
                    }
                    if (messageType.equals("heartbeat")) {
                        String username = (String)inputMessage.get("userName");
                        heartbeat(message, username);
                    }
                }
                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}

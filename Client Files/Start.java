import java.awt.List;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.ws.handler.LogicalHandler;

public class Start{

    // HashMap to send in type,username, password, k1,k2
    private static HashMap<String,Object> loginMsg;
    private static HashMap<String,Object> loginMsgOuter;
    private static HashMap<String,String> clientTickets;
    private static String userName;
    private static String [] buddyList;
    private String messageType;
    private static String hostName = "";
    private static String password;
    public static Key checksumKey;
    public static Key messageKey;

    public static String user;
    private static String loginSuccess = "";
    private static boolean success = false;
    static ArrayList<String> line = new ArrayList<>();
    static Scanner reader1 = new Scanner(System.in);


    public static void main(String[] args) throws NoSuchAlgorithmException, ClassNotFoundException, IOException, InterruptedException {
        // TODO Auto-generated method stub
        //String directory = "C:/Users/Varsha/Desktop/Documents/eclipse/NetSecChatApp/src/config.txt";
        String directory = "config.txt";
        BufferedReader reader = new BufferedReader(new FileReader(directory));

        try{

            while (reader.ready()) {
                line.add(reader.readLine());
                //reader.close();
            }
        }
        finally{
            reader.close();
        }

        hostName = line.get(0);

        while(!success){

            GetLoginDetails();
        }
        Server serverListen = new Server();
        Thread t1 = new Thread(serverListen);
        t1.start();

        Thread t2 = new Thread(new Heartbeat(userName));
        t2.start();


        selectUserToChat();

        //Timer timer = new Timer();
        //timer.schedule(new Heartbeat(userName), 0, 30000);

    }



    public static void selectUserToChat() throws ClassNotFoundException {
        // TODO Auto-generated method stub
        //Ask the user who he wants to talk to
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Select the user. List of users : ");
        for (int x=1; x<line.size(); x++)
            System.out.println(line.get(x));
        System.out.println("I want to talk to : ");

        user  = reader1.next();

        if(!user.equals("talk")){

            Client chatRequestClient = new Client();
            chatRequestClient.requestForChat(userName, user);
        }
        else {
            System.out.println("Skipping user selection");
            while(true){}
        }

    }



    private static void GetLoginDetails() throws ClassNotFoundException, NoSuchAlgorithmException {
        System.out.println("Hello there. Enter Username and password\n");
        // TODO Auto-generated method stub

        Scanner reader = new Scanner(System.in);
        System.out.println("Username: ");
        userName  = reader.next();
        System.out.println("password: ");
        password  = reader.next();
        KeyGenerator keyGen1 = KeyGenerator.getInstance("DES");
        keyGen1.init(56);
        checksumKey = keyGen1.generateKey();
        KeyGenerator keyGen2 = KeyGenerator.getInstance("AES");
        keyGen2.init(128);
        messageKey = keyGen2.generateKey();
        loginMsg = new HashMap<String,Object>();
        loginMsgOuter = new HashMap<String,Object>();

        loginMsg.put("username", userName );
        loginMsg.put("password", password );
        loginMsg.put("hostName", hostName);
        loginMsg.put("checksumKey", checksumKey );
        loginMsg.put("messageKey", messageKey );
        //loginMsg.put("messageType", 1 );

        loginMsgOuter.put("messageType", "login" );
        loginMsgOuter.put("message", CryptoFunctions.RSAEncrypt(loginMsg));
        //reader.close();
        Client loginclient = new Client();
        HashMap<String,Object> loginReturn = loginclient.requestlogin(loginMsgOuter);
        //check if loginIn was successful
        loginSuccess = (String) loginReturn.get("result");
        if(loginSuccess.equals("success")){
            success = true;

        }
        else{
            System.out.println("Login Unsuccessful, try again");

        }

    }

}

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;

public class CryptoFunctions {
    static HashMap<String, byte[]> encrypt(Object object, Key checksumKey, Key messageKey) {
        byte[] byteArray = convertToByteArray(object);
        byte[] encryptedMessage = byteArrayEncrypt(byteArray, messageKey, "AES");
        byte[] encryptedChecksum = byteArrayEncrypt(calculateChecksum(encryptedMessage), checksumKey, "DES");
        HashMap<String, byte[]> result = new HashMap<String, byte[]>();
        result.put("encMessage", encryptedMessage);
        result.put("encChecksum", encryptedChecksum);
        return result;
    }

    static byte[] calculateChecksum(byte[] byteArray) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(byteArray);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    static byte[] convertToByteArray(Object object) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutput out = null;
            byte[] byteArray;

            out = new ObjectOutputStream(byteOut);
            out.writeObject(object);
            out.flush();

            byteArray = byteOut.toByteArray();
            byteOut.close();
            return byteArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static byte[] byteArrayEncrypt(byte[] message, Key key, String mode) {
        try {
            Cipher cipher;
            if(mode.equals("AES")) {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } else {
                cipher = Cipher.getInstance("DES");
                cipher.init(Cipher.ENCRYPT_MODE, key);
            }
            return cipher.doFinal(message);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static byte[] byteArrayDecrypt(byte[] encMessage, Key key, String mode) {
        try {
            Cipher cipher;
            if(mode.equals("AES")) {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, key);
            } else {
                cipher = Cipher.getInstance("DES");
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            return cipher.doFinal(encMessage);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Object convertFromByteArray(byte[] byteStream) {
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteStream);
            ObjectInput in = new ObjectInputStream(byteIn);
            Object output = in.readObject();
            if (in != null) {
                in.close();
            }
            return output;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    static Object decrypt(HashMap<String, byte[]> message, Key cKey, Key mKey){
        byte[] encCheckSum = message.get("encChecksum");
        byte[] encMessage = message.get("encMessage");
        byte[] checksum = byteArrayDecrypt(encCheckSum, cKey, "DES");

        byte[] calculatedChecksum = calculateChecksum(encMessage);

        if(Arrays.equals(checksum, calculatedChecksum)) {
            byte[] decMessage = byteArrayDecrypt(encMessage, mKey, "AES");
            return convertFromByteArray(decMessage);
        } else {
            return null;
        }
    }

    static byte[] RSAEncrypt(Object object) {
        try {
            ObjectInputStream inputStream = null;
            inputStream = new ObjectInputStream(new FileInputStream("public.key"));
            PublicKey publicKey = (PublicKey) inputStream.readObject();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return blockCipher(convertToByteArray(object), Cipher.ENCRYPT_MODE, cipher);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    static byte[] append(byte[] prefix, byte[] suffix){
        byte[] toReturn = new byte[prefix.length + suffix.length];
        for (int i=0; i< prefix.length; i++){
            toReturn[i] = prefix[i];
        }
        for (int i=0; i< suffix.length; i++){
            toReturn[i+prefix.length] = suffix[i];
        }
        return toReturn;
    }


    static byte[] blockCipher(byte[] bytes, int mode, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
        byte[] scrambled = new byte[0];
        byte[] toReturn = new byte[0];
        int length = (mode == Cipher.ENCRYPT_MODE)? 100 : 128;
        byte[] buffer = new byte[length];

        for (int i=0; i< bytes.length; i++){
            if ((i > 0) && (i % length == 0)){
                scrambled = cipher.doFinal(buffer);
                toReturn = append(toReturn,scrambled);
                int newlength = length;

                if (i + length > bytes.length) {
                    newlength = bytes.length - i;
                }
                buffer = new byte[newlength];
            }
            buffer[i%length] = bytes[i];
        }

        scrambled = cipher.doFinal(buffer);
        toReturn = append(toReturn,scrambled);
        return toReturn;
    }


    static Object RSADecrypt(byte[] byteArray) {
        try {
            ObjectInputStream inputStream = null;
            inputStream = new ObjectInputStream(new FileInputStream("private.key"));
            PrivateKey privateKey = (PrivateKey) inputStream.readObject();
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return convertFromByteArray(blockCipher(byteArray,Cipher.DECRYPT_MODE, cipher));
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}



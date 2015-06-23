/************************************************************************
 * Source filename: Encryption.java
 * <p/>
 * Creation date: February 11, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/
package wde.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

public class Encryption {
    private static String seed = "RDE_^y1u275uJ6WxdeQr_Guest";
    private static SimpleDateFormat timeFormatter;

    static {
        timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String getTimeVariantMessage(String message) {
        String newMessage = null;
        long now = System.currentTimeMillis();
        newMessage = timeFormatter.format(now) + " " + message;

        return newMessage;
    }

    public static String getOriginalMessage(String inMsg) throws Exception {
        String message = decryptToString(inMsg);
        String dateStr = message.substring(0, 18);
        Date date = timeFormatter.parse(dateStr);
        long now = System.currentTimeMillis();
        long delta = Math.abs(date.getTime() - now);
        if (delta > 300000)
            throw new Exception("Message has expired!");

        return message.substring(20);
    }

    public static String encryptToString(String message) throws Exception {
        return Base64Encoder.encode(encrypt(message));
    }

    public static String decryptToString(String encodedString) throws Exception {
        return new String(decrypt(encodedString), "UTF-8");
    }

    public static String decryptToString(byte[] encodedBytes) throws Exception {
        return new String(decrypt(encodedBytes), "UTF-8");
    }

    public static byte[] encrypt(String message) throws Exception {
        final MessageDigest md = MessageDigest.getInstance("md5");
        final byte[] digestOfMessage = md.digest(seed.getBytes("utf-8"));

        final byte[] keyBytes = Arrays.copyOf(digestOfMessage, 24);

        for (int j = 0, k = 16; j < 8; ) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        final byte[] plainTextBytes = message.getBytes("utf-8");
        final byte[] cipherText = cipher.doFinal(plainTextBytes);

        return cipherText;
    }

    public static byte[] decrypt(String encodedString) throws Exception {

        byte[] message = Base64Decoder.decode(encodedString);
        final MessageDigest md = MessageDigest.getInstance("md5");
        final byte[] digestOfMessage = md.digest(seed.getBytes("utf-8"));

        final byte[] keyBytes = Arrays.copyOf(digestOfMessage, 24);

        for (int j = 0, k = 16; j < 8; ) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        final byte[] plainText = decipher.doFinal(message);

        return plainText;
    }

    public static byte[] decrypt(byte[] encodedBytes) throws Exception {
        final MessageDigest md = MessageDigest.getInstance("md5");
        final byte[] digestOfPassword = md.digest(seed.getBytes("utf-8"));

        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);

        for (int j = 0, k = 16; j < 8; ) {
            keyBytes[k++] = keyBytes[j++];
        }

        final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
        final IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        decipher.init(Cipher.DECRYPT_MODE, key, iv);

        final byte[] plainText = decipher.doFinal(encodedBytes);

        return plainText;
    }

    public static void main(String[] args) throws Exception {
        System.out.println();

        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase("-enc")) {
                String message = getTimeVariantMessage(args[1]);
                System.out.println(message);
                System.out.println("=-=-=-=-=-=-= BELOW IS YOUR MESSAGE ENCRYPTED IN 3DES =-=-=-=-=-=-=");
                System.out.println();
                System.out.println(Encryption.encryptToString(message));
                System.out.println();
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
            } else if (args[0].equalsIgnoreCase("-dec")) {
                String message = Encryption.getOriginalMessage(args[1]);
                System.out.println("=-=-=-=-=-=-= BELOW IS YOUR 3DES MESSAGE DECRYPTED =-=-=-=-=-=-=");
                System.out.println();
                System.out.println(message);
                System.out.println();
                System.out.println();
                System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
            } else
                System.out.println("Invalid arguments were entered");
        } else
            System.out.println("No arguments were provided");
    }
}

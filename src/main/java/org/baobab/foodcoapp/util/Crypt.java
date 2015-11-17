package org.baobab.foodcoapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.spongycastle.util.encoders.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Crypt {

    private static final String TAG = "POS";
    public static final int ITERATION_COUNT = 1; // very secure!

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static String hash(String data, Context ctx) {
        return hash(data.toCharArray(), Salt.SALT);
    }

    public static String hash(char[] pin, byte[] salt) {
        byte[] hash = null;
        try {
            hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(new PBEKeySpec(pin, salt, ITERATION_COUNT, 256))
                    .getEncoded();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage());
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, e.getMessage());
        }
        return hex(hash);
    }

    private static char[] hexAlphabet = "0123456789ABCDEF".toCharArray();

    public static String hex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexAlphabet[v >>> 4];
            hexChars[j * 2 + 1] = hexAlphabet[v & 0x0F];
        }
        return new String(hexChars);
    }

    static byte[] salt(Context ctx) {
        SharedPreferences prefs =
                ctx.getSharedPreferences("s", Context.MODE_PRIVATE);
        if (!prefs.contains("salt")) {
            prefs.edit().putString("salt",
                    Base64.toBase64String((Crypt.salt())))
                    .commit();
        }
        return Base64.decode(prefs.getString("salt", null));
    }

    private static byte[] salt() {
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
}
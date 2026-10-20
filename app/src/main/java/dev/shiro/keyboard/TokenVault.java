package dev.shiro.keyboard;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class TokenVault {
    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (!ks.containsAlias("shiro-api")) {
            KeyGenerator g = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder("shiro-api", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            g.generateKey();
        }
        return (SecretKey) ks.getKey("shiro-api", null);
    }
    static void save(Context c, String token) throws Exception {
        if (token.isEmpty()) { c.getSharedPreferences("secret",0).edit().clear().apply(); return; }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        c.getSharedPreferences("secret",0).edit().putString("iv", Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
            .putString("token",Base64.encodeToString(cipher.doFinal(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP)).apply();
    }
    static String read(Context c) throws Exception {
        var p = c.getSharedPreferences("secret",0); String t=p.getString("token",""); if(t.isEmpty()) return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(t,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);
    }
}

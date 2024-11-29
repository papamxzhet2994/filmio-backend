package com.papamxzhet.filmio.service;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = "1234567890123456"; // 16-char key

    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM));
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM));
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифровки", e);
        }
    }
}

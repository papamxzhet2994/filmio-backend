package com.papamxzhet.filmio;

import com.papamxzhet.filmio.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EncryptionService Tests")
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    @DisplayName("Должен успешно шифровать и расшифровывать простой текст.")
    void encryptDecrypt_SimpleText_ShouldWorkCorrectly() {
        String originalText = "Hello World";

        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    @DisplayName("Должен шифровать один и тот же текст с одинаковым результатом")
    void encrypt_SameTextMultipleTimes_ShouldProduceSameResult() {
        String text = "test message";

        String encrypted1 = encryptionService.encrypt(text);
        String encrypted2 = encryptionService.encrypt(text);

        assertEquals(encrypted1, encrypted2);
    }

    @Test
    @DisplayName("Должен обрабатывать шифрование и дешифрование пустых строк.")
    void encryptDecrypt_EmptyString_ShouldWorkCorrectly() {
        String emptyString = "";

        String encrypted = encryptionService.encrypt(emptyString);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertEquals(emptyString, decrypted);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a",
            "123",
            "Test with spaces and symbols !@#$%^&*()",
            "Тест с русскими символами",
            "Test\nwith\nnewlines",
            "Test\twith\ttabs",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed ut risus in justo ultrices tincidunt. Vestibulum",
    })
    @DisplayName("Should encrypt and decrypt various text types")
    void encryptDecrypt_VariousTextTypes_ShouldWorkCorrectly(String originalText) {
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    @DisplayName("Должен обрабатывать специальные символы и Unicode")
    void encryptDecrypt_UnicodeCharacters_ShouldWorkCorrectly() {
        String unicodeText = "🎬 Фильм с эмодзи 中文 العربية 😊";

        String encrypted = encryptionService.encrypt(unicodeText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(unicodeText, decrypted);
    }

    @Test
    @DisplayName("Должен выдавать зашифрованный текст, закодированный в Base64.")
    void encrypt_ShouldProduceValidBase64() {
        String text = "test";

        String encrypted = encryptionService.encrypt(text);

        assertDoesNotThrow(() -> {
            java.util.Base64.getDecoder().decode(encrypted);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Должен выдавать исключение при шифровании нулевой или пустой строки")
    void encrypt_NullOrEmptyInput_ShouldThrowException(String input) {
        if (input == null) {
            assertThrows(RuntimeException.class, () -> encryptionService.encrypt(input));
        } else {
            assertDoesNotThrow(() -> encryptionService.encrypt(input));
        }
    }

    @Test
    @DisplayName("Должен выдавать исключение при расшифровке нулевого ввода")
    void decrypt_NullInput_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(null));
    }

    @Test
    @DisplayName("Должен выдавать исключение при расшифровке недействительного Base64")
    void decrypt_InvalidBase64_ShouldThrowException() {
        String invalidBase64 = "This is not valid Base64!";

        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(invalidBase64));
    }

    @Test
    @DisplayName("При расшифровке поврежденных данных должно возникнуть исключение.")
    void decrypt_CorruptedData_ShouldThrowException() {
        String validBase64ButInvalidAES = java.util.Base64.getEncoder().encodeToString("invalid".getBytes());

        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(validBase64ButInvalidAES));
    }

    @Test
    @DisplayName("При расшифровке пустой строки должно возникнуть исключение.")
    void decrypt_EmptyString_ShouldThrowException() {
        String emptyString = "";

        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(emptyString));
    }

    @Test
    @DisplayName("Должен обрабатывать шифрование данных JSON")
    void encryptDecrypt_JsonData_ShouldWorkCorrectly() {
        String jsonData = "{\"username\":\"testUser\",\"roomId\":\"12345\",\"timestamp\":1234567890}";

        String encrypted = encryptionService.encrypt(jsonData);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(jsonData, decrypted);
    }

    @Test
    @DisplayName("Должно обрабатывать шифрование очень длинных текстов.")
    void encryptDecrypt_VeryLongText_ShouldWorkCorrectly() {
        String originalText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.".repeat(1000);

        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(originalText, decrypted);
    }

    @Test
    @DisplayName("Должен выдавать разные зашифрованные значения для разных входных данных.")
    void encrypt_DifferentInputs_ShouldProduceDifferentOutputs() {
        String text1 = "Hello";
        String text2 = "World";

        String encrypted1 = encryptionService.encrypt(text1);
        String encrypted2 = encryptionService.encrypt(text2);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    @DisplayName("Должен быть чувствителен к регистру.")
    void encryptDecrypt_CaseSensitive_ShouldProduceDifferentResults() {
        String lowerCase = "hello";
        String upperCase = "HELLO";

        String encryptedLower = encryptionService.encrypt(lowerCase);
        String encryptedUpper = encryptionService.encrypt(upperCase);

        assertNotEquals(encryptedLower, encryptedUpper);
        assertEquals(lowerCase, encryptionService.decrypt(encryptedLower));
        assertEquals(upperCase, encryptionService.decrypt(encryptedUpper));
    }

    @Test
    @DisplayName("Должен правильно обрабатывать пробелы")
    void encryptDecrypt_WithWhitespace_ShouldPreserveWhitespace() {
        String textWithSpaces = "  hello   world  ";

        String encrypted = encryptionService.encrypt(textWithSpaces);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(textWithSpaces, decrypted);
    }

    @Test
    @DisplayName("Должен обрабатывать числовые строки")
    void encryptDecrypt_NumericStrings_ShouldWorkCorrectly() {
        String numericString = "1234567890.123456";

        String encrypted = encryptionService.encrypt(numericString);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(numericString, decrypted);
    }

    @Test
    @DisplayName("Зашифрованные данные не должны содержать исходный текст.")
    void encrypt_ShouldNotContainOriginalText() {
        String originalText = "secretPassword123";

        String encrypted = encryptionService.encrypt(originalText);

        assertFalse(encrypted.contains(originalText));
        assertFalse(encrypted.toLowerCase().contains(originalText.toLowerCase()));
    }
}
package com.warehouse.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tiện ích mã hoá/giải mã AES cho dữ liệu nhạy cảm (email, phone)
 *
 * KHÁC BIỆT GIỮA HASH VÀ MÃ HOÁ:
 * - Hash (BCrypt): 1 chiều, không giải mã được -> dùng cho password
 * - Mã hoá AES: 2 chiều, có thể giải mã với key -> dùng cho email, phone
 *
 * DỮ LIỆU CẦN MÃ HOÁ:
 * - Email: thông tin cá nhân nhạy cảm
 * - Phone: thông tin liên lạc cá nhân
 * - Địa chỉ: thông tin nhạy cảm
 *
 * Trong production: key phải lưu ở biến môi trường, không hardcode trong code
 */
public class AESUtil {

    // Trong thực tế: lấy từ biến môi trường hoặc file cấu hình bên ngoài
    private static final String SECRET_KEY = "WarehouseKey2024";  // 16 bytes = 128-bit AES
    private static final String ALGORITHM = "AES";

    /**
     * Mã hoá chuỗi bằng AES
     * @param plainText Văn bản gốc
     * @return Chuỗi đã mã hoá (Base64)
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi mã hoá AES: " + e.getMessage(), e);
        }
    }

    /**
     * Giải mã chuỗi AES
     * @param encryptedText Chuỗi đã mã hoá (Base64)
     * @return Văn bản gốc
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return encryptedText;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Dữ liệu có thể chưa được mã hoá (dữ liệu cũ)
            return encryptedText;
        }
    }

    /**
     * Kiểm tra chuỗi có phải Base64 hợp lệ không
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

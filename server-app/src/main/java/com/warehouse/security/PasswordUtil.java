package com.warehouse.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích hash/verify mật khẩu bằng BCrypt
 *
 * LÝ DO DÙNG HASH (không mã hoá 2 chiều):
 * - Password hash là 1 chiều: không thể giải mã ngược lại
 * - Ngay cả admin cũng không biết password của user
 * - Khi đăng nhập: hash password nhập vào rồi so sánh với hash trong DB
 * - BCrypt tự tạo salt ngẫu nhiên mỗi lần hash -> cùng password có hash khác nhau
 * - Dùng mã hoá 2 chiều (AES) cho password là sai vì nếu key bị lộ -> lộ toàn bộ
 */
public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash mật khẩu bằng BCrypt
     * @param plainPassword Mật khẩu gốc
     * @return Chuỗi BCrypt hash
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Kiểm tra mật khẩu với hash
     * @param plainPassword Mật khẩu người dùng nhập
     * @param hashedPassword Hash trong database
     * @return true nếu khớp
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra độ mạnh mật khẩu (tối thiểu 6 ký tự)
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}

package com.smartcanteen.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkPassword(String rawPassword, String hash) {
        return hashPassword(rawPassword).equalsIgnoreCase(hash);
    }

    public static String generateOtp() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    /** Sinh ma nhan hang: S/G/K + 6 so cuoi cua StudentId hoac Phone */
    public static String generatePickupCode(String role, String studentId, String phone) {
        String prefix;
        String source;
        switch (role) {
            case "STUDENT": prefix = "S"; source = studentId; break;
            case "LECTURER": prefix = "G"; source = phone; break;
            default: prefix = "K"; source = phone; break;
        }
        if (source == null || source.isBlank()) source = phone != null ? phone : "000000";
        String digits = source.replaceAll("[^0-9]", "");
        String last6 = digits.length() >= 6 ? digits.substring(digits.length() - 6)
                : String.format("%6s", digits).replace(' ', '0');
        return prefix + last6;
    }

    /** Ma don hang dang SC + yyMMdd + so ngau nhien 4 chu so */
    public static String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int rand = 1000 + RANDOM.nextInt(9000);
        return "SC" + datePart + rand;
    }
}

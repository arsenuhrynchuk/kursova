package com.project;

import org.mindrot.jbcrypt.BCrypt;

public class Encryption {

    // Генеруємо хеш для пароля з використанням BCrypt
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12)); // Використовуємо новий рівень складності для генерації salt
    }

    // Перевірка пароля
    public static boolean checkPassword(String inputPassword, String storedHash) {
        if (storedHash == null) {
            return false;
        }

        // Перевіряємо пароль і хешуємо його з наявним хешем
        return BCrypt.checkpw(inputPassword, storedHash);
    }
}


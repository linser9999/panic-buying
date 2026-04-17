package com.linser.utils;

import com.linser.dto.UserDTO;
import com.linser.entity.User;

public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static UserDTO getUser() {
        return tl.get();
    }

    public static void saveUser(UserDTO user) {
        tl.set(user);
    }

    public static void remove() {
        tl.remove();
    }
}

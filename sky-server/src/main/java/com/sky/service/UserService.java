package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

import javax.security.auth.login.LoginException;

public interface UserService {
    User login(UserLoginDTO userLoginDTO) throws LoginException;
}

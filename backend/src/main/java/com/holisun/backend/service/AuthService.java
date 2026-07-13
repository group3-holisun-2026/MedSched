package com.holisun.backend.service;

import com.holisun.backend.entity.User;
import com.holisun.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public User registerUser(User user){
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new RuntimeException("Acest email este deja folosit!");

        }
        if(userRepository.findByUsername(user.getUsername()).isPresent()){
            throw new RuntimeException("Acest username este deja luat!");
        }
        return userRepository.save(user);
    }
}

package com.fantone.app_saos.security;

import com.fantone.app_saos.model.User;
import com.fantone.app_saos.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserDetailsImpl(user);
    }

    //public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    //    User user = userService.findByUsername(username)
    //            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    //    return new UserDetailsImpl(user);
    //}
}

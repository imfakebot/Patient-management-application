package com.pma.service;

import com.pma.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ tùy chỉnh để tải thông tin người dùng cho Spring Security. Lớp này sẽ
 * tìm UserAccount trong cơ sở dữ liệu và trả về nó, cho phép Spring Security
 * làm việc trực tiếp với entity của chúng ta.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}

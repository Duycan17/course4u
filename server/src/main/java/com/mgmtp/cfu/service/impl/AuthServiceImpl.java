package com.mgmtp.cfu.service.impl;

import com.mgmtp.cfu.dto.authdto.LoginRequest;
import com.mgmtp.cfu.dto.authdto.LoginResponse;
import com.mgmtp.cfu.dto.*;
import com.mgmtp.cfu.dto.authdto.SignUpRequest;
import com.mgmtp.cfu.dto.authdto.SignUpResponse;
import com.mgmtp.cfu.entity.User;
import com.mgmtp.cfu.enums.Gender;
import com.mgmtp.cfu.enums.Role;
import com.mgmtp.cfu.exception.AccountExistByEmailException;
import com.mgmtp.cfu.repository.UserRepository;
import com.mgmtp.cfu.service.IAuthService;
import com.mgmtp.cfu.service.IEmailService;
import com.mgmtp.cfu.service.IJwtService;
import com.mgmtp.cfu.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {
    private final AuthenticationManager authenticationManager;
    private final IJwtService jwtService;
    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow(() -> new BadCredentialsException("Authentication failed: Invalid credentials"));
            var accessToken = jwtService.generatedClaim(user.getUsername(), List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .build();
        } else {
            throw new BadCredentialsException("Authentication failed: Invalid credentials");
        }
    }

    @Override
    public SignUpResponse handleSignUpNewUser(SignUpRequest signUpRequest) {

        List<User> existingUsers = userRepository.findAllByEmail(signUpRequest.getEmail());

        if(userRepository.existsByUsername(signUpRequest.getUsername()))
            throw new AccountExistByEmailException("Username is already in use.");

        boolean emailInUseWithInvalidRole = existingUsers.stream()
                .anyMatch(user -> !user.getRole().equals(Role.ACCOUNTANT) && !user.getRole().equals(Role.ADMIN));

        if (emailInUseWithInvalidRole) {
            throw new AccountExistByEmailException("Email is already in use.");
        }

        // Hash the password
        String hashedPassword = passwordEncoder.encode(signUpRequest.getPassword());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .password(hashedPassword)
                .fullName(signUpRequest.getFullname())
                .email(signUpRequest.getEmail())
                .dateOfBirth((signUpRequest.getDateofbirth() != null && !signUpRequest.getDateofbirth().isEmpty()) ? LocalDate.parse(signUpRequest.getDateofbirth(), formatter) : null)
                .role(Role.USER)
                .gender((signUpRequest.getGender() != null && !signUpRequest.getGender().isEmpty()) ? Gender.valueOf(signUpRequest.getGender()) : null)
                .build();

        user = userRepository.save(user);
        List<MailContentUnit> mailContentUnits = List.of(
                EmailUtil.generateTitle("Welcome to Course4U!"),
                EmailUtil.updateTitleStyle("Welcome to Course4U!"),
                EmailUtil.generateGreeting("Hello {name},", user),
                EmailUtil.generateWelcomeContent()
        );
        emailService.sendMail(user.getEmail(), EmailUtil.generateSubject("Welcome to Course4U!"), "email-template.xml", mailContentUnits);

        return SignUpResponse.builder()
                .username(user.getUsername())
                .message("User registered successfully")
                .build();
    }
}

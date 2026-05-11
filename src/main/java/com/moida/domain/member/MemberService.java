package com.moida.domain.member;

import com.moida.common.request.SignupRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional
    public void signup(SignupRequest dto) {
        // memberNo 자동 생성 (예: 2026050900001)
        String memberNo = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%05d", memberRepository.count() + 1);


        Member member = Member.builder()
                .memberNo(memberNo)
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .location(dto.getLocation())
                .role(MemberRole.USER)
                .build();

        memberRepository.save(member);
    }


}

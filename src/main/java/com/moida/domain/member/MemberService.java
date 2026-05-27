package com.moida.domain.member;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.SignupRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.List;

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

    public Member findById(Long id) {
        // id로 회원 조회, 없으면 예외
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public List<Member> findAll() {
        // 최신 가입순으로 정렬
        return memberRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void updateMemberRole(Long id, MemberRole role) {
        // 회원 조회 후 역할 변경 (더티 체킹으로 자동 저장)
        Member member = findById(id);
        member.updateRole(role);
    }

}

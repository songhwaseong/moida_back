/*
package com.moida.test;

import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.member.MemberRole;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class MemberTest {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원 몇 명 추가하기")
    void insertMember() {

        Member mem01 = Member.builder()
                .memberNo("2026050900001")
                .name("관리자")
                .email("admin@naver.com")
                .password(passwordEncoder.encode("Admin@123"))
                .location("마포구 공덕동")
                .role(MemberRole.ADMIN)
                .build();

        memberRepository.save(mem01);
        System.out.println("----------------------------------------");

        Member mem02 = Member.builder()
                .memberNo("2026050900002")
                .name("홍길동")
                .email("hong@naver.com")
                .password(passwordEncoder.encode("Hong@456"))
                .location("용산구 이태원동")
                .role(MemberRole.USER)
                .build();

        memberRepository.save(mem02);
        System.out.println("----------------------------------------");

        Member mem03 = Member.builder()
                .memberNo("2026050900003")
                .name("곰돌이")
                .email("gomdori@naver.com")
                .password(passwordEncoder.encode("Gomdori@789"))
                .location("동대문구 휘경동")
                .role(MemberRole.USER)
                .build();

        memberRepository.save(mem03);
        System.out.println("----------------------------------------");
    }
}
*/

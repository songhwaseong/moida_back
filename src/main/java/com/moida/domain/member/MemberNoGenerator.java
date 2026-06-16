package com.moida.domain.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class MemberNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_DAILY_SEQUENCE = 99999;

    private final MemberRepository memberRepository;

    public String generate() {
        String prefix = LocalDateTime.now().format(DATE_FORMAT);
        int nextSequence = memberRepository.findTopByMemberNoStartingWithOrderByMemberNoDesc(prefix)
                .map(Member::getMemberNo)
                .map(memberNo -> parseSequence(prefix, memberNo) + 1)
                .orElse(1);

        if (nextSequence > MAX_DAILY_SEQUENCE) {
            throw new IllegalStateException("Daily member number sequence exceeded.");
        }

        return prefix + String.format("%05d", nextSequence);
    }

    private int parseSequence(String prefix, String memberNo) {
        if (memberNo == null || !memberNo.startsWith(prefix) || memberNo.length() <= prefix.length()) {
            return 0;
        }

        try {
            return Integer.parseInt(memberNo.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}

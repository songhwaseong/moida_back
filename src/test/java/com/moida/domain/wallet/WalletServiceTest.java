package com.moida.domain.wallet;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.BankAccountRequest;
import com.moida.common.request.WalletAmountRequest;
import com.moida.common.response.WalletResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WalletService에 대한 단위 테스트 클래스
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberBankAccountRepository accountRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    @DisplayName("계좌를 등록하고 변경할 수 있다")
    void saveAccountCreatesAndUpdatesAccount() {
        Member member = member();
        MemberBankAccount existingAccount = account(member, "신한은행", "1234567890", "홍길동");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(accountRepository.findByMemberId(MEMBER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(MemberBankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findAllByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        WalletResponse created = walletService.saveAccount(
                MEMBER_ID,
                new BankAccountRequest("신한은행", "1234567890", "홍길동")
        );
        WalletResponse updated = walletService.saveAccount(
                MEMBER_ID,
                new BankAccountRequest("국민은행", "123456789012", "김모이다")
        );

        assertThat(created.account().bank()).isEqualTo("신한은행");
        assertThat(updated.account().bank()).isEqualTo("국민은행");
        assertThat(updated.account().accountNumber()).isEqualTo("123456789012");
        assertThat(updated.account().verified()).isTrue();
    }

    @Test
    @DisplayName("계좌를 삭제한다")
    void deleteAccount() {
        Member member = member();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(transactionRepository.findAllByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        WalletResponse response = walletService.deleteAccount(MEMBER_ID);

        verify(accountRepository).deleteByMemberId(MEMBER_ID);
        assertThat(response.account()).isNull();
    }

    @Test
    @DisplayName("최소 금액 미만 충전은 거절한다")
    void rejectSmallDeposit() {
        assertThatThrownBy(() -> walletService.deposit(MEMBER_ID, new WalletAmountRequest(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_WALLET_AMOUNT);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("최소 금액 미만 출금은 거절한다")
    void rejectSmallWithdrawal() {
        assertThatThrownBy(() -> walletService.withdraw(MEMBER_ID, new WalletAmountRequest(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_WALLET_AMOUNT);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("충전 요청 시 잔액은 유지되고 PENDING 거래가 생성된다")
    void depositCreatesPendingTransactionWithoutIncreasingBalance() {
        Member member = member();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(accountRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
        when(transactionRepository.findAllByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        WalletResponse response = walletService.deposit(MEMBER_ID, new WalletAmountRequest(10_000L));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(response.balance()).isZero();
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransaction.TransactionType.DEPOSIT);
        assertThat(captor.getValue().getStatus()).isEqualTo(WalletTransaction.TransactionStatus.PENDING);
        assertThat(captor.getValue().getAmount()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("계좌가 없으면 출금을 거절한다")
    void rejectWithdrawalWithoutAccount() {
        Member member = member();
        member.chargeBalance(10_000L);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(accountRepository.existsByMemberId(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> walletService.withdraw(MEMBER_ID, new WalletAmountRequest(5_000L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔액 부족 출금을 거절한다")
    void rejectWithdrawalWithInsufficientBalance() {
        Member member = member();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(accountRepository.existsByMemberId(MEMBER_ID)).thenReturn(true);

        assertThatThrownBy(() -> walletService.withdraw(MEMBER_ID, new WalletAmountRequest(5_000L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("출금 요청 시 잔액은 유지되고 PENDING 거래가 생성된다")
    void withdrawalCreatesPendingTransactionWithoutDeductingBalance() {
        Member member = member();
        member.chargeBalance(10_000L);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(accountRepository.existsByMemberId(MEMBER_ID)).thenReturn(true);
        when(transactionRepository.findAllByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        WalletResponse response = walletService.withdraw(MEMBER_ID, new WalletAmountRequest(4_000L));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(response.balance()).isEqualTo(10_000L);
        assertThat(captor.getValue().getType()).isEqualTo(WalletTransaction.TransactionType.WITHDRAW);
        assertThat(captor.getValue().getStatus()).isEqualTo(WalletTransaction.TransactionStatus.PENDING);
        assertThat(captor.getValue().getAmount()).isEqualTo(4_000L);
    }

    private Member member() {
        Member member = Member.builder()
                .memberNo("2026052200001")
                .email("user@example.com")
                .password("password")
                .name("홍길동")
                .build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private MemberBankAccount account(Member member, String bank, String accountNumber, String holder) {
        return MemberBankAccount.builder()
                .member(member)
                .bank(bank)
                .accountNumber(accountNumber)
                .holder(holder)
                .build();
    }

}

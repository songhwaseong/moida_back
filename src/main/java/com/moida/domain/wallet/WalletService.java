package com.moida.domain.wallet;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.BankAccountRequest;
import com.moida.common.request.WalletAmountRequest;
import com.moida.common.response.AdminWalletTransactionResponse;
import com.moida.common.response.WalletResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import com.moida.domain.notification.Notification;
import com.moida.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지갑 잔액 충전/출금 및 출금 계좌 관리를 위한 비즈니스 로직을 제공하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    /** 최소 요청 가능 금액 (1,000원) */
    private static final long MIN_AMOUNT = 1000L;
    /** 조회할 최근 거래 내역 최대 개수 */
    private static final int RECENT_TRANSACTION_LIMIT = 50;
    /** 관리자 지갑 요청 조회 최대 개수 */
    private static final int MAX_ADMIN_TRANSACTION_SIZE = 200;

    private final MemberRepository memberRepository;
    private final MemberBankAccountRepository accountRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AdminActionLogService adminActionLogService;
    private final NotificationService notificationService;

    /**
     * 회원의 지갑 정보(잔액, 출금 계좌, 거래 내역)를 조회합니다.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long memberId) {
        Member member = findMember(memberId);
        return buildWalletResponse(member);
    }

    /**
     * 회원의 출금 계좌 정보를 등록하거나 갱신합니다.
     */
    @Transactional
    public WalletResponse saveAccount(Long memberId, BankAccountRequest request) {
        Member member = findMember(memberId);
        String bank = request.getBank().trim();
        String accountNumber = request.getAccountNumber().trim();
        String holder = request.getHolder().trim();
        MemberBankAccount account = accountRepository.findByMemberId(memberId)
                .orElseGet(() -> MemberBankAccount.builder()
                        .member(member)
                        .bank(bank)
                        .accountNumber(accountNumber)
                        .holder(holder)
                        .build());

        account.update(bank, accountNumber, holder);
        MemberBankAccount savedAccount = accountRepository.save(account);

        return buildWalletResponse(member, savedAccount);
    }

    /**
     * 회원의 출금 계좌 정보를 삭제합니다.
     */
    @Transactional
    public WalletResponse deleteAccount(Long memberId) {
        Member member = findMember(memberId);
        accountRepository.deleteByMemberId(memberId);
        return buildWalletResponse(member, null);
    }

    /**
     * 지갑 잔액 충전을 위해 PENDING 상태의 거래 내역을 생성합니다.
     * 실제 잔액 차감/증가는 관리자 승인 또는 입금 확인 후 별도로 처리됩니다.
     */
    @Transactional
    public WalletResponse deposit(Long memberId, WalletAmountRequest request) {
        validateAmount(request.getAmount());
        Member member = findMember(memberId);
        transactionRepository.save(WalletTransaction.builder()
                .member(member)
                .type(WalletTransaction.TransactionType.DEPOSIT)
                .status(WalletTransaction.TransactionStatus.PENDING)
                .amount(request.getAmount())
                .description("가상계좌 입금 대기")
                .build());

        return buildWalletResponse(member);
    }

    /**
     * 지갑 잔액 출금을 위해 PENDING 상태의 거래 내역을 생성합니다.
     * 계좌 존재 여부와 잔액 검증을 수행하며, 실제 잔액 차감은 출금 처리 완료 시 반영됩니다.
     */
    @Transactional
    public WalletResponse withdraw(Long memberId, WalletAmountRequest request) {
        validateAmount(request.getAmount());
        Member member = findMember(memberId);
        if (!accountRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.WALLET_ACCOUNT_NOT_FOUND);
        }
        if (member.getBalance() < request.getAmount()) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }

        transactionRepository.save(WalletTransaction.builder()
                .member(member)
                .type(WalletTransaction.TransactionType.WITHDRAW)
                .status(WalletTransaction.TransactionStatus.PENDING)
                .amount(request.getAmount())
                .description("출금 신청")
                .build());

        return buildWalletResponse(member);
    }

    /**
     * 가상계좌 송금 확인이 끝난 입금 요청을 완료 처리하고 회원 잔액에 반영합니다.
     */
    @Transactional
    public WalletResponse confirmDeposit(Long transactionId, String reason) {
        WalletTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_TRANSACTION_NOT_FOUND));
        validatePendingTransaction(transaction, WalletTransaction.TransactionType.DEPOSIT);
        Member member = memberRepository.findByIdForUpdate(transaction.getMember().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.chargeBalance(transaction.getAmount());
        transaction.completeDeposit();
        recordWalletAdminAction("WALLET_DEPOSIT_CONFIRM", transaction, reason);
        notificationService.createAndPush(
                member,
                Notification.NotificationType.WALLET_DEPOSIT_CONFIRMED,
                "입금이 승인됐어요",
                String.format("%,d원이 지갑 잔액에 반영되었습니다. 구매 내역에서 진행 중인 거래를 확인해보세요.",
                        transaction.getAmount()),
                "/my/purchases"
        );

        return buildWalletResponse(member);
    }

    /**
     * 출금 처리가 끝난 출금 요청을 완료 처리하고 회원 잔액에서 차감합니다.
     */
    @Transactional
    public WalletResponse confirmWithdrawal(Long transactionId, String reason) {
        WalletTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_TRANSACTION_NOT_FOUND));
        validatePendingTransaction(transaction, WalletTransaction.TransactionType.WITHDRAW);
        Member member = memberRepository.findByIdForUpdate(transaction.getMember().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getBalance() < transaction.getAmount()) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }

        member.deductBalance(transaction.getAmount());
        transaction.completeWithdrawal();
        recordWalletAdminAction("WALLET_WITHDRAWAL_CONFIRM", transaction, reason);
        notificationService.createAndPush(
                member,
                Notification.NotificationType.WALLET_WITHDRAWAL_CONFIRMED,
                "출금이 완료되었어요",
                String.format("%,d원이 등록된 계좌로 출금 처리되었습니다. 내 계좌에서 거래 내역을 확인해보세요.",
                        transaction.getAmount()),
                "/my/wallet"
        );

        return buildWalletResponse(member);
    }

    /**
     * 판매 정산금을 판매자의 거래 내역(입금/완료)으로 기록합니다.
     *
     * 잔액 증가(chargeBalance)는 {@link com.moida.domain.settlement.Settlement#payToSeller()} 가
     * 이미 수행하므로, 여기서는 계좌이력 화면에 노출될 wallet_transactions 행만 생성합니다.
     * (잔액을 다시 더하지 않도록 주의 — 중복 반영 금지)
     */
    @Transactional
    public void recordSettlementCredit(Member seller, long amount, String description) {
        if (amount <= 0) {
            return;
        }
        transactionRepository.save(WalletTransaction.builder()
                .member(seller)
                .type(WalletTransaction.TransactionType.DEPOSIT)
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .amount(amount)
                .description(description)
                .build());
    }

    /**
     * 경매 낙찰 결제(잔액 차감)를 구매자의 거래 내역(출금/완료)으로 기록합니다.
     *
     * 잔액 차감(deductBalance)은 결제 처리 로직이 이미 수행하므로, 여기서는 계좌이력 화면에
     * 노출될 wallet_transactions 행만 생성합니다. (잔액을 다시 차감하지 않도록 주의 — 중복 반영 금지)
     */
    @Transactional
    public void recordPaymentDebit(Member buyer, long amount, String description) {
        if (amount <= 0) {
            return;
        }
        transactionRepository.save(WalletTransaction.builder()
                .member(buyer)
                .type(WalletTransaction.TransactionType.WITHDRAW)
                .status(WalletTransaction.TransactionStatus.COMPLETED)
                .amount(amount)
                .description(description)
                .build());
    }

    /**
     * 아직 처리되지 않은 입금 요청을 취소합니다.
     */
    @Transactional
    public WalletResponse cancelDeposit(Long transactionId, String reason) {
        WalletTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_TRANSACTION_NOT_FOUND));
        validatePendingTransaction(transaction, WalletTransaction.TransactionType.DEPOSIT);

        transaction.cancelDeposit();
        recordWalletAdminAction("WALLET_DEPOSIT_CANCEL", transaction, reason);

        return buildWalletResponse(transaction.getMember());
    }

    /**
     * 아직 처리되지 않은 출금 요청을 취소합니다.
     */
    @Transactional
    public WalletResponse cancelWithdrawal(Long transactionId, String reason) {
        WalletTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_TRANSACTION_NOT_FOUND));
        validatePendingTransaction(transaction, WalletTransaction.TransactionType.WITHDRAW);

        transaction.cancelWithdrawal();
        recordWalletAdminAction("WALLET_WITHDRAWAL_CANCEL", transaction, reason);

        return buildWalletResponse(transaction.getMember());
    }

    /**
     * 관리자 화면에서 입출금 요청 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<AdminWalletTransactionResponse> getAdminTransactions(
            WalletTransaction.TransactionType type,
            WalletTransaction.TransactionStatus status,
            int size
    ) {
        if (size < 1 || size > MAX_ADMIN_TRANSACTION_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return transactionRepository.searchForAdmin(type, status, PageRequest.of(0, size)).stream()
                .map(transaction -> AdminWalletTransactionResponse.of(
                        transaction,
                        transaction.getType() == WalletTransaction.TransactionType.WITHDRAW
                                ? accountRepository.findByMemberId(transaction.getMember().getId()).orElse(null)
                                : null
                ))
                .toList();
    }

    /**
     * 요청 금액이 유효한지(null이 아니고 최소 금액 이상인지) 검증합니다.
     */
    private void validateAmount(Long amount) {
        if (amount == null || amount < MIN_AMOUNT) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_AMOUNT);
        }
    }

    /**
     * 주어진 거래가 기대 타입의 PENDING 거래인지 검증합니다.
     */
    private void validatePendingTransaction(WalletTransaction transaction, WalletTransaction.TransactionType type) {
        if (transaction.getType() != type || transaction.getStatus() != WalletTransaction.TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_TRANSACTION);
        }
    }

    /**
     * 지갑 응답 객체를 빌드합니다. (계좌 정보 자동 조회)
     */
    private WalletResponse buildWalletResponse(Member member) {
        MemberBankAccount account = accountRepository.findByMemberId(member.getId()).orElse(null);
        return buildWalletResponse(member, account);
    }

    /**
     * 주어진 계좌 정보를 활용하여 지갑 응답 객체를 빌드하고 최근 거래 내역을 첨부합니다.
     */
    private WalletResponse buildWalletResponse(Member member, MemberBankAccount account) {
        List<WalletTransaction> transactions = transactionRepository.findAllByMemberIdOrderByCreatedAtDesc(
                member.getId(),
                PageRequest.of(0, RECENT_TRANSACTION_LIMIT)
        );
        return WalletResponse.of(member, account, transactions);
    }

    /**
     * 회원 식별자로 회원 엔티티를 조회하며, 없으면 예외를 발생시킵니다.
     */
    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void recordWalletAdminAction(String actionType, WalletTransaction transaction, String reason) {
        adminActionLogService.record(
                actionType,
                "WALLET_TRANSACTION",
                transaction.getId(),
                transaction.getMember().getEmail(),
                adminActionLogService.fields("status", WalletTransaction.TransactionStatus.PENDING),
                adminActionLogService.fields(
                        "status", transaction.getStatus(),
                        "type", transaction.getType(),
                        "memberId", transaction.getMember().getId(),
                        "amount", transaction.getAmount(),
                        "description", transaction.getDescription()
                ),
                reason
        );
    }
}

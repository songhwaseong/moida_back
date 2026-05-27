package com.moida.domain.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberAddress 엔티티에 대한 데이터 액세스 처리를 담당하는 리포지토리 인터페이스
 */
public interface MemberAddressRepository extends JpaRepository<MemberAddress, Long> {

    /**
     * 회원 식별자로 주소 목록을 조회합니다.
     */
    List<MemberAddress> findAllByMemberIdOrderByDefaultAddressDescCreatedAtDescIdDesc(Long memberId);

    /**
     * 주소 식별자와 회원 식별자로 주소를 조회합니다.
     */
    Optional<MemberAddress> findByIdAndMemberId(Long id, Long memberId);

    /**
     * 해당 회원에게 등록된 주소가 존재하는지 여부를 확인합니다.
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 삭제 대상 주소를 제외하고 가장 최근에 생성된 주소를 조회합니다.
     */
    Optional<MemberAddress> findFirstByMemberIdAndIdNotOrderByCreatedAtDescIdDesc(Long memberId, Long id);

    /**
     * 해당 회원의 모든 기본 배송지 표시를 해제합니다.
     */
    @Modifying(flushAutomatically = true)
    @Query("update MemberAddress a set a.defaultAddress = false where a.member.id = :memberId and a.defaultAddress = true")
    void clearDefaultAddress(@Param("memberId") Long memberId);

    /**
     * 해당 회원의 특정 주소를 제외한 기본 배송지 표시를 해제합니다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update MemberAddress a
               set a.defaultAddress = false
             where a.member.id = :memberId
               and a.id <> :addressId
               and a.defaultAddress = true
            """)
    void clearOtherDefaultAddresses(@Param("memberId") Long memberId, @Param("addressId") Long addressId);
}

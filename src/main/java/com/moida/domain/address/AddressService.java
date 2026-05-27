package com.moida.domain.address;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AddressRequest;
import com.moida.common.response.AddressResponse;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원 배송지 주소 관리를 위한 비즈니스 로직을 제공하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final MemberRepository memberRepository;
    private final MemberAddressRepository addressRepository;

    /**
     * 회원의 주소 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long memberId) {
        return addressRepository.findAllByMemberIdOrderByDefaultAddressDescCreatedAtDescIdDesc(memberId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    /**
     * 회원의 주소를 등록합니다.
     */
    @Transactional
    public AddressResponse createAddress(Long memberId, AddressRequest request) {
        Member member = findMember(memberId);
        boolean firstAddress = !addressRepository.existsByMemberId(memberId);
        boolean defaultAddress = firstAddress || request.isDefaultRequested();
        // 첫 주소이거나 사용자가 기본 배송지로 지정한 경우, 기존 기본값을 먼저 해제한다.
        if (defaultAddress) {
            addressRepository.clearDefaultAddress(memberId);
        }

        MemberAddress address = MemberAddress.builder()
                .member(member)
                .name(normalizeRequired(request.getName()))
                .zonecode(normalizeRequired(request.getZonecode()))
                .address(normalizeRequired(request.getAddress()))
                .detail(normalizeOptional(request.getDetail()))
                .phone(normalizeRequired(request.getPhone()))
                .defaultAddress(defaultAddress)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }

    /**
     * 회원의 주소를 수정합니다.
     */
    @Transactional
    public AddressResponse updateAddress(Long memberId, Long addressId, AddressRequest request) {
        MemberAddress address = findAddress(memberId, addressId);
        boolean wasDefault = address.isDefaultAddress();

        // 명시적으로 기본 배송지로 설정하면 다른 주소의 기본 표시를 모두 해제한다.
        if (request.isDefaultRequested()) {
            addressRepository.clearOtherDefaultAddresses(memberId, addressId);
            address.update(
                    normalizeRequired(request.getName()),
                    normalizeRequired(request.getZonecode()),
                    normalizeRequired(request.getAddress()),
                    normalizeOptional(request.getDetail()),
                    normalizeRequired(request.getPhone()),
                    true
            );
            return AddressResponse.from(address);
        }

        // 기본 주소가 하나뿐인 경우에는 수정 중 체크가 해제되어도 기본 배송지를 유지한다.
        boolean keepDefault = wasDefault && addressRepository
                .findFirstByMemberIdAndIdNotOrderByCreatedAtDescIdDesc(memberId, addressId)
                .isEmpty();
        address.update(
                normalizeRequired(request.getName()),
                normalizeRequired(request.getZonecode()),
                normalizeRequired(request.getAddress()),
                normalizeOptional(request.getDetail()),
                normalizeRequired(request.getPhone()),
                keepDefault
        );

        // 기본 주소를 해제하는 수정이면 남은 주소 중 최근 주소를 기본 배송지로 승격한다.
        if (wasDefault && !keepDefault) {
            addressRepository.findFirstByMemberIdAndIdNotOrderByCreatedAtDescIdDesc(memberId, addressId)
                    .ifPresent(replacement -> replacement.setDefaultAddress(true));
        }

        return AddressResponse.from(address);
    }

    /**
     * 회원의 주소를 삭제합니다.
     */
    @Transactional
    public void deleteAddress(Long memberId, Long addressId) {
        MemberAddress address = findAddress(memberId, addressId);
        // 기본 배송지를 삭제할 때도 남은 주소가 있으면 기본 배송지가 하나 유지되도록 한다.
        if (address.isDefaultAddress()) {
            addressRepository.findFirstByMemberIdAndIdNotOrderByCreatedAtDescIdDesc(memberId, addressId)
                    .ifPresent(replacement -> replacement.setDefaultAddress(true));
        }
        addressRepository.delete(address);
    }

    /**
     * 특정 주소를 기본 배송지로 설정합니다.
     */
    @Transactional
    public AddressResponse setDefaultAddress(Long memberId, Long addressId) {
        MemberAddress address = findAddress(memberId, addressId);
        addressRepository.clearOtherDefaultAddresses(memberId, addressId);
        address.setDefaultAddress(true);
        return AddressResponse.from(address);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberAddress findAddress(Long memberId, Long addressId) {
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}

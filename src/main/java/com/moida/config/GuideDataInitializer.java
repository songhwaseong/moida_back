package com.moida.config;

import com.moida.domain.guide.Guide;
import com.moida.domain.guide.GuideRepository;
import com.moida.domain.guide.GuideStep;
import com.moida.domain.guide.GuideTip;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GuideDataInitializer implements ApplicationRunner {

    private final GuideRepository guideRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DefaultGuide guide : defaultGuides()) {
            if (guideRepository.existsByType(guide.type())) {
                guideRepository.findByType(guide.type()).ifPresent(saved ->
                        saved.update(
                                guide.tabLabel(),
                                guide.bannerLabel(),
                                guide.bannerTitle(),
                                guide.bannerDescription(),
                                guide.displayOrder(),
                                guide.steps(),
                                guide.tips()
                        )
                );
                continue;
            }

            guideRepository.save(Guide.builder()
                    .type(guide.type())
                    .tabLabel(guide.tabLabel())
                    .bannerLabel(guide.bannerLabel())
                    .bannerTitle(guide.bannerTitle())
                    .bannerDescription(guide.bannerDescription())
                    .displayOrder(guide.displayOrder())
                    .steps(guide.steps())
                    .tips(guide.tips())
                    .build());
        }
    }

    private List<DefaultGuide> defaultGuides() {
        return List.of(
                new DefaultGuide(
                        Guide.GuideType.BUY,
                        "구매",
                        "구매 가이드",
                        "안전하고 스마트한 구매",
                        "좋은 물건을 합리적인 가격에 구매하세요.",
                        1,
                        List.of(
                                step("검색", "상품 검색", "검색 또는 카테고리에서 원하는 상품을 찾아보세요. 상품 상태, 가격, 위치를 꼼꼼히 확인하세요."),
                                step("문의", "판매자 채팅", "궁금한 점은 채팅으로 문의해보세요. 직거래 장소, 추가 사진 요청 등을 상의할 수 있어요."),
                                step("결제", "안전결제", "안전결제를 이용하면 대금을 안전하게 보호받을 수 있어요. 직거래 시에는 현장에서 확인 후 결제하세요."),
                                step("수령", "상품 수령", "상품을 수령하면 이상이 없는지 꼭 확인하세요. 문제가 있다면 즉시 판매자 또는 고객센터에 연락하세요."),
                                step("후기", "후기 작성", "거래 완료 후 후기를 남겨주세요. 좋은 후기는 판매자에게 큰 힘이 됩니다.")
                        ),
                        List.of(
                                tip("확인", "직거래는 공공장소에서 진행하세요.", false),
                                tip("사진", "상품 상태를 사진으로 꼭 확인하세요.", false),
                                tip("채팅", "가격 조정은 채팅으로 정중하게 이야기해보세요.", false),
                                tip("주의", "계좌 직접 송금 요청은 주의하세요.", true),
                                tip("주의", "비정상적으로 저렴한 상품은 한 번 더 확인하세요.", true)
                        )
                ),
                new DefaultGuide(
                        Guide.GuideType.SELL,
                        "판매",
                        "판매 가이드",
                        "믿고 빠르게 내 물건 판매",
                        "내 물건을 좋은 구매자에게 안전하게 판매해보세요.",
                        2,
                        List.of(
                                step("촬영", "사진 촬영", "상품의 정면, 측면, 하자 부분을 밝은 곳에서 촬영하세요. 선명한 사진은 빠른 판매에 도움이 됩니다."),
                                step("등록", "상품 등록", "카테고리, 상태, 가격을 정확히 입력하세요. 경매 상품은 시작가와 최소 입찰 단위도 설정할 수 있어요."),
                                step("응답", "구매자 응대", "문의 채팅에 빠르게 답할수록 판매 성공률이 높아집니다. 친절한 응대가 좋은 후기로 이어져요."),
                                step("거래", "거래 확정", "거래 장소와 시간을 확정하세요. 직거래는 현장 확인 후 거래를 완료해주세요."),
                                step("정산", "수익 출금", "판매 수익은 내 지갑에 적립되며, 등록한 계좌로 출금 신청할 수 있어요.")
                        ),
                        List.of(
                                tip("설명", "상품 설명은 최대한 자세하게 작성하세요.", false),
                                tip("가격", "시세보다 10~15% 낮게 설정하면 빠르게 판매될 수 있어요.", false),
                                tip("사진", "여러 장의 선명한 사진이 판매에 유리합니다.", false),
                                tip("주의", "허위 상품 설명은 제재 대상입니다.", true),
                                tip("주의", "거래 완료 후 개인정보 요구는 거절하세요.", true)
                        )
                ),
                new DefaultGuide(
                        Guide.GuideType.AUCTION,
                        "경매",
                        "경매 가이드",
                        "짜릿한 경매 낙찰 도전",
                        "시작가부터 경쟁하며 원하는 가격에 낙찰받아보세요.",
                        3,
                        List.of(
                                step("탐색", "경매 탐색", "실시간 경매 목록에서 관심 있는 상품을 찾아보세요. 마감 시간과 현재 입찰가를 확인하세요."),
                                step("충전", "금액 충전", "입찰 전 지갑에 충분한 금액이 있는지 확인하세요. 보유 금액이 입찰가보다 많아야 참여할 수 있어요."),
                                step("입찰", "입찰 참여", "현재 최고 입찰가보다 높은 금액을 입력하고 입찰을 확정하세요. 최소 입찰 단위 이상으로 올려야 합니다."),
                                step("낙찰", "낙찰 확인", "경매 종료 후 최고 입찰자가 낙찰됩니다. 낙찰 알림을 받으면 판매자와 거래를 이어가세요."),
                                step("결제", "대금 지급 및 수령", "낙찰 금액은 지갑에서 차감됩니다. 판매자와 협의하여 직거래 또는 배송으로 상품을 수령하세요.")
                        ),
                        List.of(
                                tip("시간", "마감 직전 입찰은 가격이 빠르게 오를 수 있어요.", false),
                                tip("알림", "관심 경매를 등록해 알림을 받아보세요.", false),
                                tip("예산", "최대 예산을 미리 정하고 입찰에 참여하세요.", false),
                                tip("주의", "입찰 확정 후에는 취소가 어렵습니다.", true),
                                tip("주의", "금액 부족 시 입찰할 수 없으니 미리 충전하세요.", true)
                        )
                )
        );
    }

    private GuideStep step(String icon, String title, String description) {
        return GuideStep.builder()
                .icon(icon)
                .title(title)
                .description(description)
                .build();
    }

    private GuideTip tip(String icon, String text, boolean warning) {
        return GuideTip.builder()
                .icon(icon)
                .text(text)
                .warning(warning)
                .build();
    }

    private record DefaultGuide(
            Guide.GuideType type,
            String tabLabel,
            String bannerLabel,
            String bannerTitle,
            String bannerDescription,
            Integer displayOrder,
            List<GuideStep> steps,
            List<GuideTip> tips
    ) {
    }
}

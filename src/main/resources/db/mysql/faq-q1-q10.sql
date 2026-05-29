CREATE DATABASE IF NOT EXISTS moida
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE moida;

CREATE TABLE IF NOT EXISTS faqs (
  faq_id BIGINT NOT NULL AUTO_INCREMENT,
  category VARCHAR(50) NOT NULL,
  question VARCHAR(255) NOT NULL,
  answer TEXT NOT NULL,
  display_order INT NOT NULL,
  is_visible BIT(1) NOT NULL DEFAULT b'1',
  visible BIT(1) NOT NULL DEFAULT b'1',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (faq_id),
  UNIQUE KEY uq_faqs_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO faqs (faq_id, category, question, answer, display_order, is_visible, visible) VALUES
(1, '경매', '경매(Auction)와 일반 중고 거래(Trade)의 차이점은 무엇인가요?', '일반 거래는 판매자가 등록한 가격에 구매자가 즉시 구매하는 방식입니다. 경매 거래는 설정하신 시작가부터 시작하여 정해진 기간 동안 구매자들이 입찰 경쟁을 벌여 가장 높은 금액을 제시한 사람에게 낙찰되는 방식입니다. 등록 시 두 가지 타입 중 하나를 선택하실 수 있습니다.', 1, b'1', b'1'),
(2, '경매', '경매 시작가와 즉시 구매가는 어떻게 설정하는 것이 좋은가요?', '시작 가격(Min Bid)은 상품의 상태(S~C 등급)를 고려하여 ''이 가격 이하라면 팔지 않겠다''는 최소한의 금액으로 설정하시는 것이 안전합니다. 만약 경매 기간을 기다리지 않고 바로 판매하고 싶으시다면, 시세보다 약간 높은 금액으로 즉시 구매가(Immediate Price)를 함께 설정해 두시면 빠른 거래에 도움이 됩니다.', 2, b'1', b'1'),
(3, '경매', '낙찰자가 물건을 낙찰받아 놓고 대금을 결제하지 않으면 어떻게 되나요?', '낙찰 후 24시간 이내에 구매자가 결제하지 않으면 거래는 자동 취소되며, 해당 구매자에게는 노쇼(No-Show)로 인한 서비스 이용 제한(Sanctions) 페널티가 부여됩니다. 판매자님은 [재경매 등록]을 통해 상품을 다시 올리시거나, [차순위 입찰자에게 판매] 기능을 통해 다음으로 높은 금액을 부른 입찰자에게 판매 제안을 하실 수 있습니다.', 3, b'1', b'1'),
(4, '경매', '경매가 진행 중(Live)일 때 중간에 글을 수정하거나 경매를 취소할 수 있나요?', '입찰자가 아무도 없는 상태에서는 언제든지 글 수정 및 경매 취소가 가능합니다. 하지만 이미 입찰(Bids)이 발생한 이후에는 다른 참여자들의 공정한 경쟁을 위해 내용을 수정하거나 경매를 임의로 취소하실 수 없습니다. 상품 정보를 등록하실 때 신중하게 작성해 주세요.', 4, b'1', b'1'),
(5, '경매', '경매가 마감되었는데 입찰자가 아무도 없어요. 어떻게 해야 하나요?', '입찰자가 없이 마감된 경매는 유찰(FAILED) 상태로 변경됩니다. 유찰 시 별도의 수수료나 페널티는 발생하지 않으며, 판매자님은 시작 가격을 조금 낮추거나 상품 설명을 보완하여 언제든지 재경매를 진행하실 수 있습니다.', 5, b'1', b'1'),
(6, '경매', '경매 참여자가 가격을 인위적으로 올리는 것 같아요. 조작이 의심되면 어떻게 하나요?', '모이다 플랫폼은 AI 기반의 이상 입찰 탐지 시스템(is_suspicious)을 가동하고 있습니다. 동일한 IP에서의 반복 입찰이나 비정상적인 가격 폭등이 감지되면 시스템이 자동으로 해당 입찰을 차단합니다. 만약 의심스러운 정황을 발견하시면 상품 페이지 내 [신고하기(Reports)] 버튼을 통해 제보해 주시면 운영팀에서 즉시 조사에 착수합니다.', 6, b'1', b'1'),
(7, '경매', '상품 문의(Inquiries)나 채팅으로 따로 연락해서 직거래를 하자고 하는데 응해도 되나요?', '경매 진행 중 플랫폼 외부에서 따로 돈을 주고받는 외부 직거래 유도는 서비스 이용 약관 위반에 해당하며, 사기 피해의 위험이 매우 높습니다. 이를 응하거나 유도할 경우 판매자님도 계정 정지(Suspended) 처리를 받으실 수 있으니, 반드시 안전한 플랫폼 내 입찰 시스템을 이용해 주세요. 비밀 문의(is_secret) 기능을 활용해 무리한 네고 요청은 정중히 거절하시는 것을 권장합니다.', 7, b'1', b'1'),
(8, '경매', '경매 낙찰 후 판매 대금은 언제 저의 지갑(Wallet)으로 정산되나요?', '구매자가 상품을 배송받은 후 [구매 확정]을 누르면, 플랫폼 이용 수수료(fee_rate)를 제외한 최종 금액이 즉시 판매자님의 지갑 잔액(balance)으로 정산(Settlements)됩니다. 정산된 금액은 등록하신 계좌로 즉시 출금하실 수 있습니다.', 8, b'1', b'1'),
(9, '경매', '구매자가 물건을 받은 후 단순 변심으로 환불이나 반품을 요구하는데, 해줘야 하나요?', '경매 거래는 특성상 입찰 경쟁을 통해 낙찰자가 결정되므로, 구매자의 단순 변심으로 인한 환불 및 반품은 거부하실 수 있습니다. 단, 상품 등록 시 기재했던 등급(S, A, B, C)이나 설명과 실제 물건의 상태가 현저히 달라 분쟁(Reports)이 발생한 경우에는 운영팀의 중재 하에 환불 절차가 진행될 수 있으니 상품 상태를 투명하게 적어주시는 것이 중요합니다.', 9, b'1', b'1'),
(10, '경매', '판매자 사정으로 낙찰된 물건을 보내지 못하면 어떤 제재를 받게 되나요?', '낙찰이 완료된 후 판매자가 일방적으로 거래를 파기하거나 물건을 발송하지 않을 경우, 구매자 보호 정책에 따라 경고 및 일정 기간 서비스 이용이 정지되는 패널티(suspended_until)를 받게 됩니다. 더불어 판매자 매너 온도(manner_temp)가 크게 하락하여 향후 다른 거래 시 불이익을 받으실 수 있습니다.', 10, b'1', b'1')
ON DUPLICATE KEY UPDATE
  category = VALUES(category),
  question = VALUES(question),
  answer = VALUES(answer),
  display_order = VALUES(display_order),
  is_visible = VALUES(is_visible),
  visible = VALUES(visible),
  updated_at = CURRENT_TIMESTAMP;

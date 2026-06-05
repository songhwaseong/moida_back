package com.moida.domain.category;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.CategoryReorderRequest;
import com.moida.common.response.AdminCategoryResponse;
import com.moida.domain.audit.AdminActionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 카테고리 관리 서비스.
 * 현재 화면이 지원하는 동작은 (1) 노출 토글, (2) 순서 일괄 변경 두 가지뿐.
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private static final String HIDDEN_CATEGORY_NAME = "이월상품";

    private final CategoryRepository categoryRepository;
    private final AdminActionLogService adminActionLogService;

    /** 전체 카테고리 목록 (활성/비활성 모두 포함, displayOrder ASC) */
    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(category -> !HIDDEN_CATEGORY_NAME.equals(category.getName()))
                .map(AdminCategoryResponse::from)
                .toList();
    }

    /** 노출 여부 토글 */
    @Transactional
    public AdminCategoryResponse setVisibility(Long id, boolean visible) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        Boolean previousVisible = category.getIsActive();
        if (visible) category.activate(); else category.deactivate();
        adminActionLogService.record(
                "CATEGORY_VISIBILITY_CHANGE",
                "CATEGORY",
                category.getId(),
                category.getName(),
                adminActionLogService.fields("visible", previousVisible),
                adminActionLogService.fields("visible", category.getIsActive()),
                "카테고리 노출 여부 변경"
        );
        return AdminCategoryResponse.from(category);
    }

    /**
     * 순서 일괄 변경. 클라이언트가 드래그 후 새 순서 전체를 보낸다.
     * 트랜잭션 안에서 한 번에 처리해 중간 실패 시 원자적으로 롤백된다.
     */
    @Transactional
    public List<AdminCategoryResponse> reorder(CategoryReorderRequest request) {
        if (request.getOrders() == null || request.getOrders().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "변경할 순서 데이터가 없습니다.");
        }
        // id 별로 displayOrder 매핑
        Map<Long, Integer> updates = new HashMap<>();
        for (CategoryReorderRequest.Item item : request.getOrders()) {
            if (item.getId() == null || item.getDisplayOrder() == null) continue;
            updates.put(item.getId(), item.getDisplayOrder());
        }

        List<Category> all = categoryRepository.findAllByOrderByDisplayOrderAscIdAsc();
        List<Map<String, Object>> beforeValue = all.stream()
                .filter(category -> updates.containsKey(category.getId()))
                .map(category -> adminActionLogService.fields(
                        "id", category.getId(),
                        "name", category.getName(),
                        "displayOrder", category.getDisplayOrder()
                ))
                .toList();
        for (Category category : all) {
            Integer next = updates.get(category.getId());
            if (next != null && !next.equals(category.getDisplayOrder())) {
                category.update(null, null, next);
            }
        }
        List<Map<String, Object>> afterValue = all.stream()
                .filter(category -> updates.containsKey(category.getId()))
                .map(category -> adminActionLogService.fields(
                        "id", category.getId(),
                        "name", category.getName(),
                        "displayOrder", category.getDisplayOrder()
                ))
                .toList();
        adminActionLogService.record(
                "CATEGORY_REORDER",
                "CATEGORY",
                null,
                "카테고리 순서",
                beforeValue,
                afterValue,
                "카테고리 순서 변경"
        );
        // 변경 후 정렬된 결과를 다시 조회해 응답한다.
        return categoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .filter(category -> !HIDDEN_CATEGORY_NAME.equals(category.getName()))
                .map(AdminCategoryResponse::from)
                .toList();
    }
}

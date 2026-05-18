package com.moida;

import com.moida.domain.category.Category;
import com.moida.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;

@EnableJpaAuditing
@SpringBootApplication
@RequiredArgsConstructor
public class MoidaBackendApplication {

    private final CategoryRepository categoryRepository;

    public static void main(String[] args) {
        SpringApplication.run(MoidaBackendApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            if (categoryRepository.count() > 0) return;

            List<String[]> categories = List.of(
                    new String[]{"디지털/가전", "📱"},
                    new String[]{"패션/의류", "👗"},
                    new String[]{"명품", "💎"},
                    new String[]{"시계/주얼리", "⌚"},
                    new String[]{"신발", "👟"},
                    new String[]{"스포츠/레저", "⚽"},
                    new String[]{"뷰티/미용", "💄"},
                    new String[]{"게임/취미", "🎮"},
                    new String[]{"음향/악기", "🎵"},
                    new String[]{"한정판", "⭐"},
                    new String[]{"이월상품", "🏷️"}
            );

            for (int i = 0; i < categories.size(); i++) {
                categoryRepository.save(
                        Category.builder()
                                .name(categories.get(i)[0])
                                .emoji(categories.get(i)[1])
                                .displayOrder(i + 1)
                                .build()
                );
            }
        };
    }
}
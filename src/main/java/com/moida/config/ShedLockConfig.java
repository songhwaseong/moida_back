package com.moida.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock 설정 — 다중 인스턴스(EC2 2대) 환경에서 @Scheduled 중복 실행 방지.
 *
 * 동작: 스케줄 메서드 진입 시 shedlock 테이블에 락 row 를 선점한 인스턴스만 실행하고,
 *       나머지 인스턴스는 그 주기를 건너뛴다. 이렇게 클러스터 전체에서 단일 실행을 보장한다.
 *
 * defaultLockAtMostFor: 모든 @SchedulerLock 의 기본 lockAtMostFor.
 *   락을 잡은 인스턴스가 락을 풀지 못하고 죽어도(JVM kill 등) 이 시간이 지나면
 *   락이 자동 만료돼 다른 인스턴스가 이어받는다. (가장 짧은 스케줄 주기보다 작게 잡는다)
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT50S")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // ShedLock 락 테이블은 JPA 엔티티가 아니라 ddl-auto(update)로 생성되지 않는다.
        // 별도 마이그레이션 도구가 없으므로 기동 시 멱등(IF NOT EXISTS) DDL 로 직접 보장한다.
        // → EC2 어느 인스턴스가 먼저 떠도 안전하게 테이블이 준비된다.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS shedlock (
                    name       VARCHAR(64)  NOT NULL,
                    lock_until TIMESTAMP(3) NOT NULL,
                    locked_at  TIMESTAMP(3) NOT NULL,
                    locked_by  VARCHAR(255) NOT NULL,
                    PRIMARY KEY (name)
                )
                """);

        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(jdbcTemplate)
                        // 인스턴스 간 시계 오차(clock skew)에 휘둘리지 않도록 락 만료를 DB 시간 기준으로 판단.
                        .usingDbTime()
                        .build()
        );
    }
}

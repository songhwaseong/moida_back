package com.moida.domain.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findAllByStatus(Report.ReportStatus status, Pageable pageable);

    long countByStatus(Report.ReportStatus status);
}

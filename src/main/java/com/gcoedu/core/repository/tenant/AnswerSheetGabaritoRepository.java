package com.gcoedu.core.repository.tenant;

import com.gcoedu.core.domain.entity.tenant.AnswerSheetGabarito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerSheetGabaritoRepository extends JpaRepository<AnswerSheetGabarito, String> {
    List<AnswerSheetGabarito> findByTestId(String testId);
}

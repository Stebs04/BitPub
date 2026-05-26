package com.bitpub.stats.specification;

import com.bitpub.common.specification.BaseSpecification;
import com.bitpub.common.specification.SearchCriteria;
import com.bitpub.stats.model.PlayerStats;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class StatsSpecification {
    public static Specification<PlayerStats> createSpecification(List<SearchCriteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return null;
        }

        Specification<PlayerStats> result = new BaseSpecification<>(criteriaList.get(0));

        for (int i = 1; i < criteriaList.size(); i++) {
            result = Specification.where(result).and(new BaseSpecification<>(criteriaList.get(i)));
        }

        return result;
    }
}

package com.bitpub.game.specification;

import com.bitpub.common.specification.BaseSpecification;
import com.bitpub.common.specification.SearchCriteria;
import com.bitpub.game.model.Game;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class GameSpecification {
    public static Specification<Game> createSpecification(List<SearchCriteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return null;
        }

        Specification<Game> result = new BaseSpecification<>(criteriaList.get(0));

        for (int i = 1; i < criteriaList.size(); i++) {
            result = Specification.where(result).and(new BaseSpecification<>(criteriaList.get(i)));
        }

        return result;
    }
}

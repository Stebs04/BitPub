package com.bitpub.auth.specification;

import com.bitpub.auth.model.User;
import com.bitpub.common.specification.BaseSpecification;
import com.bitpub.common.specification.SearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    
    public static Specification<User> createSpecification(List<SearchCriteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return null;
        }

        Specification<User> result = new BaseSpecification<>(criteriaList.get(0));

        for (int i = 1; i < criteriaList.size(); i++) {
            result = Specification.where(result).and(new BaseSpecification<>(criteriaList.get(i)));
        }

        return result;
    }
}

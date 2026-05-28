package com.bitpub.common.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;

public class BaseSpecification<T> implements Specification<T> {

    private final SearchCriteria criteria;

    public BaseSpecification(SearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (criteria == null || criteria.getKey() == null || criteria.getValue() == null) {
            return null;
        }

        switch (criteria.getOperation()) {
            case EQUALITY:
                return builder.equal(root.get(criteria.getKey()), criteria.getValue());
            case NEGATION:
                return builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
            case GREATER_THAN:
                if (criteria.getValue() instanceof LocalDateTime) {
                    return builder.greaterThan(root.get(criteria.getKey()), (LocalDateTime) criteria.getValue());
                } else if (criteria.getValue() instanceof Number) {
                    return builder.gt(root.get(criteria.getKey()), (Number) criteria.getValue());
                }
                return builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LESS_THAN:
                if (criteria.getValue() instanceof LocalDateTime) {
                    return builder.lessThan(root.get(criteria.getKey()), (LocalDateTime) criteria.getValue());
                } else if (criteria.getValue() instanceof Number) {
                    return builder.lt(root.get(criteria.getKey()), (Number) criteria.getValue());
                }
                return builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LIKE:
                return builder.like(builder.lower(root.get(criteria.getKey())), criteria.getValue().toString().toLowerCase());
            case STARTS_WITH:
                return builder.like(builder.lower(root.get(criteria.getKey())), criteria.getValue().toString().toLowerCase() + "%");
            case ENDS_WITH:
                return builder.like(builder.lower(root.get(criteria.getKey())), "%" + criteria.getValue().toString().toLowerCase());
            case CONTAINS:
                return builder.like(builder.lower(root.get(criteria.getKey())), "%" + criteria.getValue().toString().toLowerCase() + "%");
            case IN:
                if (criteria.getValue() instanceof Collection) {
                    return root.get(criteria.getKey()).in((Collection<?>) criteria.getValue());
                }
                return null;
            default:
                return null;
        }
    }
}

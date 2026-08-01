package com.lampify.repository;

import com.lampify.dto.ProductSearchCriteria;
import com.lampify.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Product> searchProducts(ProductSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> product = query.from(Product.class);
        product.fetch("category", JoinType.INNER);
        product.fetch("images", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, product, criteria);
        query.select(product).distinct(true);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        query.orderBy(buildOrders(cb, product, criteria));

        TypedQuery<Product> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());
        return typedQuery.getResultList();
    }

    @Override
    public long countProducts(ProductSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Product> product = query.from(Product.class);
        product.join("category", JoinType.INNER);

        List<Predicate> predicates = buildPredicates(cb, product, criteria);
        query.select(cb.countDistinct(product));
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public List<String> findDistinctBrands() {
        return entityManager.createQuery("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand", String.class)
                .getResultList();
    }

    @Override
    public BigDecimal findMinPrice() {
        return entityManager.createQuery("SELECT MIN(p.price) FROM Product p", BigDecimal.class)
                .getSingleResult();
    }

    @Override
    public BigDecimal findMaxPrice() {
        return entityManager.createQuery("SELECT MAX(p.price) FROM Product p", BigDecimal.class)
                .getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Product> product, ProductSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        if (hasText(criteria.getSearch())) {
            String pattern = "%" + criteria.getSearch().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(product.get("name")), pattern),
                    cb.like(cb.lower(product.get("description")), pattern),
                    cb.like(cb.lower(product.get("brand")), pattern)
            ));
        }
        if (hasText(criteria.getCategory())) {
            predicates.add(cb.equal(product.get("category").get("slug"), criteria.getCategory().trim()));
        }
        if (hasText(criteria.getBrand())) {
            predicates.add(cb.equal(product.get("brand"), criteria.getBrand().trim()));
        }
        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get("price"), criteria.getMinPrice()));
        }
        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get("price"), criteria.getMaxPrice()));
        }

        return predicates;
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<Product> product, ProductSearchCriteria criteria) {
        String sort = criteria.getSort() == null ? "relevance" : criteria.getSort().toLowerCase();
        return switch (sort) {
            case "price_asc" -> List.of(cb.asc(product.get("price")));
            case "price_desc" -> List.of(cb.desc(product.get("price")));
            case "rating" -> List.of(cb.desc(product.get("rating")), cb.asc(product.get("name")));
            case "name" -> List.of(cb.asc(product.get("name")));
            default -> hasText(criteria.getSearch())
                    ? List.of(cb.asc(product.get("name")), cb.desc(product.get("rating")))
                    : List.of(cb.desc(product.get("rating")), cb.asc(product.get("name")));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

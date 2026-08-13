package com.ai.learning.planner.dto.assessment;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页结果包装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int pages;

    public static <T> PageResult<T> from(Page<T> pageData) {
        return PageResult.<T>builder()
                .records(pageData.getContent())
                .total(pageData.getTotalElements())
                .page(pageData.getNumber() + 1)
                .size(pageData.getSize())
                .pages(pageData.getTotalPages())
                .build();
    }
}
package com.geli.warehouse.web;

import com.geli.warehouse.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequests {

    private PageRequests() {
    }

    public static Pageable of(int page, int size, String sort, Set<String> allowed) {
        if (page < 0) {
            throw new BadRequestException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size must be between 1 and 100");
        }
        String raw = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();
        String[] parts = raw.split(",");
        if (parts.length > 2) {
            throw new BadRequestException("Sort must look like field,asc or field,desc");
        }
        String field = parts[0].trim();
        if (!allowed.contains(field)) {
            throw new BadRequestException("Unsupported sort field: " + field);
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            String dir = parts[1].trim();
            if (!dir.equalsIgnoreCase("asc") && !dir.equalsIgnoreCase("desc")) {
                throw new BadRequestException("Sort direction must be asc or desc");
            }
            direction = Sort.Direction.fromString(dir);
        }
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}

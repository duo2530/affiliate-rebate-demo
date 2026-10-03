package com.techplant.affiliate.common;

import java.util.List;

public record PageData<T>(int page, int pageSize, long total, List<T> list) {
}

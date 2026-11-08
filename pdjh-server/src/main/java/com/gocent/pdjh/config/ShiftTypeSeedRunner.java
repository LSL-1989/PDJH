package com.gocent.pdjh.config;

import com.gocent.pdjh.service.ShiftTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Hibernate 建好 dict_shift_type 后，空表写入上午/下午/全天/停诊。
 * 只在缺数据时补，不覆盖已维护内容。
 */
@Component
@Order(102)
@RequiredArgsConstructor
public class ShiftTypeSeedRunner implements ApplicationRunner {

    private final ShiftTypeService shiftTypeService;

    @Override
    public void run(ApplicationArguments args) {
        shiftTypeService.seedIfEmpty();
    }
}

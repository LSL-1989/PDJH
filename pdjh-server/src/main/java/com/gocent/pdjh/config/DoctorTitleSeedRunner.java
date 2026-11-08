package com.gocent.pdjh.config;

import com.gocent.pdjh.service.DoctorTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Hibernate 建好 dict_doctor_title 后，空表写入默认职称。
 * 与菜单、超管种子一样只在缺数据时补，不覆盖已维护内容。
 */
@Component
@Order(102)
@RequiredArgsConstructor
public class DoctorTitleSeedRunner implements ApplicationRunner {

    private final DoctorTitleService doctorTitleService;

    @Override
    public void run(ApplicationArguments args) {
        doctorTitleService.seedIfEmpty();
    }
}

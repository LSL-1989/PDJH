package com.gocent.pdjh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 医生头像静态资源映射（DoctorService 上传目录一致）
 * 访问路径：/api/uploads/intros/{filename}
 */
@Configuration
public class DoctorAvatarResourceConfig implements WebMvcConfigurer {

    public static Path avatarUploadDir() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "pdjh-intros");
    }

    /** @deprecated 兼容旧引用 */
    public static Path introUploadDir() {
        return avatarUploadDir();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = avatarUploadDir().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/intros/**")
                .addResourceLocations(location);
    }
}

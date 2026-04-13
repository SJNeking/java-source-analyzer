package cn.dolphinmind.glossary.java.analyze;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 * 
 * 整合了 MyBatis, Redisson, PostgreSQL 等组件。
 */
@SpringBootApplication
@MapperScan("cn.dolphinmind.glossary.java.analyze.mapper")
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
        System.out.println("✅ CodeGuardian Backend v3.5 Started Successfully!");
        System.out.println("🌐 API Docs: http://localhost:8080/api/v1/docs (Coming Soon)");
    }
}

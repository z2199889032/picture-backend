package com.zzm.picturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * PictureBackendApplication 是系统的主应用程序类。
 * 它使用了Spring Boot的自动配置和MyBatis的Mapper扫描功能。
 * 管理员账号：root
 * 密码：123456789
 */
@EnableAsync
// 启用AspectJ自动代理，exposeProxy设为true以方便在服务层获取当前代理对象
@EnableAspectJAutoProxy(exposeProxy = true)
// 标识这是一个Spring Boot应用，同时自动配置和组件扫描会生效
@SpringBootApplication
// 指定MyBatis的Mapper接口所在包，自动扫描该包下的所有Mapper接口
@MapperScan("com.zzm.picturebackend.mapper")
public class PictureBackendApplication {

    /**
     * 主程序入口。
     * 启动Spring Boot应用，参数为命令行参数。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PictureBackendApplication.class, args);
    }

}

# 逸云图库 - 后端服务

[![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen)](https://spring.io/projects/spring-boot)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.9-blue)](https://baomidou.com/)
[![Knife4j](https://img.shields.io/badge/Knife4j-4.4.0-orange)](https://doc.xiaominfo.com/)
[![JDK](https://img.shields.io/badge/JDK-17%2B-green)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

逸云图库是一个功能强大的图片管理系统，提供图片存储、分类、元数据管理和智能审核等功能。本仓库包含逸云图库的后端服务代码。

## 技术架构

- **核心框架**: Spring Boot 2.7.x
- **ORM 框架**: MyBatis-Plus 3.5.9
- **安全认证**: Spring Session + Redis
- **接口文档**: Knife4j 4.4.0 (Swagger 增强)
- **数据存储**: 
  - MySQL 8.x
  - Redis 7.x
- **缓存策略**:
  - Redis 分布式缓存
  - Caffeine 本地缓存
- **对象存储**: 腾讯云 COS
- **工具链**: 
  - Lombok
  - Hutool 5.8.26
  - JSoup 1.15.3

## 功能特性

- **图片管理**
  - 图片元数据管理（EXIF解析/存储）
  - 图片主色调提取
  - 缩略图自动生成
- **多空间存储策略**
  - 私有/团队空间
  - 空间容量管理
  - 空间成员权限控制
- **RBAC 权限控制系统**
  - 用户角色管理
  - 空间角色：viewer/editor/admin
- **智能审核工作流**
  - 图片审核状态管理
  - 审核流程追踪
- **可视化数据分析接口**
- **批量操作API**
- **分布式Session管理**

## 项目结构

```
picture-backend/
├── src/                # 源代码目录
│   ├── main/java/     # Java 源代码
│   └── main/resources/ # 配置文件
├── sql/               # 数据库脚本
│   └── create_table_sql.sql # 建表语句
└── pom.xml            # Maven 依赖配置
```

## 快速启动

### 环境要求
- JDK 17+
- MySQL 8.x
- Redis 7.x
- Maven 3.8+

### 数据库配置
1. 创建数据库：
```sql
CREATE DATABASE picture CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本：
```bash
mysql -u your_username -p picture < sql/create_table_sql.sql
```

### 应用配置
1. 修改 `application.yml` 配置文件：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/picture?serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password # 如果有

# 腾讯云对象存储配置（如使用）
cos:
  client:
    secretId: your_secret_id
    secretKey: your_secret_key
    region: ap-region
    bucketName: your_bucket_name
```

### 构建与运行

```bash
# 克隆项目
git clone https://your-repository-url/picture-backend.git
cd picture-backend

# 编译打包
mvn clean package -DskipTests

# 运行应用
java -jar target/picture-backend-0.0.1-SNAPSHOT.jar
```

### 接口文档

启动应用后，访问 Knife4j 接口文档：
```
http://localhost:8080/doc.html
```

## 核心数据模型

- **用户 (User)**: 系统用户信息
- **图片 (Picture)**: 图片元数据和存储信息
- **空间 (Space)**: 图片存储空间
- **空间用户 (SpaceUser)**: 空间与用户的关联关系

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

[MIT License](LICENSE)
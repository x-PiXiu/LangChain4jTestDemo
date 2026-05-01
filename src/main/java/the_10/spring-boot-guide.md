# Spring Boot 快速入门指南

## 项目创建

Spring Boot 项目可以通过 Spring Initializr 快速生成。访问 start.spring.io，选择依赖后下载即可。
推荐使用 Maven 构建，JDK 17+ 环境。

## 配置文件

Spring Boot 使用 application.yml 或 application.properties 进行配置。
常用配置包括：服务端口（server.port）、数据库连接（spring.datasource）、日志级别（logging.level）。
配置文件放在 src/main/resources 目录下。

## 数据库访问

Spring Data JPA 是最常用的数据库访问方案。
定义一个接口继承 JpaRepository，即可获得增删改查能力，无需写 SQL。
复杂查询可以用 @Query 注解自定义 JPQL。

## REST API开发

使用 @RestController 和 @GetMapping 等注解可以快速定义 REST 接口。
参数绑定用 @RequestParam（查询参数）和 @PathVariable（路径参数）。
返回值自动序列化为 JSON，无需手动转换。

## 异常处理

全局异常处理用 @ControllerAdvice + @ExceptionHandler。
可以统一封装错误响应格式，避免每个 Controller 重复写 try-catch。
建议定义一个通用的 ApiResponse 类，包含 code、message、data 三个字段。

## 安全认证

Spring Security 是 Spring 生态的安全框架。
常用方案：JWT Token 认证。用户登录后签发 Token，后续请求携带 Token 验证身份。
注意：密码必须加密存储，推荐 BCryptPasswordEncoder。

## 部署

打包命令：mvn clean package -DskipTests
生成的 JAR 文件用 java -jar app.jar 启动。
生产环境建议使用 Docker 容器化部署，配合 Nginx 反向代理。
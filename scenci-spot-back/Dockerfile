# 基于JDK17的官方镜像，完美匹配你的开发环境
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 把target目录下的jar包复制到容器里，重命名为app.jar
COPY target/*.jar app.jar

# 暴露你的后端端口（如果不是8080，改成你自己的端口）
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java","-jar","app.jar"]
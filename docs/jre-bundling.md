# JRE 内嵌说明

## 使用 jlink 精简 JRE

```bash
jlink --module-path "$JAVA_HOME/jmods" \
  --add-modules java.base,java.desktop,java.management,java.naming,java.net.http,java.sql,java.xml,jdk.unsupported \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output jre
```

## 模块说明

- java.base: 核心类库
- java.desktop: Swing/AWT (Spring Boot 可能需要)
- java.management: JMX 支持
- java.naming: JNDI 支持
- java.net.http: HTTP 客户端
- java.sql: JDBC 支持 (SQLite)
- java.xml: XML 处理
- jdk.unsupported: 内部 API (部分框架依赖)

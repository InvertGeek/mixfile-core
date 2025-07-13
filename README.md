## Core Module Of MixFile
包含MixFile服务端路由逻辑实现核心代码，上传线路核心代码 \
mix_dav mixlist等文件结构,上传下载逻辑,分享码解析逻辑 \
以及前端页面

## Dependency Usage

```xml
<repositories>
    <repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```

```xml
<dependency>
	<groupId>com.github.InvertGeek</groupId>
	<artifactId>mixfile-core</artifactId>
	<version>Tag</version>
</dependency>
```

https://jitpack.io/#InvertGeek/mixfile-core


## Code Examples

https://github.com/InvertGeek/mixfile-core/blob/main/src/test/kotlin/ServerTest.kt

https://github.com/InvertGeek/mixfilecli/blob/master/src/main/kotlin/com/donut/mixfilecli/Application.kt 

https://github.com/InvertGeek/MixMessage/blob/master/app/src/main/java/com/donut/mixmessage/util/mixfile/MixFileServer.kt

### Java(Api支持有限)
https://github.com/InvertGeek/mixfile-core/blob/main/src/test/java/ServerTestJava.java
plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

group = "org.etwxr9"
version = "1.0-SNAPSHOT"
val paperJarName = "paper-1.21.1-133.jar" 

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://ci.nyaacat.com/maven/")
    maven("https://maven.playpro.com/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("me.crafter.mc:lockettepro:2.15")
    implementation("net.coreprotect:coreprotect:22.4")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // 启用Hot-Swap支持
    options.compilerArgs.add("-parameters")
}




tasks.shadowJar {
    archiveBaseName.set("AutoOrganize")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}
tasks.register<JavaExec>("runServer") {
    group = "development"
    description = "Starts a local Paper test server with the plugin installed."

    // 1. 确保在运行服务器前，插件已经被构建
    dependsOn("build")

    // 2. 设置服务器的工作目录
    workingDir = file("run")

    // 3. 配置 JVM 启动参数
    jvmArgs(
        "-Xms2G", "-Xmx2G", // 根据你的需要调整内存
        // 这是开启 Debug 模式的关键！
        // transport=dt_socket: 使用 socket 通信
        // server=y: JVM作为服务端，等待调试器连接
        // suspend=n: 不暂停，立即启动服务器，不等调试器
        // address=*:5005: 在 5005 端口上监听所有网络接口
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    )

    // 4. 设置服务器的 classpath，就是 Paper 服务端本身
    classpath = files(file("run/$paperJarName"))
    
    // 5. 设置服务器启动参数
    args("--nogui")

    
    // 6. 在任务执行前，自动复制插件
    doFirst {
        // 从 build 任务的输出中找到我们的插件 JAR
        val pluginFile = tasks.named("jar", Jar::class.java).get().archiveFile.get().asFile
        val pluginsDir = file("run/plugins")

        // 确保 plugins 文件夹存在
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
        }
        
        // 复制并覆盖已存在的插件 JAR
        copy {
            from(pluginFile)
            into(pluginsDir)
            println("Copied ${pluginFile.name} to ${pluginsDir.path}")
        }
    }

        // 关键在于这一行！

    // 将 Gradle 的标准输入（你的键盘输入）重定向到 Paper 服务器进程
    standardInput = System.`in`
    
    // 这两行确保你能看到服务器的正常输出和错误信息
    standardOutput = System.out
}
# Java Toolchain Diagnostics

## Pre-Fix Status
**Date:** 2026-02-15

### 1. System `java -version`
```text
openjdk version "21.0.7" 2025-04-15 LTS
OpenJDK Runtime Environment Temurin-21.0.7+6 (build 21.0.7+6-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.7+6 (build 21.0.7+6-LTS, mixed mode, sharing)
```

### 2. System `mvn -v`
```text
Apache Maven 3.9.7
Maven home: D:\apache-maven3.9.7\apache-maven-3.9.7
Java version: 1.8.0_401, vendor: Oracle Corporation, runtime: D:\software\jdk8\jre
Default locale: zh_CN, platform encoding: GBK
OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"
```

### 3. Wrapper `mvnw -v`
*(To be populated after execution)*

## Issue
- System `java` is JDK 21.
- `mvn` is using JDK 8.
- Spring Boot 3 requires JDK 17+.
- **Resolution:** Enforce JDK 21 via Maven properties and `maven-toolchains-plugin`.

## Verification

Run the following in PowerShell:

```powershell
$env:JAVA_HOME="D:\software\jdk21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

mvn -v
mvn -q -DskipTests clean compile
```

**Expected Output:**
1. `mvn -v`: Java version should be 21.x
2. `mvn clean compile`: BUILD SUCCESS


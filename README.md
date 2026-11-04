# Annovibe ⚡

> Compile-time AI code generation for Java. Write the prompt, vibe the code.

Annovibe is a Java library and annotation processor that generates method bodies and class definitions **at compile time** using modern LLMs (OpenRouter, OpenAI, Anthropic).

---

## The 10-Second Vibe Check ☕

Why copy-paste the same 40-line StackOverflow snippet for the 50th time when you can just pass the vibe check at compile-time?

```java
import io.annovibe.AI;
import io.annovibe.vibe;
import java.time.LocalDate;

public class SanityUtils {

    @vibe("format raw bytes into a human-readable string like '1.4 MB' or '512 KB' using 1024 base")
    public static String formatBytes(long bytes) {
        return null; // Never Google this StackOverflow snippet again
    }

    @AI("calculate deadline: take startDate, add n business days skipping Saturdays and Sundays")
    public static LocalDate addBusinessDays(LocalDate startDate, int businessDays) {
        return null; // Date math without the existential dread
    }

    @vibe("validate credit card number using the Luhn algorithm checksum: return true if valid, false otherwise")
    public static boolean isValidCreditCard(String cardNumber) {
        return false; // Full checksum logic compiled straight into bytecode
    }
}
```

Hit `mvn compile` and `javac` synthesizes and compiles the algorithms directly into `.class` bytecode. No runtime AI calls, zero latency, just clean compiled Java.

---

## Features

- **`@AI` & `@vibe` Annotations**: Annotate any method or class with natural language instructions.
- **Direct AST Injection**: Seamlessly generates and compiles code into `.class` bytecode during `javac` compilation.
- **Out-of-the-Box LLM Providers**:
  - **OpenRouter** (`https://openrouter.ai/api/v1/chat/completions`)
  - **OpenAI** (`https://api.openai.com/v1/chat/completions`)
  - **Anthropic** (`https://api.anthropic.com/v1/messages`)
  - **Mock**: Deterministic local testing mode
- **Multi-Class Coherence**: Analyzes compilation units to share sibling class signatures and field definitions with the model, ensuring interdependent components fit together.
- **Smart Compile-Time Caching**: Disk-backed SHA-256 caching under `.annovibe/cache` ensures builds are instantaneous once generated and prevents redundant token usage.
- **Source Materialization**: Optional `-Aannovibe.materialize=true` mode writes the generated code directly into your `.java` files for version control.

---

## Quick Start

### 1. Maven Dependency

Add `annovibe-core` as a dependency and `annovibe-processor` to your `maven-compiler-plugin`:

```xml
<dependencies>
    <dependency>
        <groupId>io.annovibe</groupId>
        <artifactId>annovibe-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.annovibe</groupId>
                        <artifactId>annovibe-processor</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
                <fork>true</fork>
                <compilerArgs>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. Configure Your API Key

Set the environment variable for your preferred provider:
```bash
# OpenRouter (default)
export OPENROUTER_API_KEY="sk-or-v1-..."

# Or OpenAI
export OPENAI_API_KEY="sk-..."

# Or Anthropic
export ANTHROPIC_API_KEY="sk-ant-..."
```

Or configure via `annovibe.properties` in your project root:
```properties
annovibe.provider=openrouter
annovibe.model=anthropic/claude-3.5-sonnet
annovibe.openrouter.apiKey=sk-or-v1-...
annovibe.cache=true
```

---

## Code Examples

### Method Generation

```java
import io.annovibe.AI;
import io.annovibe.vibe;

public class MathUtils {

    @AI("calculate the factorial of n recursively; if n <= 1 return 1L")
    public static long factorial(int n) {
        return 0L; // Stub is replaced during javac compilation!
    }

    @vibe("return true if str is a palindrome ignoring whitespace and case")
    public static boolean isPalindrome(String str) {
        return false;
    }
}
```

### Multi-Component Coherence

```java
public class UserRepository {
    private final Map<String, User> store = new ConcurrentHashMap<>();

    @AI("save user into store keyed by id and return the saved user")
    public User save(User user) {
        return null;
    }

    @AI("find and return user from store matching id, or null if not found")
    public User findById(String id) {
        return null;
    }
}

public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @vibe("create a new User with id and email, save user into repo, and return it")
    public User registerUser(String id, String email) {
        return null;
    }
}
```

---

## Verification

Run all unit and integration tests:
```bash
mvn clean test
```

# Annovibe ⚡

> Compile-time AI code generation for Java. Write the prompt, vibe the code.

<p align="center">
  <img src="assets/demo.gif" alt="Annovibe Demo: From @vibe stub to compiled bytecode in seconds" width="100%"/>
</p>

Annovibe is a Java library and annotation processor that generates method bodies and class definitions **at compile time** using modern LLMs (OpenRouter, OpenAI, Anthropic, Ollama).

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

- **`@AI`, `@vibe`, & `@Vibe` Annotations**: Annotate any method or class with natural language instructions. Supports both lowercase `@vibe` and idiomatic PascalCase `@Vibe` (`import io.annovibe.annotations.Vibe`).
- **1-Shot Self-Healing Compiler Loop**: If an LLM outputs code with a syntax glitch or missing token, Annovibe captures the exact `javac` diagnostic and prompts the model to self-heal in real time—recovering transparently before the compilation phase completes! Bounded by `annovibe.selfHealing.maxRetries` (default: 1) to prevent runaway token usage.
- **Direct AST Injection**: Seamlessly generates and compiles code into `.class` bytecode during `javac` compilation.
- **Out-of-the-Box LLM Providers**:
  - **Local Ollama** (Zero-config! Auto-detected when running locally on `http://localhost:11434`)
  - **OpenRouter** (`https://openrouter.ai/api/v1/chat/completions`)
  - **OpenAI** (`https://api.openai.com/v1/chat/completions`)
  - **Anthropic** (`https://api.anthropic.com/v1/messages`)
  - **Mock**: Deterministic local testing mode
- **Multi-Class Coherence**: Analyzes compilation units to share sibling class signatures and field definitions with the model, ensuring interdependent components fit together.
- **Smart Compile-Time Caching**: Disk-backed SHA-256 caching under `.annovibe/cache` ensures builds are instantaneous once generated and prevents redundant token usage.
- **Source Materialization**: Optional `-Aannovibe.materialize=true` mode writes the generated code directly into your `.java` files for version control.

---

## 🛡️ The 1-Shot Self-Healing Compiler

LLMs are brilliant, but occasionally return a syntax oversight—a missing semicolon, an unmatched parenthesis, or an unexpected markdown artifact. In standard annotation processors, this immediately crashes your `mvn compile` build.

**Annovibe fixes this with compile-time self-healing:**

```mermaid
sequenceDiagram
    participant Javac as javac (AST Parser)
    participant Annovibe as Annovibe Processor
    participant LLM as LLM Provider (Ollama/Claude/GPT)

    Annovibe->>LLM: Generate method body
    LLM-->>Annovibe: Returns code
    Annovibe->>Javac: Speculative AST Parse
    alt Parse Success
        Annovibe->>Javac: Commit AST injection
    else Syntax / Parse Error Detected
        Note over Annovibe: Intercept diagnostic before Javac logs poison the build
        Annovibe->>LLM: Self-Heal Prompt (Code + javac Error Diagnostic)
        LLM-->>Annovibe: Returns repaired code
        Annovibe->>Javac: Re-parse & commit AST injection
    end
```

### 🛑 Bounded Recursion & Token Safety
To guarantee that persistent compiler errors never run away with your API budget or hang in infinite retry loops, self-healing is strictly bounded:
- `annovibe.selfHealing.maxRetries=1` (default: 1 retry).
- If the model cannot resolve the error within the configured retries, Annovibe fails fast, emitting the compiler diagnostic directly so you can adjust your prompt.

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

### 2. Provider API Keys & Defaults

Annovibe supports both local models and cloud providers:

#### Local Ollama (Zero Configuration)
If you run Ollama locally (`ollama run llama3.2` or `codellama`), Annovibe **auto-detects** `http://localhost:11434` when no cloud API keys are present. Zero keys, zero tokens, 100% private.

#### Cloud LLM Providers
Set the environment variable for your preferred provider:
```bash
# OpenRouter (default fallback)
export OPENROUTER_API_KEY="sk-or-v1-..."

# Or OpenAI
export OPENAI_API_KEY="sk-..."

# Or Anthropic
export ANTHROPIC_API_KEY="sk-ant-..."
```

---

## Configuration Guide

Annovibe is highly configurable. You can configure it via:
1. **Annotation Attributes**: per-method or per-class (`@AI(model = "...", cache = false)` or `@Vibe(...)`)
2. **Compiler Arguments**: via Maven/javac (`-Aannovibe.materialize=true`)
3. **`annovibe.properties`**: file in your project root
4. **System Properties**: passed to JVM (`-Dannovibe.cache=false`)
5. **Environment Variables**: (`OPENROUTER_API_KEY`, etc.)

### Precedence Order
> **Annotation > Compiler Option (-A) > annovibe.properties > System Property (-D) > Environment Variable**

---

### Compile-Time Caching

Calling an LLM API on every `mvn compile` or IDE auto-build would be slow, expensive, and subject to rate limits. Annovibe includes an automatic **disk-backed cryptographic cache**:

- **How it Works**: Annovibe computes a SHA-256 hash of `provider + model + prompt + method signature + sibling context`. The generated Java snippet is saved as `.annovibe/cache/<hash>.java`.
- **Instant Re-compilation**: If the prompt and method signature haven't changed, compilation takes **< 5ms** with zero API calls.
- **Offline / CI Friendly**: You can commit the `.annovibe/cache` directory to git if you want CI builds to run without network access or API tokens!

#### Caching Options

| Setting | Configuration | Description |
| :--- | :--- | :--- |
| **Disable globally** | `annovibe.cache=false` in `annovibe.properties` or `-Aannovibe.cache=false` | Forces live LLM generation on every compile |
| **Bypass per method** | `@AI(value = "...", cache = false)` or `@vibe(cache = false)` or `@Vibe(cache = false)` | Disables caching only for this element |
| **Custom directory** | `annovibe.cacheDir=/path/to/cache` | Changes cache location (defaults to `.annovibe/cache`) |
| **Clear cache** | `rm -rf .annovibe/cache` | Invalidates all cached generations |

---

### In-Memory AST Injection vs. Source Materialization

Annovibe offers two compilation workflows:

#### 1. In-Memory AST Injection (Default)
By default, Annovibe intercepts the compiler AST in-memory:
- Your `.java` source files stay completely clean, containing only your `@AI` / `@vibe` / `@Vibe` annotations and stubs.
- The compiled `.class` files contain the full, generated implementation bytecode.
- Perfect for keeping codebase size small and focusing on business logic.

#### 2. Source Materialization Mode (`-Aannovibe.materialize=true`)
Materialization rewrites the `.java` files on disk, replacing the stub body with the generated implementation:
- **Why use it**:
  - Review generated code directly in git diffs before committing.
  - Set breakpoints and step through the generated code in standard IDE debuggers.
  - Allow team members or CI environments without LLM API keys to build the project.
- **How to enable**:
  Add `-Aannovibe.materialize=true` to the `compilerArgs` in `pom.xml`:
  ```xml
  <compilerArgs>
      <arg>-Aannovibe.materialize=true</arg>
      <!-- Javac internal exports -->
      <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
      <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
      <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
      <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
      <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
      <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
  </compilerArgs>
  ```
  Or invoke on demand from the CLI:
  ```bash
  mvn compile -DcompilerArgs="-Aannovibe.materialize=true"
  ```

---

### Full `annovibe.properties` Reference

Create an `annovibe.properties` file in your project root to customize defaults:

```properties
# LLM Provider: ollama | openrouter | openai | anthropic | mock (default: auto-detected)
annovibe.provider=ollama

# Model name (default: llama3.2 for ollama, claude-3-5-sonnet for anthropic, gpt-4o for openai)
annovibe.model=llama3.2

# Local Ollama endpoint (default: http://localhost:11434)
annovibe.ollama.url=http://localhost:11434

# Custom Base URL (e.g. for custom vLLM / OpenAI-compatible endpoints)
# annovibe.baseUrl=http://localhost:8000/v1/chat/completions

# Provider API keys (alternative to environment variables)
annovibe.openrouter.apiKey=sk-or-v1-...
annovibe.openai.apiKey=sk-...
annovibe.anthropic.apiKey=sk-ant-...

# 1-Shot Self-Healing Compiler
annovibe.selfHealing=true
# Max self-healing retry iterations to protect token usage (default: 1)
annovibe.selfHealing.maxRetries=1

# Generation temperature (default: 0.2)
annovibe.temperature=0.2

# Caching: true | false (default: true)
annovibe.cache=true

# Cache storage directory (default: .annovibe/cache)
annovibe.cacheDir=.annovibe/cache

# Project-wide global prompt / architectural rules injected into all generations
annovibe.globalPrompt=Prefer immutability. Use java.time for dates. Use Optional for nullable returns.

# Materialize generated code to source files (default: false)
annovibe.materialize=false
```

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

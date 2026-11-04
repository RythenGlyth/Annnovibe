#!/usr/bin/env bash
set -e

# ==============================================================================
# Annovibe Demo GIF Generator
# Reproduces the terminal demo recording using Charmbracelet VHS.
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

# 1. Load .env if present
if [ -f "$REPO_ROOT/.env" ]; then
    echo "Loading environment variables from .env..."
    # Export non-comment lines
    set -a
    # shellcheck disable=SC1091
    source "$REPO_ROOT/.env"
    set +a
fi

# 2. Check Provider & Model Configuration
if [ -n "$OPENROUTER_API_KEY" ]; then
    echo "=========================================================="
    echo "✨ Using OpenRouter with model: ${ANNOVIBE_MODEL:-nvidia/nemotron-3.5-super-vl:free}"
    echo "=========================================================="
    export ANNOVIBE_PROVIDER="openrouter"
    export ANNOVIBE_MODEL="${ANNOVIBE_MODEL:-nvidia/nemotron-3.5-super-vl:free}"
elif grep -qE "^annovibe.openrouter.apiKey=sk-" "$REPO_ROOT/demo/annovibe.properties" 2>/dev/null; then
    echo "=========================================================="
    echo "✨ Using OpenRouter key from demo/annovibe.properties"
    echo "=========================================================="
    export ANNOVIBE_PROVIDER="openrouter"
    export ANNOVIBE_MODEL="${ANNOVIBE_MODEL:-nvidia/nemotron-3.5-super-vl:free}"
else
    echo "=========================================================="
    echo "ℹ️  No OPENROUTER_API_KEY detected."
    echo "   Using offline mock provider for deterministic recording."
    echo "   To use OpenRouter nvidia/nemotron-3.5-super-vl:free:"
    echo "     export OPENROUTER_API_KEY=\"sk-or-v1-...\""
    echo "     or put OPENROUTER_API_KEY=... in .env"
    echo "=========================================================="
    export ANNOVIBE_PROVIDER="mock"
fi

# 3. Ensure VHS is installed
if ! command -v vhs &>/dev/null; then
    echo "❌ VHS is not installed on this system."
    echo "   Install VHS: https://github.com/charmbracelet/vhs"
    echo "   e.g.: brew install vhs  or  go install github.com/charmbracelet/vhs@latest"
    exit 1
fi

# 4. Ensure local library artifacts are up to date
echo "📦 Verifying local Maven artifacts..."
mvn clean install -DskipTests -q

# 5. Prepare clean demo state
mkdir -p "$REPO_ROOT/assets"
mkdir -p "$REPO_ROOT/demo/src/main/resources" "$REPO_ROOT/demo/src/test/resources"

# Pre-compile once to warm the LLM cache so tape recording has deterministic, fast compile timing
echo "⚡ Pre-compiling demo with $ANNOVIBE_PROVIDER ($ANNOVIBE_MODEL) to warm cache..."
(cd "$REPO_ROOT/demo" && mvn compile -Dstyle.color=always -q)

# 6. Run VHS Recording
echo "🎬 Recording demo GIF with VHS (assets/demo.tape)..."
vhs "$REPO_ROOT/assets/demo.tape"

if [ -f "$REPO_ROOT/assets/demo.gif" ]; then
    GIF_SIZE=$(du -h "$REPO_ROOT/assets/demo.gif" | cut -f1)
    echo "✅ Successfully generated assets/demo.gif (Size: $GIF_SIZE)"
else
    echo "❌ GIF generation failed."
    exit 1
fi

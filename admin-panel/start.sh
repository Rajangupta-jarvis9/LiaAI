#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "======================================"
echo "  Lia AI — License Admin Panel"
echo "======================================"
echo ""

# Check Python3
if ! command -v python3 &>/dev/null; then
  echo "❌ python3 not found. Install it first:"
  echo "   pkg install python"
  exit 1
fi

# Check/install Flask
if ! python3 -c "import flask" &>/dev/null; then
  echo "📦 Installing Flask..."
  pip install flask --quiet
fi

echo "✅ Flask ready"
echo ""
echo "🌐 Open in browser: http://localhost:5000"
echo "   (Press Ctrl+C to stop)"
echo ""

python3 app.py

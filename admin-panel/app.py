"""
Lia AI — Local License Admin Panel
Run:  python3 app.py
Open: http://localhost:5000
"""

import sqlite3
import secrets
import string
import csv
import io
import time
from flask import Flask, request, jsonify, render_template, Response

app = Flask(__name__)
DB = "lia_keys.db"


# ── Database ──────────────────────────────────────────────────────────────────

def get_db():
    con = sqlite3.connect(DB)
    con.row_factory = sqlite3.Row
    return con


def init_db():
    with get_db() as con:
        con.execute("""
            CREATE TABLE IF NOT EXISTS keys (
                key        TEXT PRIMARY KEY,
                plan       TEXT    NOT NULL DEFAULT 'basic',
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL DEFAULT 0,
                active     INTEGER NOT NULL DEFAULT 1,
                blocked    INTEGER NOT NULL DEFAULT 0,
                device     TEXT             DEFAULT ''
            )
        """)
        con.commit()


def row_to_dict(row):
    d = dict(row)
    now = int(time.time() * 1000)
    if d["blocked"]:
        d["status"] = "blocked"
    elif d["expires_at"] > 0 and now > d["expires_at"]:
        d["status"] = "expired"
    elif d["active"]:
        d["status"] = "active"
    else:
        d["status"] = "inactive"
    return d


# ── Key generation ────────────────────────────────────────────────────────────

def generate_key(prefix="LIA"):
    chars = string.ascii_uppercase + string.digits
    segments = [prefix] + [
        "".join(secrets.choice(chars) for _ in range(4))
        for _ in range(4)
    ]
    return "-".join(segments)


# ── Routes ────────────────────────────────────────────────────────────────────

@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/keys")
def api_keys():
    with get_db() as con:
        rows = con.execute("SELECT * FROM keys ORDER BY created_at DESC").fetchall()
    return jsonify([row_to_dict(r) for r in rows])


@app.route("/api/stats")
def api_stats():
    now = int(time.time() * 1000)
    with get_db() as con:
        total   = con.execute("SELECT COUNT(*) FROM keys").fetchone()[0]
        blocked = con.execute("SELECT COUNT(*) FROM keys WHERE blocked=1").fetchone()[0]
        expired = con.execute(
            "SELECT COUNT(*) FROM keys WHERE blocked=0 AND expires_at>0 AND expires_at<?", (now,)
        ).fetchone()[0]
        active  = con.execute(
            "SELECT COUNT(*) FROM keys WHERE blocked=0 AND active=1 AND (expires_at=0 OR expires_at>?)", (now,)
        ).fetchone()[0]
    return jsonify({"total": total, "active": active, "blocked": blocked, "expired": expired})


@app.route("/activate", methods=["POST"])
def activate():
    """Android app calls this to register a key on first use."""
    data = request.get_json(silent=True) or {}
    key  = data.get("key", "").strip()
    device = data.get("device", "")

    if not key:
        return jsonify({"active": False, "error": "key missing"}), 400

    with get_db() as con:
        row = con.execute("SELECT * FROM keys WHERE key=?", (key,)).fetchone()
        if row is None:
            return jsonify({"active": False, "error": "key not found"}), 404

        row = row_to_dict(row)

        if row["status"] == "blocked":
            return jsonify({"active": False, "blocked": True, "error": "key blocked"}), 403
        if row["status"] == "expired":
            return jsonify({"active": False, "error": "key expired"}), 403

        # Store device binding on first activation
        if not row["device"] and device:
            con.execute("UPDATE keys SET device=? WHERE key=?", (device, key))
            con.commit()

        return jsonify({
            "active":     True,
            "plan":       row["plan"],
            "expires_at": row["expires_at"],
            "blocked":    False,
        })


@app.route("/check", methods=["POST"])
def check():
    """Periodic re-check called by the Android app every 3 hours."""
    data = request.get_json(silent=True) or {}
    key  = data.get("key", "").strip()

    if not key:
        return jsonify({"active": False, "error": "key missing"}), 400

    with get_db() as con:
        row = con.execute("SELECT * FROM keys WHERE key=?", (key,)).fetchone()
        if row is None:
            return jsonify({"active": False, "error": "key not found"}), 404
        row = row_to_dict(row)

    return jsonify({
        "active":     row["status"] == "active",
        "blocked":    row["blocked"],
        "plan":       row["plan"],
        "expires_at": row["expires_at"],
    })


@app.route("/admin/generate", methods=["POST"])
def admin_generate():
    data        = request.get_json(silent=True) or {}
    plan        = data.get("plan", "basic")
    expires_days = int(data.get("expires_days", 0))
    count       = min(int(data.get("count", 1)), 50)

    now = int(time.time() * 1000)
    expires_at = (now + expires_days * 86400 * 1000) if expires_days > 0 else 0

    generated = []
    with get_db() as con:
        for _ in range(count):
            key = generate_key()
            con.execute(
                "INSERT INTO keys (key, plan, created_at, expires_at, active, blocked) VALUES (?,?,?,?,1,0)",
                (key, plan, now, expires_at)
            )
            generated.append(key)
        con.commit()

    return jsonify({"ok": True, "keys": generated})


@app.route("/admin/block", methods=["POST"])
def admin_block():
    key = (request.get_json(silent=True) or {}).get("key", "").strip()
    if not key:
        return jsonify({"ok": False, "error": "key missing"}), 400
    with get_db() as con:
        con.execute("UPDATE keys SET blocked=1, active=0 WHERE key=?", (key,))
        con.commit()
    return jsonify({"ok": True})


@app.route("/admin/unblock", methods=["POST"])
def admin_unblock():
    key = (request.get_json(silent=True) or {}).get("key", "").strip()
    if not key:
        return jsonify({"ok": False, "error": "key missing"}), 400
    with get_db() as con:
        con.execute("UPDATE keys SET blocked=0, active=1 WHERE key=?", (key,))
        con.commit()
    return jsonify({"ok": True})


@app.route("/admin/delete", methods=["POST"])
def admin_delete():
    key = (request.get_json(silent=True) or {}).get("key", "").strip()
    if not key:
        return jsonify({"ok": False, "error": "key missing"}), 400
    with get_db() as con:
        con.execute("DELETE FROM keys WHERE key=?", (key,))
        con.commit()
    return jsonify({"ok": True})


@app.route("/admin/export")
def admin_export():
    with get_db() as con:
        rows = con.execute("SELECT * FROM keys ORDER BY created_at DESC").fetchall()

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["key", "plan", "status", "created_at", "expires_at", "device"])
    for r in rows:
        d = row_to_dict(r)
        writer.writerow([d["key"], d["plan"], d["status"],
                         d["created_at"], d["expires_at"], d.get("device", "")])

    return Response(
        output.getvalue(),
        mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=lia_keys.csv"}
    )


# ── Entry point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    init_db()
    print("\n" + "="*50)
    print("  Lia AI Admin Panel")
    print("  Open: http://localhost:5000")
    print("="*50 + "\n")
    app.run(host="0.0.0.0", port=5000, debug=False)

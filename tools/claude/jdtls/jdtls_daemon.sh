#!/bin/bash
# Daemon management for jdtls server

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_SCRIPT="$SCRIPT_DIR/jdtls_server.py"

# Private per-user directory: $XDG_RUNTIME_DIR, else $XDG_CACHE_HOME or ~/.cache (mode 0700).
# An existing directory is refused unless it is ours and not writable by others.
umask 077
if [ -n "${XDG_RUNTIME_DIR:-}" ] && [ "${XDG_RUNTIME_DIR#/}" != "$XDG_RUNTIME_DIR" ]; then
    STATE_ROOT="$XDG_RUNTIME_DIR"
elif [ -n "${XDG_CACHE_HOME:-}" ] && [ "${XDG_CACHE_HOME#/}" != "$XDG_CACHE_HOME" ]; then
    STATE_ROOT="$XDG_CACHE_HOME"
else
    STATE_ROOT="$HOME/.cache"
fi
STATE_DIR="$STATE_ROOT/archidata-jdtls"
if [ ! -e "$STATE_DIR" ] && [ ! -L "$STATE_DIR" ]; then
    mkdir -p -m 700 "$STATE_DIR" || exit 1
fi
if [ -L "$STATE_DIR" ] || [ ! -d "$STATE_DIR" ] || [ ! -O "$STATE_DIR" ] \
    || [ -n "$(find "$STATE_DIR" -maxdepth 0 -perm /022)" ]; then
    echo "Refusing $STATE_DIR: it must be a real directory owned by you and not writable by others" >&2
    exit 1
fi
PID_FILE="$STATE_DIR/jdtls_daemon.pid"
LOG_FILE="$STATE_DIR/jdtls_daemon.log"

# True when PID is alive and its command line is our server script.
is_our_daemon() {
    local cmdline
    [[ "$1" =~ ^[0-9]+$ ]] || return 1
    [ -r "/proc/$1/cmdline" ] || return 1
    cmdline=$(tr '\0' ' ' < "/proc/$1/cmdline" 2>/dev/null) || return 1
    [[ "$cmdline" == *"$SERVER_SCRIPT"* ]]
}

# Read the PID file only if it is a regular file we own (not a symlink).
read_pid() {
    if [ -L "$PID_FILE" ] || [ ! -O "$PID_FILE" ]; then
        return 1
    fi
    cat "$PID_FILE"
}

start_daemon() {
    if [ -e "$PID_FILE" ]; then
        PID=$(read_pid) || PID=""
        if is_our_daemon "$PID"; then
            echo "jdtls daemon is already running (PID: $PID)"
            return 0
        else
            echo "Removing stale PID file"
            rm -f "$PID_FILE"
        fi
    fi

    echo "Starting jdtls daemon..."
    rm -f "$LOG_FILE"
    nohup python3 "$SERVER_SCRIPT" start > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "jdtls daemon started (PID: $(cat "$PID_FILE"))"
    echo "Logs: $LOG_FILE"

    # Wait a bit to see if it started successfully
    sleep 3
    if is_our_daemon "$(cat "$PID_FILE")"; then
        echo "Daemon is running"
    else
        echo "Failed to start daemon. Check logs: $LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
}

stop_daemon() {
    if [ ! -e "$PID_FILE" ]; then
        echo "jdtls daemon is not running (no PID file)"
        return 0
    fi

    PID=$(read_pid) || PID=""
    if ! is_our_daemon "$PID"; then
        echo "jdtls daemon is not running (stale or foreign PID file, nothing killed)"
        rm -f "$PID_FILE"
        return 0
    fi

    echo "Stopping jdtls daemon (PID: $PID)..."
    kill "$PID"

    # Wait for process to stop
    for i in {1..10}; do
        if ! is_our_daemon "$PID"; then
            echo "Daemon stopped"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done

    # Force kill if still running
    echo "Force killing daemon..."
    if is_our_daemon "$PID"; then
        kill -9 "$PID" 2>/dev/null
    fi
    rm -f "$PID_FILE"
    echo "Daemon stopped (forced)"
}

status_daemon() {
    if [ ! -e "$PID_FILE" ]; then
        echo "jdtls daemon is NOT running"
        return 1
    fi

    PID=$(read_pid) || PID=""
    if is_our_daemon "$PID"; then
        echo "jdtls daemon is RUNNING (PID: $PID)"
        echo "Logs: $LOG_FILE"

        # Show project status
        echo ""
        python3 "$SERVER_SCRIPT" status 2>/dev/null || true
        return 0
    else
        echo "jdtls daemon is NOT running (stale PID file)"
        rm -f "$PID_FILE"
        return 1
    fi
}

restart_daemon() {
    stop_daemon
    sleep 2
    start_daemon
}

tail_logs() {
    if [ ! -f "$LOG_FILE" ]; then
        echo "No log file found at $LOG_FILE"
        return 1
    fi
    tail -f "$LOG_FILE"
}

case "$1" in
    start)
        start_daemon
        ;;
    stop)
        stop_daemon
        ;;
    restart)
        restart_daemon
        ;;
    status)
        status_daemon
        ;;
    logs)
        tail_logs
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs}"
        echo ""
        echo "Commands:"
        echo "  start    - Start the jdtls daemon"
        echo "  stop     - Stop the jdtls daemon"
        echo "  restart  - Restart the jdtls daemon"
        echo "  status   - Show daemon and project status"
        echo "  logs     - Tail the daemon logs"
        exit 1
        ;;
esac

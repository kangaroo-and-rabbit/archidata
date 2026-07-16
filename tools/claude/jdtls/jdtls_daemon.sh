#!/bin/bash
# Daemon management for jdtls server

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_SCRIPT="$SCRIPT_DIR/jdtls_server.py"
PID_FILE="/tmp/jdtls_daemon.pid"
LOG_FILE="/tmp/jdtls_daemon.log"

start_daemon() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "jdtls daemon is already running (PID: $PID)"
            return 0
        else
            echo "Removing stale PID file"
            rm -f "$PID_FILE"
        fi
    fi

    echo "Starting jdtls daemon..."
    nohup python3 "$SERVER_SCRIPT" start > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "jdtls daemon started (PID: $(cat $PID_FILE))"
    echo "Logs: $LOG_FILE"

    # Wait a bit to see if it started successfully
    sleep 3
    if ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "Daemon is running"
    else
        echo "Failed to start daemon. Check logs: $LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
}

stop_daemon() {
    if [ ! -f "$PID_FILE" ]; then
        echo "jdtls daemon is not running (no PID file)"
        return 0
    fi

    PID=$(cat "$PID_FILE")
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "jdtls daemon is not running (stale PID file)"
        rm -f "$PID_FILE"
        return 0
    fi

    echo "Stopping jdtls daemon (PID: $PID)..."
    kill "$PID"

    # Wait for process to stop
    for i in {1..10}; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo "Daemon stopped"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done

    # Force kill if still running
    echo "Force killing daemon..."
    kill -9 "$PID" 2>/dev/null
    rm -f "$PID_FILE"
    echo "Daemon stopped (forced)"
}

status_daemon() {
    if [ ! -f "$PID_FILE" ]; then
        echo "jdtls daemon is NOT running"
        return 1
    fi

    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
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

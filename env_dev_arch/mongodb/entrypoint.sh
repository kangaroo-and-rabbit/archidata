#!/bin/bash
set -e

# Ensure keyfile exists with proper permissions
if [ ! -f /data/keys/keyfile ]; then
    echo "[init] Generating keyfile..."
    openssl rand -base64 756 > /data/keys/keyfile
fi
chmod 400 /data/keys/keyfile

# Background initialization using localhost exception (no auth needed for first user)
(
    echo "[rs-init] Waiting for mongod to be ready..."
    until mongosh --quiet --eval "db.runCommand({ping:1})" &>/dev/null; do
        sleep 1
    done

    echo "[rs-init] Initializing replica set..."
    mongosh --quiet --eval "
    try { rs.status() }
    catch(e) { rs.initiate({_id:'rs0', members:[{_id:0, host:'mongodb:27017'}]}) }
    "

    echo "[rs-init] Waiting for PRIMARY election..."
    until mongosh --quiet --eval "rs.isMaster().ismaster" 2>/dev/null | grep -q true; do
        sleep 1
    done

    echo "[rs-init] Creating root user..."
    mongosh --quiet --eval "
    try {
        db.getSiblingDB('admin').createUser({
            user: '${MONGO_INITDB_ROOT_USERNAME}',
            pwd: '${MONGO_INITDB_ROOT_PASSWORD}',
            roles: ['root']
        });
        print('[rs-init] Root user created');
    } catch(e) {
        print('[rs-init] User already exists: ' + e.message);
    }
    "
    echo "[rs-init] Initialization complete!"
) &

# Start mongod directly - single start, no restart cycle
exec mongod \
    --replSet rs0 \
    --bind_ip_all \
    --keyFile /data/keys/keyfile \
    --dbpath /data/db \
    --maxConns 800

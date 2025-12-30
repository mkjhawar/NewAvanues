#!/bin/bash

# Start IDEACODE API in background if not already running
if ! curl -s http://localhost:3847/health > /dev/null 2>&1; then
    echo "🚀 Starting IDEACODE API..."
    nohup bash -c 'cd /Volumes/M-Drive/Coding/ideacode/ideacode-api && npm start' > /tmp/ideacode-api.log 2>&1 &

    # Wait up to 10 seconds for API to be ready
    for i in {1..10}; do
        if curl -s http://localhost:3847/health > /dev/null 2>&1; then
            echo "✓ IDEACODE API started successfully"
            exit 0
        fi
        sleep 1
    done

    echo "⚠️  Warning: IDEACODE API startup may have timed out (check /tmp/ideacode-api.log)"
else
    echo "✓ IDEACODE API already running"
fi

exit 0

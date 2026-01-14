#!/bin/bash
# Updates the ccusage cache for statusline display
# Run this periodically or add to cron: */10 * * * * ~/.claude/update-usage-cache.sh

usage_cache="$HOME/.claude/.usage_cache"

if command -v npx &> /dev/null; then
    # Get today's cost from ccusage
    today=$(date +%Y-%m-%d)
    ccdata=$(npx -y ccusage daily --json 2>/dev/null)

    if [[ -n "$ccdata" ]]; then
        # Get today's cost (last entry in daily array is usually today)
        d_cost=$(echo "$ccdata" | jq -r ".daily[-1].totalCost // 0" 2>/dev/null)

        if [[ -n "$d_cost" ]] && [[ "$d_cost" != "null" ]] && [[ "$d_cost" != "0" ]]; then
            # Calculate % based on $10/day budget (adjust DAILY_BUDGET as needed)
            DAILY_BUDGET=10
            d_pct=$(echo "scale=0; $d_cost * 100 / $DAILY_BUDGET" | bc 2>/dev/null || echo "0")

            # Also get weekly total (sum last 7 days)
            w_cost=$(echo "$ccdata" | jq -r '[.daily[-7:][].totalCost] | add // 0' 2>/dev/null)
            WEEKLY_BUDGET=50
            w_pct=$(echo "scale=0; $w_cost * 100 / $WEEKLY_BUDGET" | bc 2>/dev/null || echo "0")

            # Write cache
            echo "daily_pct=$d_pct" > "$usage_cache"
            echo "weekly_pct=$w_pct" >> "$usage_cache"
            echo "usage_source=cc" >> "$usage_cache"
            echo "Updated: D:${d_pct}% W:${w_pct}% (cost: \$${d_cost}/day, \$${w_cost}/week)"
        else
            echo "No cost data for today"
        fi
    else
        echo "Failed to get ccusage data"
    fi
else
    echo "npx not found - install Node.js"
fi

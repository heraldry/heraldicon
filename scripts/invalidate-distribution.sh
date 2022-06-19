#!/usr/bin/env bash
set -euo pipefail

distribution_id="$(aws --profile heraldry cloudfront list-distributions | jq -r '.DistributionList.Items[] | select([.Aliases.Items[] == "'"$1"'"] | any) | .Id')"
aws --profile heraldry cloudfront create-invalidation --distribution-id "$distribution_id" --paths '/*' | cat

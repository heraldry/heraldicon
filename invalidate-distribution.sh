#!/usr/bin/env bash
set -euo pipefail

aws --profile heraldry cloudfront create-invalidation --distribution-id $(aws --profile heraldry cloudfront list-distributions | jq -r '.DistributionList.Items[] | select([.Aliases.Items[] == "heraldry.digital"] | any) | .Id') --paths '/*' | cat

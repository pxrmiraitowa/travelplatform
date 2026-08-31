#!/usr/bin/env bash
set -Eeuo pipefail

NAMESPACE="${K8S_NAMESPACE:-travel-platform}"

command -v kubectl >/dev/null 2>&1 || {
  echo "kubectl is required." >&2
  exit 1
}

declare -A expected_tables=(
  [travel_user]="role,user,user_contact,user_role"
  [travel_product]="attraction,coupon,flight,hotel,hotel_room,tour_package,train_ticket"
  [travel_order]="orders"
  [travel_content_trip]="price_alert,review,share_image,share_post,trip_plan,trip_plan_item"
)

for database in travel_user travel_product travel_order travel_content_trip; do
  actual="$(kubectl --namespace "${NAMESPACE}" exec statefulset/mysql -- sh -c \
    "mysql -h127.0.0.1 -P3306 -N -B -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e \"SELECT COALESCE(GROUP_CONCAT(table_name ORDER BY table_name SEPARATOR ','), '') FROM information_schema.tables WHERE table_schema='${database}'\"")"

  if [[ "${actual}" != "${expected_tables[${database}]}" ]]; then
    echo "Table ownership violation in ${database}." >&2
    echo "Expected: ${expected_tables[${database}]}" >&2
    echo "Actual:   ${actual}" >&2
    exit 1
  fi
  echo "${database}: ${actual}"
done

echo "Database table ownership checks passed."

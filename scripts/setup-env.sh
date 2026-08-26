#!/usr/bin/env bash
#
# Tworzy lokalny plik .env na podstawie .env.example.
# Hasła generuje losowo — nic wrażliwego nie pochodzi z repozytorium.
#
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
    echo "Plik .env już istnieje — nie ruszam go."
    echo "Aby wygenerować od nowa:  rm .env && $0"
    exit 0
fi

if ! command -v openssl >/dev/null 2>&1; then
    echo "Brak polecenia openssl. Zainstaluj je albo uzupełnij .env ręcznie." >&2
    exit 1
fi

cp .env.example .env

# sed -i.bak działa tak samo na macOS (BSD) i Linuksie (GNU).
# Separator | zamiast / — Base64 zawiera / i + , ale nigdy |
set_var() {
    sed -i.bak "s|^$1=.*|$1=$2|" .env && rm -f .env.bak
}

ADMIN_PASS="$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | cut -c1-16)"

set_var JWT_SECRET       "$(openssl rand -base64 64 | tr -d '\n')"
set_var DB_PASSWORD      "tp_password"
set_var ADMIN_PASSWORD   "$ADMIN_PASS"
set_var PGADMIN_PASSWORD "$(openssl rand -base64 12 | tr -dc 'A-Za-z0-9' | cut -c1-12)"

cat <<INFO

Utworzono .env

  JWT_SECRET        wygenerowany, 64 bajty losowe
  DB_PASSWORD       tp_password  (zgodne z istniejącym wolumenem Dockera)
  ADMIN_PASSWORD    $ADMIN_PASS
  PGADMIN_PASSWORD  wygenerowane, zajrzyj do .env

Zapisz hasło administratora — powstanie na nim konto przy pierwszym starcie.

Następny krok:
  docker compose up -d --build

INFO
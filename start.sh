#!/usr/bin/env bash

# Переходим в директорию скрипта (корень проекта lab4)
cd "$(dirname "$0")"

# Отключаем строгую проверку прав файловой системы Buildx для путей с кириллицей
export BUILDX_BAKE_ENTITLEMENTS_FS=0

echo "   Запуск микросервисов СУС WFM (ЛР №4, Kafka KRaft Cluster + Clean Architecture)..."

# Проверка Docker
if ! docker info >/dev/null 2>&1; then
    echo "ВНИМАНИЕ: Docker не запущен! Пожалуйста, запустите Docker Desktop и повторите попытку."
    exit 1
fi

# Запуск Docker Compose (PostgreSQL, Kafka KRaft x3, Eureka, Config, Gateway, 5 микросервисов)
echo "[1/2] Запуск контейнеров Docker (PostgreSQL 16 + Kafka x3 + Eureka + Gateway + 5 микросервисов)..."
docker compose up -d "$@"

# Запуск Frontend-сервера (Порты 3000 и 3001)
echo "[2/2] Запуск фронтенд-серверов..."
if [ -f .frontend.pid ] && kill -0 "$(cat .frontend.pid)" 2>/dev/null; then
    echo "Фронтенд-сервер уже запущен (PID: $(cat .frontend.pid))."
else
    node frontend/server.js > .frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "$FRONTEND_PID" > .frontend.pid
    echo "Фронтенд успешно запущен в фоне (PID: $FRONTEND_PID)."
fi

echo ""
echo "========================================================"
echo "ВСЕ СЕРВИСЫ ЛР №4 УСПЕШНО ЗАПУЩЕНЫ!"
echo "========================================================"
echo "Панель Администратора (Admin / HR / Manager): http://localhost:3000"
echo "Кабинет Сотрудника (Employee Self-Service):  http://localhost:3001"
echo "API Gateway (REST API & JWT Auth):          http://localhost:8080/api"
echo "Swagger UI Документация API:                http://localhost:8080/swagger-ui.html"
echo "Eureka Discovery Dashboard:                 http://localhost:8761"
echo "Config Server:                              http://localhost:8888"
echo "Notification Service (Clean Arch + Kafka):  http://localhost:8084"
echo "File Service (Clean Arch + Storage):        http://localhost:8085"
echo "Kafka KRaft Cluster (3 брокера):            localhost:9092, localhost:9094, localhost:9095"
echo "PostgreSQL База данных:                     localhost:5432 (portal_db)"
echo "--------------------------------------------------------"
echo "ТЕСТОВЫЕ АККАУНТЫ (пароль для ВСЕХ: 1234):"
echo "   - admin            (Иван Иванов / ADMIN)"
echo "   - zlobnaya         (Злобная Клавдия / HR)"
echo "   - managerov        (Менеджеров Менеджер / MANAGER)"
echo "   - pekarev          (Пекарёв Пекарь / EMPLOYEE #2)"
echo "   - vypechkina       (Выпечкина Слойка / EMPLOYEE #3)"
echo "--------------------------------------------------------"
echo "Для остановки сервисов выполните: ./stop.sh"

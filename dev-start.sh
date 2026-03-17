#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Starting databases..."
docker compose up -d postgres-user postgres-shelf postgres-review

echo "Waiting for databases to be healthy..."
sleep 10

echo "Starting services..."
tmux new-session  -d -s bookshelf -n "user-service"   "cd '$SCRIPT_DIR/user-service'   && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-DDB_PORT=5433; exec bash"
tmux new-window      -t bookshelf -n "shelf-service"  "cd '$SCRIPT_DIR/shelf-service'  && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-DDB_PORT=5434; exec bash"
tmux new-window      -t bookshelf -n "review-service" "cd '$SCRIPT_DIR/review-service' && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-DDB_PORT=5435; exec bash"
tmux new-window      -t bookshelf -n "react-frontend" "cd '$SCRIPT_DIR/react-frontend' && npm install && npm run dev; exec bash"

tmux attach -t bookshelf

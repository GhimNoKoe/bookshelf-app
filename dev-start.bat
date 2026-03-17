@echo off
echo Starting databases...
docker compose up -d postgres-user postgres-shelf postgres-review

echo Waiting for databases to be healthy...
timeout /t 10 /nobreak > nul

echo Starting services...
start "user-service"   cmd /k "cd /d %~dp0user-service   && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-DDB_PORT=5433"
start "shelf-service"  cmd /k "cd /d %~dp0shelf-service  && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-DDB_PORT=5434"
start "review-service" cmd /k "cd /d %~dp0review-service && mvn spring-boot:run -Dspring-boot.run.jvmArguments=-DDB_PORT=5435"
start "react-frontend" cmd /k "cd /d %~dp0react-frontend && npm install && npm run dev"

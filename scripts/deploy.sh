#!/bin/bash

REPOSITORY=/home/ec2-user/app/hello
PROJECT_NAME=hello

echo "> Build 파일 복사"

# zip 파일의 jar 파일의 경로를 이동시켜 줍니다.
# IF MAVEN THEN
cp $REPOSITORY/zip/target/*.jar $REPOSITORY/
# ELSE IF
cp $REPOSITORY/zip/build/libs/*.jar $REPOSITORY/
# FI

echo "> 현재 구동 중인 애플리케이션 pid 확인"

CURRENT_PID=$(pgrep -fl $PROJECT_NAME | awk '{print $1}')

echo "현재 구동 중인 애플리케이션 pid: $CURRENT_PID"

if [ -z "$CURRENT_PID" ]; then
  echo "> 현재 구동 중인 애플리케이션이 없으므로 종료하지 않습니다."
else
  echo "> kill -15 $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
fi

echo "> 새 애플리케이션 배포"

# 자... jar 파일 실행해 봅시다...
JAR_NAME=$(ls -tr $REPOSITORY/*.jar | tail -n 1)

echo "> JAR Name: $JAR_NAME"

echo "> $JAR_NAME 에 실행 권한 추가"

chmod +x $JAR_NAME

echo "> $JAR_NAME 실행"

# 이 부분이 핵심인데, 필요한 설정 값들을 포함해서 jar 파일을 실행시켜주면 됩니다.
nohup java -jar -Dspring.profiles.active=real $JAR_NAME > $REPOSITORY/nohup.out 2>&1 &
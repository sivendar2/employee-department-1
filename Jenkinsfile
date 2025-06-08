version: 0.2

env:
  variables:
    AWS_REGION: us-east-1
    ECR_REPO: 779846797240.dkr.ecr.us-east-1.amazonaws.com/employee-department1
    IMAGE_TAG: latest
    EXECUTION_ROLE_ARN: arn:aws:iam::779846797240:role/ecsTaskExecutionRole
    LOG_GROUP: /ecs/employee-department1
    SONAR_PROJECT_KEY: employee-department-1
    SONAR_HOST_URL: http://sonarqube.sivendar.click:9000/
    CLUSTER_NAME: employee-cluster1
    SERVICE_NAME: employee-service
    SUBNET_ID: subnet-0c06c9ba80675ca5b
    SECURITY_GROUP_ID: sg-03992897fd20860bd

  secrets-manager:
    SONAR_TOKEN: sonar_token  # Replace with your actual Secrets Manager secret name

phases:
  install:
    runtime-versions:
      java: corretto17
      docker: 20
    commands:
      - echo Installing AWS CLI v2 if needed...
      - pip install --upgrade awscli

  pre_build:
    commands:
      - echo Logging in to Amazon ECR...
      - aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO

      - echo Running SonarQube Analysis...
      - mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
          -Dsonar.projectKey=$SONAR_PROJECT_KEY \
          -Dsonar.host.url=$SONAR_HOST_URL \
          -Dsonar.login=$SONAR_TOKEN || echo "Sonar analysis skipped or failed (non-blocking)"

  build:
    commands:
      - echo Building application...
      - mvn clean package -DskipTests

      - echo Building Docker image...
      - docker build -t employee-department1 -f Docker/Dockerfile .
      - docker tag employee-department1:latest $ECR_REPO:$IMAGE_TAG
      - docker push $ECR_REPO:$IMAGE_TAG

  post_build:
    commands:
      - echo Ensuring CloudWatch Log Group exists...
      - aws logs create-log-group --log-group-name "$LOG_GROUP" --region $AWS_REGION || echo "Log group already exists"

      - echo Registering ECS Task Definition...
      - |
        cat > taskdef.json <<EOF
        {
          "family": "employee-taskdef",
          "networkMode": "awsvpc",
          "requiresCompatibilities": ["FARGATE"],
          "cpu": "512",
          "memory": "1024",
          "executionRoleArn": "${EXECUTION_ROLE_ARN}",
          "containerDefinitions": [
            {
              "name": "employee-department1",
              "image": "${ECR_REPO}:${IMAGE_TAG}",
              "portMappings": [
                {
                  "containerPort": 8080,
                  "protocol": "tcp"
                }
              ],
              "essential": true,
              "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                  "awslogs-group": "${LOG_GROUP}",
                  "awslogs-region": "${AWS_REGION}",
                  "awslogs-stream-prefix": "ecs"
                }
              }
            }
          ]
        }
        EOF
      - aws ecs register-task-definition --cli-input-json file://taskdef.json --region $AWS_REGION

      - echo Checking if ECS service exists...
      - |
        SERVICE_STATUS=$(aws ecs describe-services \
          --cluster $CLUSTER_NAME \
          --services $SERVICE_NAME \
          --query "services[0].status" \
          --output text \
          --region $AWS_REGION || echo "None")

        echo "Service status: $SERVICE_STATUS"

        if [[ "$SERVICE_STATUS" == "ACTIVE" ]]; then
          echo "Updating ECS service..."
          aws ecs update-service \
            --cluster $CLUSTER_NAME \
            --service $SERVICE_NAME \
            --force-new-deployment \
            --region $AWS_REGION
        else
          echo "Creating ECS service..."
          aws ecs create-service \
            --cluster $CLUSTER_NAME \
            --service-name $SERVICE_NAME \
            --task-definition employee-taskdef \
            --desired-count 1 \
            --launch-type FARGATE \
            --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_ID],securityGroups=[$SECURITY_GROUP_ID],assignPublicIp=ENABLED}" \
            --region $AWS_REGION
        fi

artifacts:
  files:
    - target/*.jar

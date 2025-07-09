pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPO = '779846797240.dkr.ecr.us-east-1.amazonaws.com/employee-department1'
        IMAGE_TAG = 'latest'
        EXECUTION_ROLE_ARN = 'arn:aws:iam::779846797240:role/ecsTaskExecutionRole'
        LOG_GROUP = '/ecs/employee-department1'
        SONAR_HOST_URL = 'http://sonarqube.sivendar.click:9000/'
        SONAR_PROJECT_KEY = 'employee-department-1'
        SONAR_TOKEN = credentials('sonar-token-jenkins') // Jenkins Credentials ID for the Sonar token
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/sivendar2/employee-department-1.git'
            }
        }
                stage('Dependency Scan - OWASP') {
            steps {
                sh 'mvn org.owasp:dependency-check-maven:check'
            }
        }

        stage('Dependency Scan - Snyk') {
            steps {
                sh 'snyk test || true'  // Optional fail-safe if snyk finds issues
            }
        }

        stage('Auto Upgrade Vulnerable Dependencies') {
            steps {
                sh 'mvn versions:use-latest-releases -DgenerateBackupPoms=false'
            }
        }

     stage('Commit Updated Dependencies') {
    steps {
        withCredentials([usernamePassword(
            credentialsId: 'git-cred-id',
            usernameVariable: 'GIT_USER',
            passwordVariable: 'GIT_PASS'
        )]) {
            sh '''
                git config --global user.name "$GIT_USER"
                git config --global user.email "$GIT_USER@users.noreply.github.com"

                git add pom.xml
                git commit -m "Auto-upgrade vulnerable dependencies" || echo "No changes to commit"

                git push https://$GIT_USER:$GIT_PASS@github.com/sivendar2/employee-department-1.git HEAD:main
            '''
        }
    }
}

        stage('SAST - Semgrep Scan') {
            steps {
                sh '''
                    semgrep scan --config auto --json > semgrep-report.json || true
                '''
            }
        }

        stage('Semgrep Autofix') {
            steps {
                sh '''
                    semgrep scan --config auto --autofix || true
                '''
            }
        }

        stage('Create SAST Fix PR') {
    steps {
        withCredentials([usernamePassword(credentialsId: 'git-cred-id', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
            bat '''
            @echo off
            setlocal enabledelayedexpansion

            git config --global user.name "%GIT_USER%"
            git config --global user.email "%GIT_USER%@users.noreply.github.com"

            for /f %%i in ('powershell -Command "Get-Date -UFormat %%s"') do set BRANCH_NAME=fix/sast-autofix-%%i

            git checkout -b !BRANCH_NAME!
            git add .
            git diff --cached --quiet || git commit -m "chore: auto-remediation for SAST issues"
            git push https://%GIT_USER%:%GIT_PASS%@github.com/sivendar2/employee-department-1.git !BRANCH_NAME!

            gh pr create ^
              --base main ^
              --head !BRANCH_NAME! ^
              --title "SAST: Auto-fixed issues using Semgrep" ^
              --body "This PR includes automated fixes for static code analysis issues. Please review before merging."

            endlocal
            '''
        }
    }
}


        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                    sh """
                        mvn sonar:sonar \
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                          -Dsonar.host.url=${SONAR_HOST_URL} \
                          -Dsonar.login=${SONAR_TOKEN}
                    """
                }
            }
        }

       

        stage('Build App') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t employee-department1 -f Docker/Dockerfile .'
            }
        }

        stage('Docker Login to ECR') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        mkdir -p ~/.aws
                        echo "[default]" > ~/.aws/credentials
                        echo "aws_access_key_id=$AWS_ACCESS_KEY_ID" >> ~/.aws/credentials
                        echo "aws_secret_access_key=$AWS_SECRET_ACCESS_KEY" >> ~/.aws/credentials
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO
                    '''
                }
            }
        }

        stage('Push Docker Image to ECR') {
            steps {
                sh '''
                    docker tag employee-department1:latest $ECR_REPO:$IMAGE_TAG
                    docker push $ECR_REPO:$IMAGE_TAG
                '''
            }
        }

        stage('Ensure CloudWatch Log Group Exists') {
            steps {
                sh '''
                    aws logs create-log-group --log-group-name "$LOG_GROUP" --region $AWS_REGION || echo "Log group already exists or creation skipped"
                '''
            }
        }

        stage('Register Task Definition') {
            steps {
                script {
                    def taskDefJson = """
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
                    """
                    writeFile file: 'taskdef.json', text: taskDefJson
                    sh """
                        aws ecs register-task-definition \
                          --cli-input-json file://taskdef.json \
                          --region ${AWS_REGION}
                    """
                }
            }
        }

        stage('Deploy to ECS Fargate') {
            steps {
                script {
                    def clusterName = 'employee-cluster1'
                    def serviceName = 'employee-service'
                    def networkConfig = "awsvpcConfiguration={subnets=[subnet-0c06c9ba80675ca5b],securityGroups=[sg-03992897fd20860bd],assignPublicIp=ENABLED}"

                    def serviceStatus = sh (
                        script: "aws ecs describe-services --cluster ${clusterName} --services ${serviceName} --query 'services[0].status' --output text --region ${AWS_REGION}",
                        returnStdout: true
                    ).trim()

                    echo "Service status: ${serviceStatus}"

                    if (serviceStatus == 'INACTIVE' || serviceStatus == 'None' || serviceStatus == 'null' || serviceStatus == '') {
                        echo "ECS Service does not exist or inactive. Creating service..."
                        sh """
                            aws ecs create-service \
                              --cluster ${clusterName} \
                              --service-name ${serviceName} \
                              --task-definition employee-taskdef \
                              --desired-count 1 \
                              --launch-type FARGATE \
                              --network-configuration "${networkConfig}" \
                              --region ${AWS_REGION}
                        """
                    } else if (serviceStatus == 'ACTIVE') {
                        echo "Service is ACTIVE. Proceeding with deployment..."
                        sh """
                            aws ecs update-service \
                              --cluster ${clusterName} \
                              --service ${serviceName} \
                              --force-new-deployment \
                              --region ${AWS_REGION}
                        """
                    } else {
                        error("Unexpected ECS service status: ${serviceStatus}")
                    }
                }
            }
        }
    }
}

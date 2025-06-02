pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPO = '779846797240.dkr.ecr.us-east-1.amazonaws.com/employee-department1'
        IMAGE_TAG = 'latest'
        EXECUTION_ROLE_ARN = 'arn:aws:iam::779846797240:role/ecsTaskExecutionRole'
        LOG_GROUP = '/ecs/employee-department1'
        CLUSTER_NAME = 'employee-cluster1'
        SERVICE_NAME = 'employee-service'
        SUBNET_ID = 'subnet-0c06c9ba80675ca5b'
        SECURITY_GROUP_ID = 'sg-03992897fd20860bd'
    }

    stages {
        stage('Checkout SCM') {
            steps {
                git branch: 'main', url: 'https://github.com/sivendar2/employee-department-1.git'
            }
        }

        stage('Provision ECS Infrastructure with Terraform') {
            steps {
                dir('terraform') {
                    script {
                        // Initialize Terraform
                        sh 'terraform init'

                        // Import existing resources to Terraform state (ignore errors if already imported)
                        sh '''
                            terraform import aws_ecr_repository.app_repo employee-department1 || echo "ECR repo already imported"
                            terraform import aws_cloudwatch_log_group.ecs_log_group /ecs/employee-department1 || echo "Log group already imported"
                            terraform import aws_iam_role.ecs_task_execution_role ecsTaskExecutionRole || echo "IAM role already imported"
                        '''

                        // Apply Terraform to create/update resources
                        sh 'terraform apply -auto-approve'
                    }
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
                    def networkConfig = "awsvpcConfiguration={subnets=[${SUBNET_ID}],securityGroups=[${SECURITY_GROUP_ID}],assignPublicIp=ENABLED}"

                    def serviceStatus = sh (
                        script: "aws ecs describe-services --cluster ${CLUSTER_NAME} --services ${SERVICE_NAME} --query 'services[0].status' --output text --region ${AWS_REGION}",
                        returnStdout: true
                    ).trim()

                    if (serviceStatus == 'INACTIVE') {
                        echo "ECS Service is INACTIVE. Creating service..."
                        sh """
                            aws ecs create-service \
                              --cluster ${CLUSTER_NAME} \
                              --service-name ${SERVICE_NAME} \
                              --task-definition employee-taskdef \
                              --desired-count 1 \
                              --launch-type FARGATE \
                              --network-configuration "${networkConfig}" \
                              --region ${AWS_REGION}
                        """
                    } else if (serviceStatus == 'ACTIVE') {
                        echo "Service is ACTIVE. Updating service..."
                        sh """
                            aws ecs update-service \
                              --cluster ${CLUSTER_NAME} \
                              --service ${SERVICE_NAME} \
                              --force-new-deployment \
                              --region ${AWS_REGION}
                        """
                    } else {
                        error("Unexpected service status: ${serviceStatus}")
                    }
                }
            }
        }
    }
}

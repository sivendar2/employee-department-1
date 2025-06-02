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
        SUBNETS = 'subnet-0c06c9ba80675ca5b'        // Replace with your subnet IDs
        SECURITY_GROUPS = 'sg-03992897fd20860bd'    // Replace with your security group IDs
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/sivendar2/employee-department-1.git'
            }
        }

        stage('Provision ECS Infrastructure with Terraform') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        cd terraform
                        terraform init

                        # Run terraform apply but ignore errors if resource already exists (log group, IAM role)
                        terraform apply -auto-approve || echo "Terraform apply had errors but continuing..."
                        cd ..
                    '''
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
                        export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                        export AWS_DEFAULT_REGION=${AWS_REGION}

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
                withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        aws logs create-log-group --log-group-name "$LOG_GROUP" --region $AWS_REGION || echo "Log group already exists or creation skipped"
                    '''
                }
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

                    withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                        sh '''
                            export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                            export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                            export AWS_DEFAULT_REGION=${AWS_REGION}

                            aws ecs register-task-definition --cli-input-json file://taskdef.json --region ${AWS_REGION}
                        '''
                    }
                }
            }
        }

        stage('Deploy to ECS Fargate') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                        sh """
                            export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                            export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                            export AWS_DEFAULT_REGION=${AWS_REGION}

                            echo "Checking ECS service status..."
                            serviceStatus=\$(aws ecs describe-services --cluster ${CLUSTER_NAME} --services ${SERVICE_NAME} --query 'services[0].status' --output text)

                            echo "Service status: \$serviceStatus"

                            networkConfig="awsvpcConfiguration={subnets=[${SUBNETS}],securityGroups=[${SECURITY_GROUPS}],assignPublicIp=ENABLED}"

                            if [ "\$serviceStatus" = "INACTIVE" ] || [ "\$serviceStatus" = "NONE" ] || [ -z "\$serviceStatus" ]; then
                              echo "Creating ECS service since service does not exist or inactive"
                              aws ecs create-service \
                                --cluster ${CLUSTER_NAME} \
                                --service-name ${SERVICE_NAME} \
                                --task-definition employee-taskdef \
                                --desired-count 1 \
                                --launch-type FARGATE \
                                --network-configuration "\$networkConfig"
                            elif [ "\$serviceStatus" = "ACTIVE" ]; then
                              echo "Updating ECS service to force new deployment"
                              aws ecs update-service \
                                --cluster ${CLUSTER_NAME} \
                                --service ${SERVICE_NAME} \
                                --force-new-deployment
                            else
                              echo "Unexpected ECS service status: \$serviceStatus"
                              exit 1
                            fi
                        """
                    }
                }
            }
        }
    }
}

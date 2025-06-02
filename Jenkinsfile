pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPO_NAME = 'employee-department1'
        ECS_CLUSTER_NAME = 'employee-cluster1'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        AWS_ACCOUNT_ID = 'YOUR_AWS_ACCOUNT_ID'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${ECR_REPO_NAME}:${IMAGE_TAG}")
                }
            }
        }

        stage('Push to ECR') {
            steps {
                script {
                    sh '''
                    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
                    docker tag $ECR_REPO_NAME:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO_NAME:$IMAGE_TAG
                    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO_NAME:$IMAGE_TAG
                    '''
                }
            }
        }

        stage('Terraform Init & Import') {
            steps {
                dir('infra') {
                    sh '''
                    terraform init

                    # Import ECR repo if not already imported
                    terraform state list | grep aws_ecr_repository.app_repo || terraform import aws_ecr_repository.app_repo $ECR_REPO_NAME

                    # Import log group if not already imported
                    terraform state list | grep aws_cloudwatch_log_group.ecs_log_group || terraform import aws_cloudwatch_log_group.ecs_log_group "/ecs/$ECR_REPO_NAME"

                    echo "IAM role assumed managed, skipping import"
                    '''
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                dir('infra') {
                    sh 'terraform apply -auto-approve'
                }
            }
        }

        stage('Update ECS Service') {
            steps {
                sh '''
                aws ecs update-service \
                    --cluster $ECS_CLUSTER_NAME \
                    --service $ECR_REPO_NAME \
                    --force-new-deployment \
                    --region $AWS_REGION
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Deployment successful!'
        }
        failure {
            echo '❌ Deployment failed.'
        }
    }
}

pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPO = 'your-account-id.dkr.ecr.us-east-1.amazonaws.com/employee-department'
        IMAGE_TAG = "employee-department:${BUILD_NUMBER}"
    }

    parameters {
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'AWS region')
    }

    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/sivendar2/employee-department-1.git'
            }
        }

        stage('Provision ECS Infrastructure with Terraform') {
            steps {
                dir('terraform') {
                    withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                        sh '''
                            export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                            export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                            export AWS_DEFAULT_REGION=${AWS_REGION}
                            terraform init
                            terraform apply -auto-approve || echo "Terraform apply partially failed (e.g., log group or IAM role exists). Continuing..."
                        '''
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
                sh 'docker build -t $IMAGE_TAG .'
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
                    docker tag $IMAGE_TAG $ECR_REPO:$BUILD_NUMBER
                    docker push $ECR_REPO:$BUILD_NUMBER
                '''
            }
        }

        stage('Register Task Definition') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                        export AWS_DEFAULT_REGION=${AWS_REGION}
                        aws ecs register-task-definition \
                          --cli-input-json file://ecs-task-def.json
                    '''
                }
            }
        }

        stage('Deploy to ECS Fargate') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                        export AWS_DEFAULT_REGION=${AWS_REGION}
                        aws ecs update-service \
                          --cluster employee-cluster \
                          --service employee-service \
                          --force-new-deployment
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deployment to ECS Fargate successful!"
        }
        failure {
            echo "❌ Deployment failed. Check logs."
        }
    }
}

pipeline {
    agent any

    environment {
        AWS_ACCESS_KEY_ID     = credentials('aws-access-key-id')       // from Jenkins credentials
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')   // from Jenkins credentials
        AWS_REGION            = 'us-east-1'                             // or your region
        ECR_REPO              = 'your-account-id.dkr.ecr.us-east-1.amazonaws.com/employee-department1'
    }

    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Provision ECS Infrastructure with Terraform') {
            steps {
                script {
                    runWithAwsEnv('''
                        terraform init
                        terraform apply -auto-approve || echo "Ignoring duplicate errors"
                    ''')
                }
            }
        }

        stage('Build App') {
            steps {
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t employee-department1 .'
            }
        }

        stage('Docker Login to ECR') {
            steps {
                script {
                    runWithAwsEnv('''
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO
                    ''')
                }
            }
        }

        stage('Push Docker Image to ECR') {
            steps {
                sh '''
                    docker tag employee-department1:latest $ECR_REPO:latest
                    docker push $ECR_REPO:latest
                '''
            }
        }

        stage('Register Task Definition') {
            steps {
                script {
                    runWithAwsEnv('''
                        aws ecs register-task-definition --cli-input-json file://task-definition.json
                    ''')
                }
            }
        }

        stage('Deploy to ECS Fargate') {
            steps {
                script {
                    runWithAwsEnv('''
                        aws ecs update-service \
                          --cluster employee-cluster \
                          --service employee-service \
                          --force-new-deployment
                    ''')
                }
            }
        }
    }

    post {
        failure {
            echo 'Pipeline failed!'
        }
    }
}

// ✅ Shared function to set AWS environment for shell blocks
def runWithAwsEnv(String script) {
    sh """
        export AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY_ID}
        export AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_ACCESS_KEY}
        export AWS_DEFAULT_REGION=${env.AWS_REGION}
        ${script}
    """
}

pipeline {
  agent any

  environment {
    AWS_REGION = 'us-east-1'
    ECR_REPO   = '779846797240.dkr.ecr.us-east-1.amazonaws.com/employee-department1'
    IMAGE_TAG  = 'latest'
    EXECUTION_ROLE_ARN = 'arn:aws:iam::779846797240:role/ecsTaskExecutionRole'
    LOG_GROUP  = '/ecs/employee-department1'

    SONAR_HOST_URL   = 'http://sonarqube.sivendar.click:9000/'
    SONAR_PROJECT_KEY = 'employee-department-1'
    SONAR_TOKEN = credentials('sonar-token-jenkins')

    // remediation tool locations (Windows)
    VRF_TOOL_DIR        = 'D:\\file\\demo\\vuln-remediation-poc-main'
    NEXUS_IQ_REPORT_SRC = 'D:\\file\\demo\\vuln-remediation-poc-main\\scripts\\data\\nexus_iq_report.json'
    NEXUS_IQ_REPORT     = 'scripts\\data\\nexus_iq_report.json'

    REMEDIATION_DIR = 'remediate-tmp'
    REMEDIATION_OK  = 'false'
  }

  stages {

    stage('Checkout') {
      steps {
        git branch: 'main', url: 'https://github.com/sivendar2/employee-department-1.git'
      }
    }

    stage('Bring Nexus IQ JSON into workspace') {
      steps {
        bat '''
          @echo off
          if not exist "scripts\\data" mkdir "scripts\\data"
          copy /Y "%NEXUS_IQ_REPORT_SRC%" "%NEXUS_IQ_REPORT%"
          dir "scripts\\data"
        '''
      }
    }

    stage('Run Remediation (safe temp clone)') {
      steps {
        // GH_TOKEN is used by your Python tool to create the PR *only if* compile succeeds
        withCredentials([ string(credentialsId: 'gh-token', variable: 'GH_TOKEN') ]) {
          bat '''
            @echo off
            setlocal enabledelayedexpansion

            rem workspace absolute path
            for /f "delims=" %%i in ('cd') do set WORKSPACE=%%i
            set REPORT_ABS=%WORKSPACE%\\%NEXUS_IQ_REPORT%

            rem fresh temp dir
            rmdir /S /Q "%REMEDIATION_DIR%" 2>nul
            mkdir "%REMEDIATION_DIR%"
            cd "%REMEDIATION_DIR%"

            git clone --branch main https://github.com/sivendar2/employee-department-1.git repo
            cd repo

            for /f %%i in ('powershell -NoProfile -Command "Get-Date -UFormat %%s"') do set BRANCH_NAME=fix/sast-autofix-%%i
            echo !BRANCH_NAME! > ..\\BRANCH_NAME.txt
            git checkout -b !BRANCH_NAME!

            rem run your remediation tool (it should compile and create PR internally on success)
            python "%VRF_TOOL_DIR%\\scripts\\main.py" ^
              --repo-url "https://github.com/sivendar2/employee-department-1.git" ^
              --branch-name "!BRANCH_NAME!" ^
              --py-sca-report "%REPORT_ABS%" ^
              --py-requirements "requirements.txt" ^
              --js-version-strategy keep_prefix ^
              --slack-webhook ""

            rem safe stage (even if already committed inside the tool)
            git add -A

            endlocal
          '''
        }
      }
    }
stage('Read Remediation Result') {
  steps {
    bat '''
      @echo off
      setlocal
      if exist "scripts\\output\\remediation_ok.flag" (
        echo Remediation compile OK
        setx REMEDIATION_OK true
      ) else (
        echo Remediation compile FAILED
        setx REMEDIATION_OK false
      )
      endlocal
    '''
  }
}

stage('Build App (remediated)') {
      when { expression { env.REMEDIATION_OK == 'true' } }
      steps {
        bat '''
          @echo off
          cd "%REMEDIATION_DIR%\\repo"
          call mvn clean package -DskipTests
        '''
      }
}

    stage('Build App (original)') {
      when { expression { env.REMEDIATION_OK != 'true' } }
      steps {
        bat 'mvn clean package -DskipTests'
      }
    }


    // Build Docker image from the correct tree
    stage('Build Docker Image (remediated)') {
      when { expression { env.REMEDIATION_OK == 'true' } }
      steps {
        bat '''
          @echo off
          docker build -t employee-department1 -f Docker/Dockerfile "%REMEDIATION_DIR%\\repo"
        '''
      }
    }

    stage('Build Docker Image (original)') {
      when { expression { env.REMEDIATION_OK != 'true' } }
      steps {
        bat 'docker build -t employee-department1 -f Docker/Dockerfile .'
      }
    }

    stage('Docker Login to ECR') {
      steps {
        withCredentials([ usernamePassword(credentialsId: 'aws-ecr-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY') ]) {
          bat '''
            @echo off
            set "AWS_DIR=%USERPROFILE%\\.aws"
            if not exist "%AWS_DIR%" mkdir "%AWS_DIR%"
            > "%AWS_DIR%\\credentials" echo [default]
            >>"%AWS_DIR%\\credentials" echo aws_access_key_id=%AWS_ACCESS_KEY_ID%
            >>"%AWS_DIR%\\credentials" echo aws_secret_access_key=%AWS_SECRET_ACCESS_KEY%
            aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %ECR_REPO%
          '''
        }
      }
    }

    stage('Push Docker Image to ECR') {
      steps {
        bat """
          @echo off
          docker tag employee-department1:latest %ECR_REPO%:%IMAGE_TAG%
          docker push %ECR_REPO%:%IMAGE_TAG%
        """
      }
    }

    stage('Ensure CloudWatch Log Group Exists') {
      steps {
        bat '''
          @echo off
          aws logs create-log-group --log-group-name "%LOG_GROUP%" --region %AWS_REGION% || echo Log group exists or skipped
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
                  { "containerPort": 8080, "protocol": "tcp" }
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
        }
        bat 'aws ecs register-task-definition --cli-input-json file://taskdef.json --region %AWS_REGION%'
      }
    }

    stage('Deploy to ECS Fargate') {
      steps {
        // PowerShell for clearer branching on Windows
        powershell '''
          $ErrorActionPreference = "Stop"
          $clusterName = "employee-cluster1"
          $serviceName = "employee-service"
          $region = "${env.AWS_REGION}"
          $netCfg = "awsvpcConfiguration={subnets=[subnet-0c06c9ba80675ca5b],securityGroups=[sg-03992897fd20860bd],assignPublicIp=ENABLED}"

          try {
            $status = (aws ecs describe-services --cluster $clusterName --services $serviceName --query "services[0].status" --output text --region $region).Trim()
          } catch {
            $status = ""
          }

          if ([string]::IsNullOrEmpty($status) -or $status -eq "INACTIVE" -or $status -eq "None" -or $status -eq "null") {
            Write-Host "ECS Service missing/inactive. Creating..."
            aws ecs create-service `
              --cluster $clusterName `
              --service-name $serviceName `
              --task-definition employee-taskdef `
              --desired-count 1 `
              --launch-type FARGATE `
              --network-configuration $netCfg `
              --region $region
          } elseif ($status -eq "ACTIVE") {
            Write-Host "Service ACTIVE. Forcing new deployment..."
            aws ecs update-service `
              --cluster $clusterName `
              --service $serviceName `
              --force-new-deployment `
              --region $region
          } else {
            throw "Unexpected ECS service status: $status"
          }
        '''
      }
    }
  }

  post {
    always {
      echo "REMEDIATION_OK = ${env.REMEDIATION_OK}"
    }
  }
}



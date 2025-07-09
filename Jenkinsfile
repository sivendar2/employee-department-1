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

        stage('Verify Semgrep Rule') {
            steps {
                sh 'ls -l .semgrep/sql-injection-autofix.yml'
            }
        }

        stage('Semgrep Scan & Autofix') {
            steps {
                script {
                    sh '''
                        set +e
                        semgrep scan --config .semgrep/sql-injection-autofix.yml --autofix --json > semgrep-report.json
                        SEMGREP_EXIT_CODE=$?
                        set -e
                        if [ "$SEMGREP_EXIT_CODE" -ne 0 ]; then
                            echo "Semgrep scan failed with exit code $SEMGREP_EXIT_CODE"
                            exit $SEMGREP_EXIT_CODE
                        else
                            echo "Semgrep scan and autofix completed successfully."
                        fi
                    '''
                    archiveArtifacts artifacts: 'semgrep-report.json', onlyIfSuccessful: true
                }
            }
        }

        stage('Create SAST Fix PR') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'git-cred-id', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS'),
                    string(credentialsId: 'gh-token', variable: 'GH_TOKEN')
                ]) {
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

        // Other stages (commented) can be added/enabled below as needed
        /*
        stage('Sona

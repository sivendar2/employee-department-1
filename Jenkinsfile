pipeline {
  agent any

  environment {
    AWS_REGION = 'us-east-1'
    ECR_REPO   = '779846797240.dkr.ecr.us-east-1.amazonaws.com/employee-department1'
    IMAGE_TAG  = 'latest'
    EXECUTION_ROLE_ARN = 'arn:aws:iam::779846797240:role/ecsTaskExecutionRole'
    LOG_GROUP  = '/ecs/employee-department1'

    SONAR_HOST_URL    = 'http://sonarqube.sivendar.click:9000/'
    SONAR_PROJECT_KEY = 'employee-department-1'
    SONAR_TOKEN       = credentials('sonar-token-jenkins')

    // remediation tool locations (Windows)
    VRF_TOOL_DIR        = 'D:\\file\\demo\\vuln-remediation-poc-main'
    NEXUS_IQ_REPORT_SRC = 'D:\\file\\demo\\vuln-remediation-poc-main\\scripts\\data\\nexus_iq_report.json'
    NEXUS_IQ_REPORT     = 'scripts\\data\\nexus_iq_report.json'

    REMEDIATION_DIR = 'remediate-tmp'
    REMEDIATION_OK  = 'false'

    PYTHONUNBUFFERED = '1'
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
        withCredentials([ string(credentialsId: 'gh-token', variable: 'GH_TOKEN') ]) {
          withEnv(['MVN_EXE=C:\\maven\\bin\\mvn.cmd']) {
            bat '''
              @echo off
              setlocal enabledelayedexpansion

              for /f "delims=" %%i in ('cd') do set WORKSPACE=%%i
              set REPORT_ABS=%WORKSPACE%\\%NEXUS_IQ_REPORT%
              set OUT_DIR=%WORKSPACE%\\scripts\\output

              rem ---- wipe outputs so we never read stale flags/logs ----
              if exist "%OUT_DIR%" rmdir /S /Q "%OUT_DIR%" 2>nul
              mkdir "%OUT_DIR%"

              rem fresh temp clone dir
              rmdir /S /Q "%REMEDIATION_DIR%" 2>nul
              mkdir "%REMEDIATION_DIR%"
              cd "%REMEDIATION_DIR%"

              for /f %%i in ('powershell -NoProfile -Command "Get-Date -UFormat %%s"') do set BRANCH_NAME=fix/sast-autofix-%%i
              echo !BRANCH_NAME! > BRANCH_NAME.txt

              python "%VRF_TOOL_DIR%\\scripts\\main.py" ^
                --repo-url "https://github.com/sivendar2/employee-department-1.git" ^
                --branch-name "!BRANCH_NAME!" ^
                --nexus-iq-report "%REPORT_ABS%" ^
                --py-sca-report "scripts/data/py_sca_report.json" ^
                --py-requirements "requirements.txt" ^
                --js-version-strategy keep_prefix ^
                --output-dir "%OUT_DIR%" ^
                --slack-webhook ""

              endlocal
            '''
          }
        }
      }
    }

    stage('Read Remediation Result') {
      steps {
        // show what's there (debugging)
        bat 'dir /a "scripts\\output" || echo (no output dir)'

        script {
          // Build an absolute Windows path and rely ONLY on PowerShell Test-Path
          def ws      = pwd().replace('/', '\\')
          def flagAbs = "${ws}\\scripts\\output\\remediation_ok.flag"

          def ok = powershell(returnStdout: true, script: """
            \$p = '${flagAbs}';
            if (Test-Path -LiteralPath \$p) { 'true' } else { 'false' }
          """).trim().toLowerCase() == 'true'

          env.REMEDIATION_OK = ok ? 'true' : 'false'
          echo "REMEDIATION_OK = ${env.REMEDIATION_OK}"
        }

        // keep all tool outputs
        archiveArtifacts artifacts: 'scripts/output/**', allowEmptyArchive: true
      }
    }

    stage('Show Remediation Compile Errors') {
      when { expression { env.REMEDIATION_OK != 'true' } }
      steps {
        powershell 'if (Test-Path "scripts/output/remediation_compile.log") { Get-Content "scripts/output/remediation_compile.log" -Tail 200 }'
        powershell 'if (Test-Path "scripts/output/main_log.txt") { Get-Content "scripts/output/main_log.txt" -Tail 120 }'
      }
    }

    stage('Build App (remediated)') {
      when { expression { env.REMEDIATION_OK == 'true' } }
      steps {
        bat '''
          @echo off
          cd "%REMEDIATION_DIR%\\repo"
          call mvn -B -DskipTests clean package
        '''
      }
    }

    stage('Build App (original)') {
      when { expression { env.REMEDIATION_OK != 'true' } }
      steps {
        bat 'mvn clean package -DskipTests'
      }
    }
  }

  post {
    always {
      echo "REMEDIATION_OK = ${env.REMEDIATION_OK}"
    }
  }
}

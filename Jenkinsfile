pipeline {
  agent any

  environment {
    AWS_REGION = 'us-east-1'

    // Your app image (kept for compatibility if you use it elsewhere)
    ECR_REPO   = '779846797240.dkr.ecr.us-east-1.amazonaws.com/employee-department1'
    IMAGE_TAG  = 'latest'

    // VRM image in ECR (Python runtime + deps)
    VRM_ECR_REPO  = '779846797240.dkr.ecr.us-east-1.amazonaws.com/vrm'
    VRM_IMAGE_TAG = '0.1.3'   // use 'latest' if you prefer

    // VRM tool source repo to mount into the container
    VRM_TOOL_REPO   = 'https://github.com/sivendar2/vulnerable_remediation.git'
    VRM_TOOL_BRANCH = 'main'  // change if needed

    EXECUTION_ROLE_ARN = 'arn:aws:iam::779846797240:role/ecsTaskExecutionRole'
    LOG_GROUP          = '/ecs/employee-department1'

    SONAR_HOST_URL    = 'http://sonarqube.sivendar.click:9000/'
    SONAR_PROJECT_KEY = 'employee-department-1'
    SONAR_TOKEN       = credentials('sonar-token-jenkins')

    // Nexus IQ JSON (copied from a known Windows path into the workspace)
    NEXUS_IQ_REPORT_SRC = 'D:\\file\\demo\\vuln-remediation-poc-main\\scripts\\data\\nexus_iq_report.json'
    NEXUS_IQ_REPORT     = 'scripts\\data\\nexus_iq_report.json'

    // Working dirs in workspace
    REMEDIATION_DIR = 'remediate-tmp'

    // Better Python logs
    PYTHONUNBUFFERED = '1'
  }

  stages {

    stage('Checkout app repo') {
      steps {
        git branch: 'main', url: 'https://github.com/sivendar2/employee-department-1.git'
      }
    }

    stage('Checkout VRM tool repo') {
      steps {
        dir('vrm-tool') {
          git branch: "${env.VRM_TOOL_BRANCH}", url: "${env.VRM_TOOL_REPO}"
        }
        bat 'dir /b vrm-tool\\scripts || echo (vrm-tool/scripts missing)'
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

    stage('Login & Pull VRM Image') {
      steps {
        bat """
          @echo off
          aws ecr get-login-password --region %AWS_REGION% ^
            | docker login --username AWS --password-stdin 779846797240.dkr.ecr.us-east-1.amazonaws.com
          docker pull %VRM_ECR_REPO%:%VRM_IMAGE_TAG%
        """
      }
    }

    stage('Run Remediation (safe temp clone)') {
      steps {
        withCredentials([ string(credentialsId: 'gh-token', variable: 'GH_TOKEN') ]) {
          bat '''
            @echo off
            setlocal enabledelayedexpansion

            for /f "delims=" %%i in ('cd') do set WORKSPACE=%%i
            set OUT_DIR=%WORKSPACE%\\scripts\\output

            rem ---- wipe previous outputs to avoid stale flags/logs ----
            if exist "%OUT_DIR%" rmdir /S /Q "%OUT_DIR%" 2>nul
            mkdir "%OUT_DIR%"

            rem ---- start fresh temp clone dir (host side) ----
            rmdir /S /Q "%REMEDIATION_DIR%" 2>nul
            mkdir "%REMEDIATION_DIR%"
            cd "%REMEDIATION_DIR%"

            for /f %%i in ('powershell -NoProfile -Command "Get-Date -UFormat %%s"') do set BRANCH_NAME=fix/sast-autofix-%%i
            echo !BRANCH_NAME! > BRANCH_NAME.txt

            rem ---- sanity: list tool path before running ----
            cd "%WORKSPACE%"
            docker run --rm ^
              -v "%WORKSPACE%\\vrm-tool":/vrm ^
              %VRM_ECR_REPO%:%VRM_IMAGE_TAG% ^
              sh -lc "ls -al /vrm/scripts || true"

            rem ---- run the VRM container; mount workspace and the tool repo ----
            docker run --rm ^
              -e GH_TOKEN=%GH_TOKEN% ^
              -e PYTHONUNBUFFERED=1 ^
              -e BRANCH_NAME=!BRANCH_NAME! ^
              -v "%WORKSPACE%":/workspace ^
              -v "%WORKSPACE%\\vrm-tool":/vrm ^
              %VRM_ECR_REPO%:%VRM_IMAGE_TAG% ^
              python -u /vrm/scripts/main.py ^
                --repo-url "https://github.com/sivendar2/employee-department-1.git" ^
                --branch-name "!BRANCH_NAME!" ^
                --nexus-iq-report "/workspace/scripts/data/nexus_iq_report.json" ^
                --py-sca-report "/workspace/scripts/data/py_sca_report.json" ^
                --py-requirements "/workspace/requirements.txt" ^
                --js-version-strategy keep_prefix ^
                --output-dir "/workspace/scripts/output" ^
                --slack-webhook ""

            endlocal
          '''
        }
      }
    }

    stage('Read Remediation Result') {
      steps {
        script {
          bat 'dir /a "scripts\\output" || echo (no output dir)'

          def hasFix = false
          def reason = 'no flag/status found'

          if (fileExists('scripts/output/remediation_ok.flag')) {
            def content = readFile(file: 'scripts/output/remediation_ok.flag', encoding: 'UTF-8').trim()
            echo "FLAG CONTENT: '${content}'"
            hasFix = content.length() > 0
            reason = 'flag file present'
          } else if (fileExists('scripts/output/remediation_status.json')) {
            def okStr = powershell(returnStdout: true, script: '''
              $p = "scripts\\output\\remediation_status.json"
              if (Test-Path -LiteralPath $p) {
                try {
                  $j = Get-Content -LiteralPath $p | ConvertFrom-Json
                  if ($j.compile_ok -eq $true) { "true" } else { "false" }
                } catch { "false" }
              } else { "false" }
            ''').trim().toLowerCase()
            hasFix = (okStr == 'true')
            reason = "status.json compile_ok=${okStr}"
          }

          currentBuild.displayName = "#${env.BUILD_NUMBER} • remediated=${hasFix}"
          echo "Decision: remediated=${hasFix} (${reason})"

          writeFile file: 'scripts/output/decision.txt', text: (hasFix ? 'remediated' : 'original'), encoding: 'UTF-8'
          archiveArtifacts artifacts: 'scripts/output/*', allowEmptyArchive: true
        }
      }
    }

    stage('Build App (choose by flag)') {
      steps {
        script {
          def decision = 'original'
          if (fileExists('scripts/output/decision.txt')) {
            decision = readFile(file: 'scripts/output/decision.txt', encoding: 'UTF-8').trim()
          }
          echo "Build decision from file: ${decision}"

          if (decision == 'remediated') {
            echo '➡ Building REMEDIATED tree'
            bat '''
              @echo off
              cd "%REMEDIATION_DIR%\\repo"
              call mvn -B -DskipTests clean package
            '''
          } else {
            echo '➡ Building ORIGINAL tree'
            bat 'mvn clean package -DskipTests'
          }
        }
      }
    }
  }

  post {
    always {
      echo 'Pipeline finished.'
    }
  }
}

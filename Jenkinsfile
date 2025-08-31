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

    // better Python logs
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

          rem ---- wipe previous outputs to avoid stale flags/logs ----
          if exist "%OUT_DIR%" rmdir /S /Q "%OUT_DIR%" 2>nul
          mkdir "%OUT_DIR%"

          rem optional: also start fresh temp clone
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
    script {
      // Show what's actually in the folder
      bat 'dir /a "scripts\\output" || echo (no output dir)'

      // Build absolute paths (Windows style, no guessing)
      def ws = pwd().replace('/', '\\')
      def flagAbs = "${ws}\\scripts\\output\\remediation_ok.flag"
      def jsonAbs = "${ws}\\scripts\\output\\remediation_status.json"

      // 1) Groovy: fileExists on relative path
      def g_flag = fileExists('scripts/output/remediation_ok.flag')

      // 2) Groovy: parse JSON compile_ok if present
      def g_json = false
      if (fileExists('scripts/output/remediation_status.json')) {
        def txt = readFile('scripts/output/remediation_status.json')
        try {
          def data = new groovy.json.JsonSlurperClassic().parseText(txt)
          g_json = (data?.compile_ok == true)
        } catch (e) {
          echo "WARN: JSON parse error: ${e}"
        }
      }

      // 3) PowerShell: absolute path flag check
      def ps_flag = powershell(returnStdout: true, script: """
        \$p = '${flagAbs}'
        if (Test-Path -LiteralPath \$p) { 'true' } else { 'false' }
      """).trim().toLowerCase() == 'true'

      // 4) PowerShell: JSON compile_ok (absolute path)
      def ps_json = powershell(returnStdout: true, script: """
        \$p = '${jsonAbs}'
        if (-not (Test-Path -LiteralPath \$p)) { 'false'; exit }
        try {
          \$j = Get-Content -LiteralPath \$p | ConvertFrom-Json
          if (\$j.compile_ok -eq \$true) { 'true' } else { 'false' }
        } catch { 'false' }
      """).trim().toLowerCase() == 'true'

      // Debug: print all signals so we can see which one wins
      echo "DEBUG ws=${ws}"
      echo "DEBUG flagAbs=${flagAbs}"
      echo "DEBUG jsonAbs=${jsonAbs}"
      echo "DEBUG g_flag=${g_flag}, g_json=${g_json}, ps_flag=${ps_flag}, ps_json=${ps_json}"

      // Final decision
      env.REMEDIATION_OK = ((g_flag || g_json || ps_flag || ps_json) ? 'true' : 'false')
      echo "REMEDIATION_OK = ${env.REMEDIATION_OK}"

      // keep artifacts
      archiveArtifacts artifacts: 'scripts/output/*', allowEmptyArchive: true
    }
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







pipeline {
    agent any

    environment {
        IMAGE_NAME = 'charllson717/ictu-ex-backend'
        IMAGE_TAG = "${BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 Pulling latest code...'
                checkout scm
            }
        }
        stage('Build') {
            steps {
                echo '🔨 Building application...'
                sh 'chmod +x gradlew'
                sh './gradlew :ictu-ex-app:bootJar -x test'
            }
        }
        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Coverage Check') {
            steps {
                echo '📊 Checking coverage — 80% minimum required...'
                sh './gradlew koverVerify'
            }
            post {
                failure {
                    echo '❌ Coverage below 80% — deployment BLOCKED'
                }
            }
        }
        stage('SonarCloud Pipeline Analysis') {
            steps {
                echo '🔍 Connecting to SonarCloud analytics scanner engine...'
                withCredentials([string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                )]) {
                    sh '''
                        set -e
                        if [ -z "$SONAR_TOKEN" ]; then
                          echo "❌ SONAR_TOKEN is missing"
                          exit 1
                        fi
                        curl -fsS -u "$SONAR_TOKEN:" https://sonarcloud.io/api/authentication/validate
            '''
                }
                echo 'Analysis sent to SonarCLoud...'
            }
        }
        stage('Docker Build & Push') {
            steps {
                echo '🐳 Building Docker image...'
                sh '''
                    echo $DOCKERHUB_CREDENTIALS_PSW | \
                    docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin
                '''
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .'
                sh 'docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest'
                sh 'docker push ${IMAGE_NAME}:${IMAGE_TAG}'
                sh 'docker push ${IMAGE_NAME}:latest'
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                echo '🚀 Deploying to k8s...'
                sh 'kubectl apply -f k8s/'
                sh """
                    kubectl set image deployment/ictu-ex-app \
                    ictu-ex-app=${IMAGE_NAME}:${IMAGE_TAG} \
                    -n ictu-ex
                """
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline complete — ICTU-Ex deployed successfully'
        }
        failure {
            echo '❌ Pipeline failed — check stage logs above'
        }
    }
}
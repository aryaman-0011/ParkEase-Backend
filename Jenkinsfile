// ParkEase CI/CD Pipeline
// Builds, tests, and packages all microservices automatically.

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        // ──────────────────────────────────────────────
        // Stage 1: Checkout source code
        // ──────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                echo "Commit: ${env.GIT_COMMIT ?: 'N/A'}"
            }
        }

        // ──────────────────────────────────────────────
        // Stage 2: Install Maven (not pre-installed in Jenkins image)
        // ──────────────────────────────────────────────
        stage('Setup Maven') {
            steps {
                sh '''
                    if ! command -v mvn &> /dev/null; then
                        echo "=== Installing Maven ==="
                        apt-get update -qq && apt-get install -y -qq maven > /dev/null 2>&1
                        mvn --version
                    else
                        echo "Maven already installed"
                        mvn --version
                    fi
                '''
            }
        }

        // ──────────────────────────────────────────────
        // Stage 3: Build all microservices
        // ──────────────────────────────────────────────
        stage('Build') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'auth-service',
                        'booking-service',
                        'parkinglot-service',
                        'spot-service',
                        'vehicle-service',
                        'payment-service',
                        'notification-service',
                        'analytics-service'
                    ]
                    for (svc in services) {
                        dir(svc) {
                            echo "=== Building ${svc} ==="
                            sh 'mvn clean compile -DskipTests -q'
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 4: Run unit tests
        // ──────────────────────────────────────────────
        stage('Test') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'auth-service',
                        'booking-service',
                        'parkinglot-service',
                        'spot-service',
                        'vehicle-service',
                        'payment-service',
                        'notification-service',
                        'analytics-service'
                    ]
                    for (svc in services) {
                        dir(svc) {
                            echo "=== Testing ${svc} ==="
                            sh 'mvn test -q'
                        }
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                         testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 5: Package JARs
        // ──────────────────────────────────────────────
        stage('Package') {
            steps {
                script {
                    def services = [
                        'api-gateway',
                        'auth-service',
                        'booking-service',
                        'parkinglot-service',
                        'spot-service',
                        'vehicle-service',
                        'payment-service',
                        'notification-service',
                        'analytics-service'
                    ]
                    for (svc in services) {
                        dir(svc) {
                            echo "=== Packaging ${svc} ==="
                            sh 'mvn package -DskipTests -q'
                        }
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar',
                                     fingerprint: true,
                                     allowEmptyArchive: true
                }
            }
        }
    }

    post {
        success {
            echo '====================================='
            echo '  ParkEase Pipeline: BUILD SUCCESS'
            echo '====================================='
        }
        failure {
            echo '====================================='
            echo '  ParkEase Pipeline: BUILD FAILED'
            echo '====================================='
        }
        always {
            cleanWs()
        }
    }
}

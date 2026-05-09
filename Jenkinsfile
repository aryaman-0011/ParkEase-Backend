// ParkEase CI/CD Pipeline
// This pipeline builds, tests, and analyzes all microservices automatically on every push.

pipeline {
    agent any

    environment {
        SONAR_HOST  = 'http://parkease-sonarqube:9000'
    }

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
                echo "Branch: ${env.BRANCH_NAME ?: 'N/A'}"
                echo "Commit: ${env.GIT_COMMIT ?: 'N/A'}"
            }
        }

        // ──────────────────────────────────────────────
        // Stage 2: Build all microservices (compile only)
        // ──────────────────────────────────────────────
        stage('Build') {
            steps {
                script {
                    def services = [
                        'service-registry',
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
                            sh 'chmod +x mvnw && ./mvnw clean compile -DskipTests -q'
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 3: Run unit tests + generate JaCoCo reports
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
                            sh 'chmod +x mvnw && ./mvnw test -q'
                        }
                    }
                }
            }
            post {
                always {
                    // Publish JUnit test results from all services
                    junit allowEmptyResults: true,
                         testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ──────────────────────────────────────────────
        // Stage 4: Package JARs
        // ──────────────────────────────────────────────
        stage('Package') {
            steps {
                script {
                    def services = [
                        'service-registry',
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
                            sh 'chmod +x mvnw && ./mvnw package -DskipTests -q'
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

    // ──────────────────────────────────────────────
    // Post-build actions
    // ──────────────────────────────────────────────
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

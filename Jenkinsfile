// ParkEase CI/CD Pipeline
// Builds, tests, and packages all microservices automatically.

pipeline {
    agent any

    environment {
        MAVEN_HOME = "${WORKSPACE}/maven"
        PATH       = "${MAVEN_HOME}/bin:${env.PATH}"
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "Commit: ${env.GIT_COMMIT ?: 'N/A'}"
            }
        }

        stage('Setup Maven') {
            steps {
                sh '''
                    if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
                        echo "=== Downloading Maven ==="
                        curl -sL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar xz -C "${WORKSPACE}"
                        mv "${WORKSPACE}/apache-maven-3.9.6" "${MAVEN_HOME}"
                    fi
                    mvn --version
                '''
            }
        }

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
                            sh 'mvn test -q -Dtest="!*ApplicationTests" -Dsurefire.failIfNoSpecifiedTests=false'
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

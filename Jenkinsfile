// ParkEase CI/CD Pipeline
// Builds, tests, packages, and deploys all microservices automatically.

pipeline {
    agent any

    environment {
        MAVEN_HOME  = "${WORKSPACE}/maven"
        PATH        = "${MAVEN_HOME}/bin:${env.PATH}"
        DEPLOY_HOST = '172.17.0.1'   // Docker bridge gateway → EC2 host
        DEPLOY_USER = 'ubuntu'
        DEPLOY_DIR  = '/opt/parkease/jars'
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
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
                        'service-registry',
                        'admin-server',
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

        stage('SonarQube Analysis') {
            when {
                expression { return params.RUN_SONAR }
            }
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
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
                                echo "=== Analyzing ${svc} ==="
                                sh """
                                    mvn sonar:sonar -q \
                                        -Dsonar.projectKey=parkease-${svc} \
                                        -Dsonar.projectName=\"ParkEase ${svc}\" \
                                        -Dsonar.host.url=http://sonarqube:9000 \
                                        -Dsonar.token=\$SONAR_TOKEN
                                """
                            }
                        }
                    }
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    def services = [
                        'service-registry',
                        'admin-server',
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

        stage('Deploy Backend') {
            when {
                expression {
                    return env.BRANCH_NAME == 'feature/report-service' ||
                           env.GIT_BRANCH == 'origin/feature/report-service'
                }
            }
            steps {
                script {
                    def services = [
                        'service-registry',
                        'admin-server',
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

                    echo '=== Copying JARs to deploy directory ==='
                    for (svc in services) {
                        sh "cp ${svc}/target/*.jar ${DEPLOY_DIR}/${svc}.jar"
                    }

                    echo '=== Restarting services on EC2 ==='
                    sshagent(['ec2-ssh-key']) {
                        // Stop all services (reverse order)
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                            'for svc in analytics-service notification-service payment-service vehicle-service spot-service parkinglot-service booking-service auth-service api-gateway admin-server service-registry; do
                                sudo systemctl stop parkease@\${svc} 2>/dev/null || true
                            done'
                        """

                        // Start Eureka first
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                            'sudo systemctl start parkease@service-registry && echo "Waiting for Eureka..." && sleep 20'
                        """

                        // Start infrastructure
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                            'sudo systemctl start parkease@admin-server && sudo systemctl start parkease@api-gateway && sleep 10'
                        """

                        // Start business services
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                            'for svc in auth-service booking-service parkinglot-service spot-service vehicle-service payment-service notification-service analytics-service; do
                                sudo systemctl start parkease@\${svc}
                                sleep 3
                            done'
                        """

                        // Health check
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                            'echo "=== Service Status ===" && for svc in service-registry admin-server api-gateway auth-service booking-service parkinglot-service spot-service vehicle-service payment-service notification-service analytics-service; do
                                echo -n "parkease@\${svc}: " && systemctl is-active parkease@\${svc}
                            done'
                        """
                    }
                }
            }
        }

        stage('Deploy Frontend') {
            when {
                expression {
                    return env.BRANCH_NAME == 'feature/report-service' ||
                           env.GIT_BRANCH == 'origin/feature/report-service'
                }
            }
            steps {
                sshagent(['ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} \
                        'cd /opt/parkease/parkease-frontend && \
                         git pull origin feature/report-service-frontend && \
                         export API_URL="http://13.234.116.108/api" && \
                         node scripts/write-env.js && \
                         npm install && \
                         npx ng build --configuration=production && \
                         sudo rm -rf /var/www/parkease/* && \
                         sudo cp -r dist/parkease-frontend/browser/* /var/www/parkease/ && \
                         echo "Frontend deployed successfully"'
                    """
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

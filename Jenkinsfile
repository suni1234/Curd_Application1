pipeline {
    agent {
        docker {
            image 'maven:3.9.6-eclipse-temurin-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }
    environment {
        IMAGE_NAME  = "localhost:5000/employee-service"
        IMAGE_TAG   = "${env.BUILD_NUMBER}"
        SONAR_TOKEN = credentials('sonar-token')
    }
    stages {
        stage('Checkout') { steps { checkout scm } }

        stage('Compile') { steps { sh 'mvn -B compile' } }

        stage('Test') {
            steps { sh 'mvn -B test' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }

        stage('Code Quality - SonarQube') {
            steps { sh "mvn -B sonar:sonar -Dsonar.token=${SONAR_TOKEN} -Dsonar.host.url=http://host.docker.internal:9000" }
        }

        stage('Package') { steps { sh 'mvn -B package -DskipTests' } }

        stage('Docker Build') {
            steps { sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ." }
        }

        stage('Docker Push') {
            steps { sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}" }
        }
    }
}
pipeline{
    agent any
    stages{
        // Config based on your current branch name
        stage('Build & Test Server') {
            steps{
                dir ('server'){
                    withGradle {
                        // Give permission for ./gradlew
                        sh 'chmod +x gradlew'

                        // Run the actual build & test for our server
                        sh './gradlew build'
                    }
                }
            }
        }
        stage('Build Client') {
            steps {
                script{
                    // Set default to staging for ticket purpose
                    def BUILD_MODE = ':develop'
                    if (env.BRANCH_NAME == 'staging'){
                        BUILD_MODE = ':staging'
                    }
                    else if (env.BRANCH_NAME == 'master'){
                        BUILD_MODE = ':master'
                    }
                    dir ('client') {
                        nodejs('Node18'){
                            sh "npm ci && npm run build${BUILD_MODE}"
                        }
                    }
                }
            }
        }
        stage('Build And Publish Images') {
            // condition to trigger this stage is only on staging branch
            when {
                anyOf {
                    branch 'staging'
                    branch 'develop'
                    branch 'master'
                }
            }
            steps{
                script{
                    def VERSION = env.BRANCH_NAME

                    // Building Docker image
                    withDockerRegistry(credentialsId: 'c01640b6-bb2c-4e8e-8088-5833d8cc4770', url: 'https://index.docker.io/v1/') {
                        // For Client
                        dir('client'){
                            sh "docker build . -t duycan17/course4u.client:${VERSION}"
                        }

                        // For Server
                        dir('server'){
                            sh "docker build . -t duycan17/course4u.server:${VERSION}"
                        }
                    }

                    // Publishing images
                    withDockerRegistry(credentialsId: 'c01640b6-bb2c-4e8e-8088-5833d8cc4770', url: 'https://index.docker.io/v1/') {
                        sh "docker push duycan17/course4u.client:${VERSION}" // for client
                        sh "docker push duycan17/course4u.server:${VERSION}" // for server
                    }
                }
            }
        }
        stage('Deploy') {
            // condition to trigger this stage is only on staging branch
            when {
                anyOf {
                    branch 'staging'
                    branch 'develop'
                    branch 'master'
                }
            }
            steps{
                script{
                    def PROFILE = 'develop'
                    if (env.BRANCH_NAME == 'staging') {
                        PROFILE = 'staging'
                    }
                    else if (env.BRANCH_NAME == 'master') {
                        PROFILE = 'production'
                    }
                    sshagent(['ci-user-ssh']) {
                        // Copy docker-compose.yaml to remote server
                        sh 'scp -P 8686 -o StrictHostKeyChecking=no ./docker-compose.yaml root@103.173.66.200:/home/cfu'
                        withCredentials([usernamePassword(credentialsId: 'c01640b6-bb2c-4e8e-8088-5833d8cc4770', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                            // SSH to remote server, then login to Docker Hub
                            sh 'ssh -p 8686 -o StrictHostKeyChecking=no root@103.173.66.200 "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD"'

                            // Pulling the latest Docker images and starts the server
                            sh "ssh -p 8686 -o StrictHostKeyChecking=no root@103.173.66.200 'docker compose --project-name cfu --profile ${PROFILE} up -d --pull=always'"

                            // Logout from Docker Hub
                            sh "ssh -p 8686 -o StrictHostKeyChecking=no root@103.173.66.200 'docker logout'"
                        }
                    }
                }
            }
        }
    }
}

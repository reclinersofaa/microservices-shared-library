def call(){
    pipeline {
        agent {
            label 'docker-agent-node'
        }
        
        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
            DOCKERHUB_USERNAME = 'dttreclinersofaa'  
        }

        stages{
            stage('Checkout and Detect Service'){
                steps{
                    script{
                        checkout scm
                        def serviceName = env.JOB_NAME.split('/')[0]
                        env.SERVICE_NAME = serviceName
                        echo "Checking service: ${serviceName}"
                        sh '''
                            ls -la
                            [ -f "package.json" ] && echo "YES package.json FOUND" || exit 1
                            [ -f "server.js" ] && echo "YES server.js found"
                            [ -f "test.js" ] && echo "YES test.js found"
                        '''
                    }
                }
            }
            stage('Build Service'){
                steps{
                    script{
                        echo "Building ${env.SERVICE_NAME}..."
                        sh """
                            echo "Installing dependencies..."
                            npm install 
                            echo "Running tests..."
                            npm test
                            echo "Building docker image..."
                            docker build -t ${DOCKERHUB_USERNAME}/${env.SERVICE_NAME}:${BUILD_NUMBER} . 
                            docker tag ${DOCKERHUB_USERNAME}/${env.SERVICE_NAME}:${BUILD_NUMBER} ${DOCKERHUB_USERNAME}/${env.SERVICE_NAME}:latest
                            echo "YES Docker image built successfully"
                        """
                    }
                }
            }
            stage('Push to Registry'){
                steps{
                    script{
                        echo "Pushing ${env.SERVICE_NAME} to DockerHub..."
                        sh """
                            echo "${DOCKERHUB_CREDENTIALS_PSW}" | docker login -u "${DOCKERHUB_CREDENTIALS_USR}" --password-stdin
                            docker push ${DOCKERHUB_USERNAME}/${env.SERVICE_NAME}:${BUILD_NUMBER}
                            docker push ${DOCKERHUB_USERNAME}/${env.SERVICE_NAME}:latest
                            echo "YES ${env.SERVICE_NAME} pushed to DockerHub"
                        """
                    }
                }
            }
        }
        post {
            always {
                sh '''
                    docker logout || true
                    docker image prune -f || true
                '''
            }
            success {
                echo "YES Pipeline completed successfully for ${env.SERVICE_NAME}!"
            }
            failure {
                echo "NOO Pipeline failed for ${env.SERVICE_NAME}"
            }
        }
    }
}

//setup kube, deploy to kube, verify deployment
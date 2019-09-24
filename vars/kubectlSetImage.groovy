def call(def params = [:]) {
    def timeoutSeconds = "${params.get('timeout','60')}"
    try {
        timeout(time: "${timeoutSeconds}", unit: 'SECONDS') {
            withKubeConfig(credentialsId: "${params.credentialsId}", contextName: "${params.contextName}") {
                sh "kubectl set image deployment/${params.deploymentName} ${params.containerName}=${params.imageName}:${params.imageTag}"
                sh "kubectl rollout status deployment.v1.apps/${params.deploymentName}"
            }
        }
    }
    catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException err) {
        echo "Kubectl rollout has timed out."
        currentBuild.result = 'FAILURE'
        withKubeConfig(credentialsId: "${params.credentialsId}", contextName: "${params.contextName}") {
            sh "kubectl rollout undo deployment/${params.deploymentName}"
        }
    }
}
#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2023-03'

library "knime-pipeline@$BN"

properties([
    // provide a list of upstream jobs which should trigger a rebuild of this job
    pipelineTriggers([
        upstream("knime-tp/${BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    node('maven && java11 && large') {
        knimetools.defaultTychoBuild(updateSiteProject: 'org.knime.update.core.ui')
        
        junit '**/coverage/junit.xml'
        knimetools.processAuditResults()
        
        stage('Sonarqube analysis') {
            withCredentials([usernamePassword(credentialsId: 'ARTIFACTORY_CREDENTIALS', passwordVariable: 'ARTIFACTORY_PASSWORD', usernameVariable: 'ARTIFACTORY_LOGIN')]) {
                withSonarQubeEnv('Sonarcloud') {
                    withMaven(options: [artifactsPublisher(disabled: true)]) {
                        def sonarArgs = knimetools.getSonarArgsForMaven(env.SONAR_CONFIG_NAME)
                        sh """
                            mvn -Dknime.p2.repo=${P2_REPO} package $sonarArgs -DskipTests=true
                        """
                    }
                }
            }
        }
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}

/* vim: set shiftwidth=4 expandtab smarttab: */

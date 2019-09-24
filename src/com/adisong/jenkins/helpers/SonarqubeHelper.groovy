package com.adisong.jenkins.helpers

@Grab('org.apache.commons:commons-lang3:3.7')
import hudson.plugins.sonar.SonarInstallation
import org.apache.commons.lang3.StringUtils

import javax.annotation.Nullable
import java.security.InvalidParameterException

class SonarqubeHelper {
    private final script
    private SonarInstallation sonarInstallation

    SonarqubeHelper(script, String sonarqubeEnv){
        this.script = script
        def si = SonarInstallation.get(sonarqubeEnv)
        if (si == null) {
            throw new InvalidParameterException("Specified Sonarqube installation does not exist")
        }
        sonarInstallation = si
    }

    private void envWrapper(Closure body){
        def host = sonarInstallation.getServerUrl()
        def token = sonarInstallation.getServerAuthenticationToken().getPlainText()

        script.withEnv(["SONAR_HOST=${host}", "SONAR_AUTH_TOKEN=${token}"]) {
            body.call()
        }
    }

    void CreateProject(String projectKey, String name, @Nullable String branch = null, @Nullable String visibility = null){
        def mandatoryFlags = [
                "--project=\"${projectKey}\"",
                "--name=\"${name}\""
        ]
        def optionalFlags = []

        if (!StringUtils.isBlank(branch)) optionalFlags.add("--branch=\"${branch}\"")
        if (!StringUtils.isBlank(visibility)) optionalFlags.add("--visibility=\"${visibility}\"")
        envWrapper {
            script.sh(returnStdout: true, script: "sonar-cli projects create ${mandatoryFlags.join(' ')} ${optionalFlags.join(' ')}")
        }
    }

    void DeleteProject(String projectKey) {
        def mandatoryFlags = [
                "--project=\"${projectKey}\"",
        ]
        envWrapper {
            script.sh(returnStdout: true, script: "sonar-cli projects delete ${mandatoryFlags.join(' ')}")
        }
    }

    void SetQualityProfile(String projectKey, String language, String qualityProfile){
        def mandatoryFlags = [
                "--project=\"${projectKey}\"",
                "--language=\"${language}\"",
                "--quality-profile=\"${qualityProfile}\""
        ]
        envWrapper {
            script.sh(returnStdout: true, script: "sonar-cli qualityprofiles add-project ${mandatoryFlags.join(' ')}")
        }
    }

}

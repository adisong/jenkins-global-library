package com.adisong.jenkins.helpers

import com.cloudbees.groovy.cps.NonCPS
import org.apache.commons.lang3.StringUtils

/**
 * Helper class for git repositories
 */
class GitHelper {
    private script
    private URL url
    private String credentialsId

    private static final String releaseAndHotfixBranchRegex = /(release|hotfix)\/(\d+\.\d+\.\d+)/
    private static final String rcTagRegex = /\d+\.\d+\.\d+-RC\.(\d+)/
    private static final String semVerRegex = /\d+\.\d+\.\d+/

    GitHelper(def script, String url, String credentialsId){
        this.script = script
        this.url = new URL(url)
        this.credentialsId = credentialsId
    }

    /**
     * Checkouts repository on given revision and downloads submodules (tag or branch)
     * @param scmRev Revision to checkout
     */
    void CheckoutWithSubmodules(String scmRev){
        script.checkout poll: false, scm: [
                $class: 'GitSCM',
                branches: [[name: "$scmRev"]],
                doGenerateSubmoduleConfigurations: false,
                extensions:
                        [
                                [
                                        $class: 'SubmoduleOption',
                                        disableSubmodules: false,
                                        parentCredentials: true,
                                        recursiveSubmodules: true,
                                        reference: '',
                                        trackingSubmodules: true
                                ],
                                [
                                        $class: 'WipeWorkspace'
                                ]
                        ],
                submoduleCfg: [],
                userRemoteConfigs:
                        [
                                [
                                        credentialsId: "$credentialsId",
                                        url: "${url.toString()}"
                                ]
                        ]
        ]
    }

    /**
     * Checkout repo and perform pre build merge to target branch (FastForward)
     * @param sourceBranch Source branch
     * @param targetBranch Target branch
     */
    void CheckoutWithPreBuildMerge(String sourceBranch, String targetBranch){
        script.checkout changelog: true, poll: true, scm: [
                $class                           : 'GitSCM',
                branches                         : [
                        [
                                name: "origin/$sourceBranch"
                        ]
                ],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [
                        [
                                $class: 'PreBuildMerge',
                                options: [
                                        fastForwardMode: 'FF',
                                        mergeRemote: 'origin',
                                        mergeStrategy: 'DEFAULT',
                                        mergeTarget: "$targetBranch"
                                ]
                        ]
                ],
                submoduleCfg                     : [],
                userRemoteConfigs                : [
                        [
                                credentialsId: "$credentialsId",
                                name: 'origin',
                                url: "${url.toString()}"
                        ]
                ]
        ]
    }

    /**
     * Checks if given revision is repository tag
     * @param scmRev Revision to check
     * @return True when given revision is tag
     */
    boolean isTag(String scmRev){
        if (scmRev == script.sh(returnStdout: true, script: "git tag -l $scmRev").trim()){
            return true
        }
        return false
    }

    /**
     * Gets HEAD SHA shortcut
     * @return Repository HEAD SHA shortcut
     */
    String getShortSha(){
        return script.sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
    }

    /**
     * Removes remote name eg. origin from branch name
     * @param scmRev Revision specifier
     * @return Branch name without remote specifier
     */
    String getBranchName(String scmRev){
        String remoteName = script.sh(returnStdout: true, script: "git remote").trim()
        return StringUtils.replace(scmRev,"$remoteName/","")
    }

    /**
     * Gets latest RC tag for specified version
     * tags are created with following format
     * {semver}-RC.{rcVersion}
     * @param versionPrefix Semver version specification e.g. 0.1.0 must have major, minor and patch portion
     * @return RC tag with highest rcVersion number or null if tag does not exist
     */
    String getLatestRCTag(String versionPrefix){
        if (versionPrefix ==~ semVerRegex ) {
            def tag = script.sh(returnStdout: true, script: "git tag --list --sort v:refname \"${versionPrefix}-RC.*\" | tail -n 1").trim()
            if (tag != ''){
                return tag
            } else {
                return null
            }
        } else {
            throw new IllegalArgumentException("versionPrefix: ${versionPrefix} does not match ${semVerRegex}")
        }  
    }

    /**
     * Publishes next Release Candidate tag on current branch.
     * Only valid when on release or hotfix branch.
     * Will get version specified in branch name and calculate next rcVersion based on previous tags.
     */
    void publishNextRCTag(){
        String branch = script.sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
        if ( branch ==~ releaseAndHotfixBranchRegex ) {
            String version = getVersionFromBranch(branch)
            String latestRCTag = getLatestRCTag(version)
            int latestRCNumber = latestRCTag == null ? 0 : getRCNumberFromRCTagName(latestRCTag) // 0 means no RC tag exists yet for specified version
            String newTag = "${version}-RC.${++latestRCNumber}"
            script.sh(returnStdout: true, script: "git tag ${newTag}")
            pushRev("${newTag}")
        } else {
            throw new IllegalStateException("current branch: ${branch} does not match ${releaseAndHotfixBranchRegex}")
        }
    }

    /**
     * Extracts version from release/hotfix branch name.
     * Those branches have format (release|hotfix)/{version}
     * @param branch Branch name - must be either hotfix or release branch
     * @return Semantic version
     */
    @NonCPS
    String getVersionFromBranch(String branch){
        if ( branch ==~ releaseAndHotfixBranchRegex ) {
            def matcher = branch =~ releaseAndHotfixBranchRegex
            return matcher[0][2]
        } else {
            throw new IllegalArgumentException("branch: ${branch} does not match ${releaseAndHotfixBranchRegex}")
        }
    }

    /**
     * Extracts RC number from RC tag name
     * @param rcTag RC tag name
     * @return RC number
     */
    @NonCPS
    int getRCNumberFromRCTagName(String rcTag){
        if (rcTag ==~ rcTagRegex) {
            def rcNumberMatch = rcTag =~ rcTagRegex
            return rcNumberMatch[0][1] as Integer
        } else {
            throw new IllegalArgumentException("rcTag: ${rcTag} does not match ${rcTagRegex}")
        }

    }

    /**
     * Pushes specified revision to origin
     * @param revName Revision name (can be branch or tag name)
     */
    void pushRev(String revName) {
        script.withCredentials([script.usernamePassword(credentialsId: credentialsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
            script.sh(returnStdout: true, script: "git push ${url.getProtocol()}://\$GIT_USERNAME:\$GIT_PASSWORD@${url.getHost()}${url.getPath()} ${revName}")
        }
    }

    /**
     * Gets commits diff between source and target branch of merge request
     * @param sourceBranch Branch with new commits added by MR eg. feature or hotfix branch
     * @param targetBranch Branch MR will be merged to eg. develop, master
     * @return Comma separated list of commits SHAs
     */
    String getMergeRequestCommitsDiff(String sourceBranch, String targetBranch){
        return script.sh(returnStdout: true, script: "git log --pretty=format:%H ${targetBranch}..${sourceBranch} | tr '\n' ','").trim()
    }

}


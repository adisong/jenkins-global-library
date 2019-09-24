package com.adisong.jenkins.helpers

import spock.lang.Specification

class GitHelperTest extends Specification {
    def "Branches with valid names return valid versions with getVersionFromBranch"(String branchName, String version) {
        given:
            def gitHelper = new GitHelper(null,"https://github.com/username/repo.git","mycredentials")

        expect:
            gitHelper.getVersionFromBranch(branchName) == version

        where:
            branchName      | version
            "release/0.1.0" | "0.1.0"
            "hotfix/0.1.1"  | "0.1.1"
    }

    def "Branches with invalid names throws IllegalArgumentException with getVersionFromBranch"(String branchName) {
        given:
            def gitHelper = new GitHelper(null,"https://github.com/username/repo.git","mycredentials")

        when:
            gitHelper.getVersionFromBranch(branchName)

        then:
            thrown IllegalArgumentException

        where:
            branchName << [ "release/0.1", "hotfix/0.1.1-stable", null, "", "release/0-1-0"]
    }

    def "RC tags with valid names returns valid RC tag number with getRCNumberFromRCTagName"(String rcTag, int rcTagNumber) {
        given:
            def gitHelper = new GitHelper(null,"https://github.com/username/repo.git","mycredentials")

        expect:
            gitHelper.getRCNumberFromRCTagName(rcTag) == rcTagNumber

        where:
            rcTag           | rcTagNumber
            "0.1.0-RC.1"    | 1
            "0.1.0-RC.10"   | 10
    }

    def "RC tags with invalid names throws IllegalArgumentException with getRCNumberFromRCTagName"(String rcTag) {
        given:
            def gitHelper = new GitHelper(null,"https://github.com/username/repo.git","mycredentials")

        when:
            gitHelper.getRCNumberFromRCTagName(rcTag)

        then:
            thrown IllegalArgumentException

        where:
            rcTag << ["0.1.0-rc.1","0.1.0.RC.1","0.1-RC.1",null,"","0.1-0-RC.1"]
    }
}

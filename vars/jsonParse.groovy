import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurperClassic

@NonCPS
def call(String json) {
    new JsonSlurperClassic().parseText(json)
}
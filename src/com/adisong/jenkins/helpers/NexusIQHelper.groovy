package com.adisong.jenkins.helpers
import com.sonatype.nexus.api.iq.ApplicationPolicyEvaluation

class NexusIQHelper {
    NexusIQHelper(){}

    /**
     * prepareMRComment prepares comment to put in merge request based on NexusIQ policy evaluation result
     * This method is needed to avoid adding getter methods to Jenkins script security approvals
     * @param evaluationResult NexusIQ policy evaluation result returned by nexusPolicyEvaluation step
     * @return Comment string in markdown
     */
    String prepareMRComment(ApplicationPolicyEvaluation evaluationResult){
        return """NexusIQ have identified ${evaluationResult.getAffectedComponentCount()} components with policy violations:

  * Components in **critical** state: ${evaluationResult.getCriticalComponentCount()}
  * Components in **severe** state: ${evaluationResult.getSevereComponentCount()} 
  * Components in moderate state: ${evaluationResult.getModerateComponentCount()}
  * Grandfathered violations: ${evaluationResult.getGrandfatheredPolicyViolationCount()}

Report defails are available under: ${evaluationResult.getApplicationCompositionReportUrl()}
"""
    }
}

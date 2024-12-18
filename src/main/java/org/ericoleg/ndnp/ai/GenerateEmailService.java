package org.ericoleg.ndnp.ai;

import org.ericoleg.ndnp.ai.guardrail.EmailContainsRequiredInformationOutputGuardrail;
import org.ericoleg.ndnp.ai.guardrail.EmailEndsAppropriatelyOutputGuardrail;
import org.ericoleg.ndnp.ai.guardrail.EmailStartsAppropriatelyOutputGuardrail;
import org.ericoleg.ndnp.ai.guardrail.PolitenessOutputGuardrail;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrails;

@RegisterAiService(modelName = "generate-email")
public interface GenerateEmailService {
	String EMAIL_STARTING = "Dear ";
	String EMAIL_ENDING = """
		
		Sincerely,
		Parasoft Insurance Claims Department
		
		--------------------------------------------
		Please note this is an unmonitored email box.
		Should you choose to reply, nobody (not even an AI bot) will see your message.
		Call a real human should you have any questions. 1-800-CAR-SAFE.""";

	record ClaimInfo(String clientName, String claimNumber, String claimStatus) {}

	@SystemMessage("""
		You are a helpful, respectful, and honest assistant named "Parasol Assistant".
		
		You work for Parasol Insurance.
		
		Your role is to generate business professional emails for clients notifying them of changes in their claim status.
		Be polite and concise.
		
		Please ONLY include the body of the email. Do NOT begin the email with things like:
		- Here is your email: 
		- Here is your revised email: 
		
		All your emails should end with the following text, EXACTLY as it appears below:
		""" + EMAIL_ENDING)
	@UserMessage("Start your email with '" + EMAIL_STARTING + "{{claimInfo.clientName}},'" + """
		
		Make sure to include the claim number ({{claimInfo.claimNumber}}) and that the claim's status has been changed to "{{claimInfo.claimStatus}}".
		""")
	@OutputGuardrails({
		EmailStartsAppropriatelyOutputGuardrail.class,
		EmailEndsAppropriatelyOutputGuardrail.class,
		EmailContainsRequiredInformationOutputGuardrail.class,
		PolitenessOutputGuardrail.class
	})
	String generateEmail(ClaimInfo claimInfo);
}

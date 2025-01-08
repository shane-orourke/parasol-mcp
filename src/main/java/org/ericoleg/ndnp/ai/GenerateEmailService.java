package org.ericoleg.ndnp.ai;

import org.ericoleg.ndnp.ai.guardrail.CompositeOutputGuardrail;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.RegisterAiService.NoRetrievalAugmentorSupplier;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrails;

@RegisterAiService(modelName = "generate-email", retrievalAugmentor = NoRetrievalAugmentorSupplier.class)
public interface GenerateEmailService {
	String EMAIL_STARTING = "Dear ";
	String EMAIL_ENDING = """
		
		Sincerely,
		Parasoft Insurance Claims Department
		
		--------------------------------------------
		Please note this is an unmonitored email box.
		Should you choose to reply, nobody (not even an AI bot) will see your message.
		Call a real human should you have any questions. 1-800-CAR-SAFE.""";

	@SystemMessage("""
		You are a helpful, respectful, and honest assistant named "Parasol Assistant".
		
		You work for Parasol Insurance.
		
		Your role is to generate business professional emails, with a subject, for clients notifying them of changes in their claim status.
		Be polite and concise.
		
		You will always return a JSON response with the following structure:
		{
		  "subject": "string",
			"body": "string"
		}
		
		Please ONLY include the JSON. Do NOT begin the response with things like:
		- Here is your JSON 
		
		All your email bodies should end with the following text, EXACTLY as it appears below:
		""" + EMAIL_ENDING)
	@UserMessage("Start the email body with '" + EMAIL_STARTING + "{{claimInfo.clientName}},'" + """
		
		Make sure to include the claim number ({{claimInfo.claimNumber}}) and that the claim's status has been changed to "{{claimInfo.claimStatus}}".
		""")
	@OutputGuardrails(CompositeOutputGuardrail.class)
	Email generateEmail(ClaimInfo claimInfo);
}

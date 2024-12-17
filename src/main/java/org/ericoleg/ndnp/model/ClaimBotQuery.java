package org.ericoleg.ndnp.model;

import java.time.LocalDate;

public record ClaimBotQuery(long claimId, String claim, String query, LocalDate inceptionDate) {

}

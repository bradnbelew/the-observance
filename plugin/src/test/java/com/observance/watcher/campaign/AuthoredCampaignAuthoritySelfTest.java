package com.observance.watcher.campaign;

/** Executable parity test for the packaged, open-ended P5-P12 campaign projection. */
public final class AuthoredCampaignAuthoritySelfTest {

    private AuthoredCampaignAuthoritySelfTest() { }

    public static void main(String[] args) {
        AuthoredCampaignAuthority.Report report = AuthoredCampaignAuthority.inspect();
        require(report.valid(), "campaign authority issues: " + report.issues());
        require(report.caseCount() == 8, "expected 8 P5-P12 cases");
        require(report.conclusionCount() >= 20, "expected sustained conclusion contracts");
        require(report.evidenceCount() >= 75, "expected campaign-scale authored evidence");
        require(report.spaceCount() >= 20, "expected functional Minecraft compositions");
        require(report.contentHash().matches("[0-9a-f]{64}"), "authority hash is not SHA-256");
        System.out.println("AuthoredCampaignAuthoritySelfTest OK - " + report.caseCount()
                + " cases, " + report.conclusionCount() + " conclusions, "
                + report.evidenceCount() + " evidence records, " + report.spaceCount()
                + " spaces, hash=" + report.contentHash());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

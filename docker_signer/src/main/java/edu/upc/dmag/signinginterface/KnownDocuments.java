package edu.upc.dmag.signinginterface;

import lombok.Getter;

@Getter
public enum KnownDocuments {
    CEA_FILE(IDs.id_CEA_FILE,"CEA model","Include CEA model?", ""),
    CR_MODEL(IDs.id_CR_MODEL, "Communication Recommender's model","Include Communication Recommender's model?", "for new analysis in the future"),
    CR_RESULT(IDs.id_CR_RESULT, "Communication Recommender's result","Include current Communication Recommender's result?", "to compare with future results"),
    TVRA_MODEL(IDs.id_TVRA_MODEL,"TVRA component's model","Include TVRA component's model?", "for new analysis in the future"),
    TVRA_ATTACK_PATHS(IDs.id_TVRA_ATTACK_PATHS, "TVRA output's attack paths","Include current TVRA output's attack paths?", "to compare with future results"),
    TVRA_RECOMMENDATIONS(IDs.id_TVRA_RECOMMENDATIONS,"TVRA output's recommendations","Include current TVRA output's recommendations?", "to compare with future results"),
    TVRA_THREATS(IDs.id_TVRA_THREATS,"TVRA output's identified threats","Include current TVRA output's identified threats?", "to compare with future results"),
    TVRA_REPORT(IDs.id_TVRA_REPORT,"TVRA output's report","Include current TVRA output's report?", "to compare with future results"),
    RISK_MANAGEMENT_FILE(IDs.id_RISK_MANAGEMENT_FILE, "risk management plan","Include the risk management plan?", "to assure compliance with ISO 14971, IEC 80001-1 and MDCG"),
    SECURITY_CASE(IDs.id_SECURITY_CASE,"security case","Include Security case?", "to assure compliance with IEC 80001-2"),
    MEDICAL_IT_NETWORK_RISK_MANAGEMENT_FILE(IDs.id_MEDICAL_IT_NETWORK_RISK_MANAGEMENT_FILE, "Medical IT-Network risk management document","Include the Medical IT-Network risk management document?", "to assure compliance with IEC 80001-1"),
    ASSURANCE_CASE_REPORT(IDs.id_ASSURANCE_CASE_REPORT,"assurance case report","Include the assurance case report?", "to assure compliance with IEC 80001-1"),
    INSTRUCTIONS_OF_USE(IDs.id_INSTRUCTIONS_OF_USE, "Instructions of Use","Include the Instructions of Use?", "to assure compliance with MDCG and IEC 82304"),
    VALIDATION_REPORT(IDs.id_VALIDATION_REPORT, "validation report","Include the validation report?", "to assure compliance with IEC 82304"),
    TECHNICAL_DESCRIPTION(IDs.id_TECHNICAL_DESCRIPTION, "technical description","Include the technical description?", "to assure compliance with IEC 82304"),
    TREND_REPORT(IDs.id_TREND_REPORT, "trend report","Include the trend report?", "to assure compliance with MDCG");

    public static class IDs {
        static final String id_CEA_FILE = "ceafile";
        static final String id_CR_MODEL = "CR_model";
        static final String id_CR_RESULT = "CR_result";
        static final String id_TVRA_MODEL = "TVRA_model";
        static final String id_TVRA_ATTACK_PATHS = "TVRA_AttackPaths";
        static final String id_TVRA_RECOMMENDATIONS = "TVRA_Recommendations";
        static final String id_TVRA_THREATS = "TVRA_Threats";
        static final String id_TVRA_REPORT = "TVRA_Report";
        static final String id_RISK_MANAGEMENT_FILE = "Documentation_riskManagementPlan";
        static final String id_SECURITY_CASE = "GeneralContent_securityCase";
        static final String id_MEDICAL_IT_NETWORK_RISK_MANAGEMENT_FILE = "Documentation_medicalItNetworkRiskManagementFile";
        static final String id_ASSURANCE_CASE_REPORT = "Documentation_assuranceCaseReport";
        static final String id_INSTRUCTIONS_OF_USE = "Documentation_instructionsOfUse";
        static final String id_VALIDATION_REPORT = "Documentation_validationReport";
        static final String id_TECHNICAL_DESCRIPTION = "Documentation_technicalDescription";
        static final String id_TREND_REPORT = "Documentation_TrendReport";
    }

    private final String id;
    private final String name;
    private final String caption;
    private final String goal;

    KnownDocuments(String id, String name, String caption, String goal) {
        this.id = id;
        this.name = name;
        this.caption = caption;
        this.goal = goal;
    }

    public String getDropZoneId() {
        return "drop-zone-" + getId();
    }

    public String getIconId() {
        return getId() + "_icon";
    }

    public String getFeedbackId() {
        return getId() + "_feedback";
    }

    public String getProgressId() {
        return getId() + "_progress";
    }

    public String getProgressContainerId() {
        return getId() + "_progress_container";
    }

}

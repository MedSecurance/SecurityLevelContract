package edu.upc.dmag.signinginterface;

import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum KnownDocuments {
    CEA_FILE("ceafile","CEA model","Include CEA model?", ""),
    CR_MODEL("CR_model", "Communication Recommender's model","Include Communication Recommender's model?", "for new analysis in the future"),
    CR_RESULT("CR_result", "Communication Recommender's result","Include current Communication Recommender's result?", "to compare with future results"),
    TVRA_MODEL("TVRA_model","TVRA component's model","Include TVRA component's model?", "for new analysis in the future"),
    TVRA_ATTACK_PATHS("TVRA_AttackPaths", "TVRA output's attack paths","Include current TVRA output's attack paths?", "to compare with future results"),
    TVRA_RECOMMENDATIONS("TVRA_Recommendations","TVRA output's recommendations","Include current TVRA output's recommendations?", "to compare with future results"),
    TVRA_THREATS("TVRA_Threats","TVRA output's identified threats","Include current TVRA output's identified threats?", "to compare with future results"),
    TVRA_REPORT("TVRA_Report","TVRA output's report","Include current TVRA output's report?", "to compare with future results"),
    RISK_MANAGEMENT_FILE("Documentation_riskManagementPlan", "risk management plan","Include the risk management plan?", "to assure compliance with ISO 14971, IEC 80001-1 and MDCG"),
    SECURITY_CASE("GeneralContent_securityCase","security case","Include Security case?", "to assure compliance with IEC 80001-2"),
    MEDICAL_IT_NETWORK_RISK_MANAGEMENT_FILE("Documentation_medicalItNetworkRiskManagementFile", "Medical IT-Network risk management document","Include the Medical IT-Network risk management document?", "to assure compliance with IEC 80001-1"),
    ASSURANCE_CASE_REPORT("Documentation_assuranceCaseReport","assurance case report","Include the assurance case report?", "to assure compliance with IEC 80001-1"),
    INSTRUCTIONS_OF_USE("Documentation_instructionsOfUse", "Instructions of Use","Include the Instructions of Use?", "to assure compliance with MDCG and IEC 82304"),
    VALIDATION_REPORT("Documentation_validationReport", "validation report","Include the validation report?", "to assure compliance with IEC 82304"),
    TECHNICAL_DESCRIPTION( "Documentation_technicalDescription", "technical description","Include the technical description?", "to assure compliance with IEC 82304"),
    TREND_REPORT("Documentation_TrendReport", "trend report","Include the trend report?", "to assure compliance with MDCG");

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

    public String getId() {
        return id;
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

    public String getCaption() {
        return caption;
    }

    public String getGoal() {
        return goal;
    }

    public String getName() {
        return name;
    }

    public static List<Map<String, String>> getMap(){
        List<Map<String, String>> result = new ArrayList<>();
        for(KnownDocuments doc : KnownDocuments.values()){
            Map<String, String> entry = new HashMap<>();
            entry.put("dropZone_name", doc.getDropZoneId());
            entry.put("fileInput_name", doc.getId());
            entry.put("fileInfo_name", doc.getFeedbackId());
            entry.put("icon", doc.getIconId());
            result.add(entry);
        }
        return result;
    }
}

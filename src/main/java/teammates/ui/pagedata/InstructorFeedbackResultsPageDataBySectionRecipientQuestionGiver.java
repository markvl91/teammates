package teammates.ui.pagedata;

import teammates.common.datatransfer.attributes.*;
import teammates.common.util.*;
import teammates.ui.datatransfer.InstructorFeedbackResultsPageViewType;
import teammates.ui.template.ElementTag;
import teammates.ui.template.InstructorFeedbackResultsResponseRow;
import teammates.ui.template.InstructorFeedbackResultsSectionPanel;

import java.util.*;

public class InstructorFeedbackResultsPageDataBySectionRecipientQuestionGiver
        extends InstructorFeedbackResultsPageDataBySection {

    public InstructorFeedbackResultsPageDataBySectionRecipientQuestionGiver(
            AccountAttributes account, String sessionToken) {
        super(account, sessionToken);

        viewType = InstructorFeedbackResultsPageViewType.RECIPIENT_QUESTION_GIVER;
        sortType = viewType.toString();
    }

    protected void initializeForSectionViewType(String selectedSection) {
        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> sortedResponsesForRqg =
                bundle.getResponsesSortedByRecipientQuestionGiver(true);

        buildSectionPanelForViewByParticipantQuestionParticipant(selectedSection,
                sortedResponsesForRqg, viewType.additionalInfoId());
    }

    protected void prepareHeadersForTeamPanelsInSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel) {
        sectionPanel.setStatisticsHeaderText("Statistics for Received Responses");
        sectionPanel.setDetailedResponsesHeaderText("Detailed Responses");
    }

    protected void configureResponseRow(
            String giver, String recipient,
            InstructorFeedbackResultsResponseRow responseRow) {
        responseRow.setRecipientDisplayed(false);
        responseRow.setGiverProfilePictureAColumn(true);

        responseRow.setGiverProfilePictureLink(getProfilePictureIfEmailValid(giver));
        responseRow.setActionsDisplayed(true);
    }

    protected List<InstructorFeedbackResultsResponseRow> getResponseRows(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses,
            String participantIdentifier, List<ElementTag> columnTags, Map<String, Boolean> isSortable) {
        List<InstructorFeedbackResultsResponseRow> responseRows;

        buildTableColumnHeaderForRecipientQuestionGiverView(columnTags, isSortable);
        responseRows =
                buildResponseRowsForQuestionForSingleRecipient(question, responses, participantIdentifier);

        return responseRows;
    }

    private void buildTableColumnHeaderForRecipientQuestionGiverView(
            List<ElementTag> columnTags,
            Map<String, Boolean> isSortable) {
        ElementTag photoElement = new ElementTag("Photo");
        ElementTag giverTeamElement =
                new ElementTag("Team", "id", "button_sortFromTeam", "class", "button-sort-ascending toggle-sort",
                        "style", "width: 15%; min-width: 67px;");
        ElementTag giverElement =
                new ElementTag("Giver", "id", "button_sortFromName", "class", "button-sort-none toggle-sort",
                        "style", "width: 15%; min-width: 65px;");
        ElementTag responseElement =
                new ElementTag("Feedback", "id", "button_sortFeedback", "class", "button-sort-none toggle-sort",
                        "style", "min-width: 95px;");
        ElementTag actionElement = new ElementTag("Actions", "class", "action-header");

        columnTags.add(photoElement);
        columnTags.add(giverTeamElement);
        columnTags.add(giverElement);
        columnTags.add(responseElement);
        columnTags.add(actionElement);

        isSortable.put(photoElement.getContent(), false);
        isSortable.put(giverTeamElement.getContent(), true);
        isSortable.put(giverElement.getContent(), true);
        isSortable.put(responseElement.getContent(), true);
        isSortable.put(actionElement.getContent(), false);
    }

    private List<InstructorFeedbackResultsResponseRow> buildResponseRowsForQuestionForSingleRecipient(
            FeedbackQuestionAttributes question,
            List<FeedbackResponseAttributes> responses,
            String recipientIdentifier) {
        return buildResponseRowsForQuestionForSingleParticipant(question, responses, recipientIdentifier, false);
    }

    protected void finalizeBuildingSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel, String sectionName,
            Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> responsesGroupedByTeam,
            Set<String> teamsWithResponses) {

        prepareHeadersForTeamPanelsInSectionPanel(sectionPanel);
        if (!responsesGroupedByTeam.isEmpty()) {
            buildTeamsStatisticsTableForSectionPanel(sectionPanel, responsesGroupedByTeam,
                    teamsWithResponses);
        }

        Map<String, Boolean> isTeamDisplayingStatistics = new HashMap<>();
        for (String team : teamsWithResponses) {
            // teamsWithResponses can include teams of anonymous student ("Anonymous student #'s Team")
            // and "-"
            isTeamDisplayingStatistics.put(team, isTeamVisible(team));
        }
        sectionPanel.setDisplayingTeamStatistics(isTeamDisplayingStatistics);
        sectionPanel.setSectionName(sectionName);
        sectionPanel.setSectionNameForDisplay(sectionName.equals(Const.DEFAULT_SECTION)
                ? Const.NO_SPECIFIC_SECTION
                : sectionName);
    }

    /**
     * Checks if there are entities in No Specific Section.
     *
     * <ul>
     * <li>true if the course has teams in Default Section</li>
     * <li>true if there is General Feedback or Feedback to Instructors</li>
     * <li>false otherwise</li>
     * </ul>
     */
    protected boolean hasEntitiesInNoSpecificSection() {
        boolean hasFeedbackToInstructorOrGeneral = bundle.hasResponseToInstructorOrGeneral();
        boolean hasTeamsInNoSection = bundle.getTeamsInSectionFromRoster(Const.DEFAULT_SECTION).size() > 0;

        return hasTeamsInNoSection || hasFeedbackToInstructorOrGeneral;
    }
}

package teammates.ui.pagedata;

import teammates.common.datatransfer.attributes.*;
import teammates.common.util.*;
import teammates.ui.datatransfer.InstructorFeedbackResultsPageViewType;
import teammates.ui.template.ElementTag;
import teammates.ui.template.InstructorFeedbackResultsResponseRow;
import teammates.ui.template.InstructorFeedbackResultsSectionPanel;

import java.util.*;

public class InstructorFeedbackResultsPageDataBySectionGiverQuestionRecipient
        extends InstructorFeedbackResultsPageDataBySection {

    public InstructorFeedbackResultsPageDataBySectionGiverQuestionRecipient(
            AccountAttributes account, String sessionToken) {
        super(account, sessionToken);

        viewType = InstructorFeedbackResultsPageViewType.GIVER_QUESTION_RECIPIENT;
        sortType = viewType.toString();
    }

    protected void initializeForSectionViewType(String selectedSection) {
        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> sortedResponsesForGqr =
                bundle.getResponsesSortedByGiverQuestionRecipient(true);

        buildSectionPanelForViewByParticipantQuestionParticipant(selectedSection,
                sortedResponsesForGqr, viewType.additionalInfoId());
    }

    protected void prepareHeadersForTeamPanelsInSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel) {
        sectionPanel.setStatisticsHeaderText("Statistics for Given Responses");
        sectionPanel.setDetailedResponsesHeaderText("Detailed Responses");
    }

    protected void configureResponseRow(
            String giver, String recipient,
            InstructorFeedbackResultsResponseRow responseRow) {

        responseRow.setGiverDisplayed(false);
        responseRow.setGiverProfilePictureLink(null);
        responseRow.setRecipientProfilePictureAColumn(true);

        responseRow.setRecipientProfilePictureLink(getProfilePictureIfEmailValid(recipient));
        responseRow.setActionsDisplayed(false);
    }

    protected List<InstructorFeedbackResultsResponseRow> getResponseRows(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses,
            String participantIdentifier, List<ElementTag> columnTags, Map<String, Boolean> isSortable) {
        List<InstructorFeedbackResultsResponseRow> responseRows;

        buildTableColumnHeaderForGiverQuestionRecipientView(columnTags, isSortable);
        responseRows =
                buildResponseRowsForQuestionForSingleGiver(question, responses, participantIdentifier);

        return responseRows;
    }

    private void buildTableColumnHeaderForGiverQuestionRecipientView(
            List<ElementTag> columnTags,
            Map<String, Boolean> isSortable) {
        ElementTag photoElement = new ElementTag("Photo");
        ElementTag recipientTeamElement =
                new ElementTag("Team", "id", "button_sortFromTeam", "class", "button-sort-ascending toggle-sort",
                        "style", "width: 15%; min-width: 67px;");
        ElementTag recipientElement =
                new ElementTag("Recipient", "id", "button_sortTo", "class", "button-sort-none toggle-sort",
                        "style", "width: 15%; min-width: 90px;");
        ElementTag responseElement =
                new ElementTag("Feedback", "id", "button_sortFeedback", "class", "button-sort-none toggle-sort",
                        "style", "min-width: 95px;");

        columnTags.add(photoElement);
        columnTags.add(recipientTeamElement);
        columnTags.add(recipientElement);
        columnTags.add(responseElement);

        isSortable.put(photoElement.getContent(), false);
        isSortable.put(recipientTeamElement.getContent(), true);
        isSortable.put(recipientElement.getContent(), true);
        isSortable.put(responseElement.getContent(), true);
    }

    private List<InstructorFeedbackResultsResponseRow>
    buildResponseRowsForQuestionForSingleGiver(
            FeedbackQuestionAttributes question,
            List<FeedbackResponseAttributes> responses,
            String giverIdentifier) {
        return buildResponseRowsForQuestionForSingleParticipant(question, responses, giverIdentifier, true);
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
     * <li>true if there is feedback from Instructors</li>
     * <li>false otherwise</li>
     * </ul>
     */
    protected boolean hasEntitiesInNoSpecificSection() {
        boolean hasFeedbackFromInstructor = bundle.hasResponseFromInstructor();
        boolean hasTeamsInNoSection = bundle.getTeamsInSectionFromRoster(Const.DEFAULT_SECTION).size() > 0;

        return hasTeamsInNoSection || hasFeedbackFromInstructor;
    }
}

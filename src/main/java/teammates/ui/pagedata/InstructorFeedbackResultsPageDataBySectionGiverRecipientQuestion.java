package teammates.ui.pagedata;

import teammates.common.datatransfer.attributes.*;
import teammates.common.util.*;
import teammates.ui.datatransfer.InstructorFeedbackResultsPageViewType;
import teammates.ui.template.ElementTag;
import teammates.ui.template.InstructorFeedbackResultsResponseRow;
import teammates.ui.template.InstructorFeedbackResultsSectionPanel;

import java.util.*;

public class InstructorFeedbackResultsPageDataBySectionGiverRecipientQuestion
        extends InstructorFeedbackResultsPageDataBySection {

    public InstructorFeedbackResultsPageDataBySectionGiverRecipientQuestion(
            AccountAttributes account, String sessionToken) {
        super(account, sessionToken);

        viewType = InstructorFeedbackResultsPageViewType.GIVER_RECIPIENT_QUESTION;
        sortType = viewType.toString();
    }

    protected void initializeForSectionViewType(String selectedSection) {
        Map<String, Map<String, List<FeedbackResponseAttributes>>> sortedResponsesForGrq =
                bundle.getResponsesSortedByGiverRecipientQuestion(true);

        buildSectionPanelForViewByParticipantParticipantQuestion(selectedSection,
                sortedResponsesForGrq, viewType.additionalInfoId());
    }

    protected void prepareHeadersForTeamPanelsInSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel) {
        Assumption.fail("There should be no headers for the view type");
    }

    protected void configureResponseRow(
            String giver, String recipient,
            InstructorFeedbackResultsResponseRow responseRow) {
        Assumption.fail();
    }

    protected List<InstructorFeedbackResultsResponseRow> getResponseRows(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses,
            String participantIdentifier, List<ElementTag> columnTags, Map<String, Boolean> isSortable) {

        Assumption.fail("View type should not involve question tables");

        return new ArrayList<>();
    }

    protected void finalizeBuildingSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel, String sectionName,
            Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> responsesGroupedByTeam,
            Set<String> teamsWithResponses) {

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

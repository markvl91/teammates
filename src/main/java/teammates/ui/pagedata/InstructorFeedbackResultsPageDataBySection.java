package teammates.ui.pagedata;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.*;
import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.util.*;
import teammates.ui.datatransfer.InstructorFeedbackResultsPageViewType;
import teammates.ui.template.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;

public abstract class InstructorFeedbackResultsPageDataBySection extends InstructorFeedbackResultsPageDataByQuestion {
    public InstructorFeedbackResultsPageDataBySection(AccountAttributes account, String sessionToken) {
        super(account, sessionToken);
    }

    /**
     * Creates {@code InstructorFeedbackResultsSectionPanel}s for sectionPanels.
     *
     * <p>Iterates through the responses and creates panels and questions for them. Keeps track
     * of missing sections, teams and participants who do not have responses
     * and create panels for these missing sections, teams and participants.
     * <p>
     * {@code bundle} should be set before this method
     * TODO: simplify the logic in this method
     */
    @Override
    public void initialize(
            InstructorAttributes instructor,
            String selectedSection, String showStats,
            String groupByTeam, InstructorFeedbackResultsPageViewType view,
            boolean isMissingResponsesShown) {
        initCommonVariables(instructor, selectedSection, showStats, groupByTeam,
                isMissingResponsesShown, view);

        // results page to be loaded by ajax
        if (isAllSectionsSelected()) {
            if (bundle.isComplete) {
                buildSectionPanelsForForAjaxLoading(getSections());
            } else {
                buildSectionPanelWithErrorMessage();
            }

            return;
        }

        // Note that if the page needs to load by ajax, then responses may be empty too,
        // therefore the check for ajax to come before this
        if (bundle.responses.isEmpty()) {
            // no responses, nothing to initialize
            return;
        }

        initializeForSectionViewType(selectedSection);

    }

    protected abstract void initializeForSectionViewType(String selectedSection);

    protected void buildSectionPanelForViewByParticipantParticipantQuestion(
            String section,
            Map<String, Map<String, List<FeedbackResponseAttributes>>> sortedResponses,
            String additionalInfoId) {
        sectionPanels = new LinkedHashMap<>();
        InstructorFeedbackResultsSectionPanel sectionPanel = new InstructorFeedbackResultsSectionPanel();

        String prevTeam = "";

        String sectionPrefix = String.format("section-%s-", getSectionPosition(section));

        Set<String> teamsWithResponses = new HashSet<>();
        Set<String> teamMembersWithResponses = new HashSet<>();

        // Iterate through the primary participant
        int primaryParticipantIndex = this.getStartIndex();
        for (Entry<String, Map<String, List<FeedbackResponseAttributes>>> primaryToSecondaryParticipantToResponsesMap
                : sortedResponses.entrySet()) {
            primaryParticipantIndex += 1;
            String primaryParticipantIdentifier = primaryToSecondaryParticipantToResponsesMap.getKey();

            String currentTeam = getCurrentTeam(bundle, primaryParticipantIdentifier);

            boolean isStudent = bundle.isParticipantIdentifierStudent(primaryParticipantIdentifier);
            String participantSection = bundle.getSectionFromRoster(primaryParticipantIdentifier);

            if (isStudent && !participantSection.equals(section)) {
                continue;
            }

            boolean isDifferentTeam = !prevTeam.equals(currentTeam);

            if (isDifferentTeam) {
                boolean isFirstTeam = prevTeam.isEmpty();
                if (!isFirstTeam) {
                    // construct missing participant panels for the previous team
                    buildMissingParticipantPanelsForTeam(
                            sectionPanel, prevTeam, teamMembersWithResponses);
                    teamMembersWithResponses.clear();
                }

                teamsWithResponses.add(currentTeam);
                sectionPanel.getIsTeamWithResponses().put(currentTeam, true);
            }

            // Build participant panel for the current primary participant
            InstructorFeedbackResultsParticipantPanel recipientPanel =
                    buildGroupByParticipantPanel(primaryParticipantIdentifier,
                            primaryToSecondaryParticipantToResponsesMap,
                            sectionPrefix + additionalInfoId, primaryParticipantIndex);

            sectionPanel.addParticipantPanel(currentTeam, recipientPanel);

            teamMembersWithResponses.add(primaryParticipantIdentifier);

            prevTeam = currentTeam;
        }

        // for the last section with responses
        buildMissingParticipantPanelsForTeam(sectionPanel, prevTeam, teamMembersWithResponses);

        teamsWithResponses.add(prevTeam);
        buildMissingTeamAndParticipantPanelsForSection(sectionPanel, section, teamsWithResponses);

        finalizeBuildingSectionPanelWithoutTeamStats(sectionPanel, section);
        sectionPanels.put(section, sectionPanel);
    }

    /**
     * Constructs section panel for the {@code sortedResponses}.
     *
     * <p>Also builds team statistics tables for every team.
     */
    protected void buildSectionPanelForViewByParticipantQuestionParticipant(
            String section,
            Map<String, Map<FeedbackQuestionAttributes,
                    List<FeedbackResponseAttributes>>> sortedResponses, String additionalInfoId) {
        sectionPanels = new LinkedHashMap<>();

        Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> responsesGroupedByTeam =
                viewType.isPrimaryGroupingOfGiverType() ? bundle.getQuestionResponseMapByGiverTeam()
                        : bundle.getQuestionResponseMapByRecipientTeam();

        String prevTeam = "";

        String sectionPrefix = String.format("section-%s-", getSectionPosition(section));

        Set<String> teamsWithResponses = new LinkedHashSet<>();
        Set<String> teamMembersWithResponses = new HashSet<>();

        InstructorFeedbackResultsSectionPanel sectionPanel = new InstructorFeedbackResultsSectionPanel();

        // Iterate through the primary participant
        int primaryParticipantIndex = this.getStartIndex();
        for (Entry<String,
                Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>>
                primaryToSecondaryParticipantToResponsesMap
                : sortedResponses.entrySet()) {
            primaryParticipantIndex += 1;
            String primaryParticipantIdentifier = primaryToSecondaryParticipantToResponsesMap.getKey();

            boolean isStudent = bundle.isParticipantIdentifierStudent(primaryParticipantIdentifier);
            String participantSection = bundle.getSectionFromRoster(primaryParticipantIdentifier);

            if (isStudent && !participantSection.equals(section)) {
                continue;
            }

            String currentTeam = getCurrentTeam(bundle, primaryParticipantIdentifier);

            boolean isDifferentTeam = !prevTeam.equals(currentTeam);

            if (isDifferentTeam) {
                boolean isFirstTeam = prevTeam.isEmpty();
                if (!isFirstTeam) {
                    // construct missing participant panels for the previous team
                    buildMissingParticipantPanelsForTeam(
                            sectionPanel, prevTeam, teamMembersWithResponses);
                    teamMembersWithResponses.clear();
                }

                teamsWithResponses.add(currentTeam);
                sectionPanel.getIsTeamWithResponses().put(currentTeam, true);
            }

            // Build participant panel for the current participant
            InstructorFeedbackResultsParticipantPanel primaryParticipantPanel =
                    buildGroupByQuestionPanel(primaryParticipantIdentifier,
                            primaryToSecondaryParticipantToResponsesMap,
                            sectionPrefix + additionalInfoId, primaryParticipantIndex);

            sectionPanel.addParticipantPanel(currentTeam, primaryParticipantPanel);

            teamMembersWithResponses.add(primaryParticipantIdentifier);

            prevTeam = currentTeam;
        }

        // for the last section with responses
        buildMissingParticipantPanelsForTeam(sectionPanel, prevTeam, teamMembersWithResponses);

        teamsWithResponses.add(prevTeam);
        buildMissingTeamAndParticipantPanelsForSection(sectionPanel, section, teamsWithResponses);

        finalizeBuildingSectionPanel(sectionPanel, section, responsesGroupedByTeam, teamsWithResponses);
        sectionPanels.put(section, sectionPanel);
    }

    private InstructorFeedbackResultsGroupByParticipantPanel buildGroupByParticipantPanel(
            String primaryParticipantIdentifier,
            Entry<String, Map<String, List<FeedbackResponseAttributes>>> recipientToGiverToResponsesMap,
            String additionalInfoId, int primaryParticipantIndex) {
        // first build secondary participant panels for the primary participant panel
        Map<String, List<FeedbackResponseAttributes>> giverToResponsesMap =
                recipientToGiverToResponsesMap.getValue();
        List<InstructorFeedbackResultsSecondaryParticipantPanelBody> secondaryParticipantPanels =
                buildSecondaryParticipantPanels(
                        additionalInfoId, primaryParticipantIndex, giverToResponsesMap);

        // construct the primary participant panel
        String primaryParticipantNameWithTeamName =
                bundle.appendTeamNameToName(bundle.getNameForEmail(primaryParticipantIdentifier),
                        bundle.getTeamNameForEmail(primaryParticipantIdentifier));

        InstructorFeedbackResultsModerationButton moderationButton;
        if (viewType.isPrimaryGroupingOfGiverType()) {
            moderationButton =
                    buildModerationButtonForGiver(null, primaryParticipantIdentifier,
                            "btn btn-primary btn-xs",
                            MODERATE_RESPONSES_FOR_GIVER);
        } else {
            moderationButton = null;
        }
        return buildInstructorFeedbackResultsGroupBySecondaryParticipantPanel(
                primaryParticipantIdentifier, primaryParticipantNameWithTeamName,
                secondaryParticipantPanels, moderationButton);
    }

    private List<InstructorFeedbackResultsSecondaryParticipantPanelBody> buildSecondaryParticipantPanels(
            String additionalInfoId, int primaryParticipantIndex,
            Map<String, List<FeedbackResponseAttributes>> secondaryParticipantToResponsesMap) {
        List<InstructorFeedbackResultsSecondaryParticipantPanelBody> secondaryParticipantPanels = new ArrayList<>();

        int secondaryParticipantIndex = 0;
        for (Entry<String, List<FeedbackResponseAttributes>> secondaryParticipantResponses
                : secondaryParticipantToResponsesMap.entrySet()) {
            secondaryParticipantIndex += 1;
            String secondaryParticipantIdentifier = secondaryParticipantResponses.getKey();

            boolean isEmail = validator.getInvalidityInfoForEmail(secondaryParticipantIdentifier).isEmpty();
            String secondaryParticipantDisplayableName;
            if (isEmail && !bundle.getTeamNameForEmail(secondaryParticipantIdentifier).isEmpty()) {
                secondaryParticipantDisplayableName =
                        bundle.getNameForEmail(secondaryParticipantIdentifier)
                                + " (" + bundle.getTeamNameForEmail(secondaryParticipantIdentifier) + ")";
            } else {
                secondaryParticipantDisplayableName = bundle.getNameForEmail(secondaryParticipantIdentifier);
            }
            List<InstructorFeedbackResultsResponsePanel> responsePanels =
                    buildResponsePanels(additionalInfoId, primaryParticipantIndex,
                            secondaryParticipantIndex, secondaryParticipantResponses.getValue());

            InstructorFeedbackResultsSecondaryParticipantPanelBody secondaryParticipantPanel =
                    new InstructorFeedbackResultsSecondaryParticipantPanelBody(
                            secondaryParticipantIdentifier, secondaryParticipantDisplayableName,
                            responsePanels);

            secondaryParticipantPanel
                    .setProfilePictureLink(getProfilePictureIfEmailValid(secondaryParticipantIdentifier));

            if (!viewType.isPrimaryGroupingOfGiverType()) {
                String sectionName = bundle.getSectionFromRoster(secondaryParticipantIdentifier);
                boolean isAllowedToModerate = isAllowedToModerate(instructor, sectionName, feedbackSessionName);

                secondaryParticipantPanel.setModerationButton(
                        isAllowedToModerate
                                ? buildModerationButtonForGiver(null, secondaryParticipantIdentifier,
                                "btn btn-default btn-xs",
                                MODERATE_RESPONSES_FOR_GIVER)
                                : null);
            }

            secondaryParticipantPanels.add(secondaryParticipantPanel);
        }

        return secondaryParticipantPanels;
    }

    private List<InstructorFeedbackResultsResponsePanel> buildResponsePanels(
            final String additionalInfoId,
            int primaryParticipantIndex, int secondaryRecipientIndex,
            List<FeedbackResponseAttributes> giverResponses) {
        List<InstructorFeedbackResultsResponsePanel> responsePanels = new ArrayList<>();

        for (int responseIndex = 0; responseIndex < giverResponses.size(); responseIndex++) {
            FeedbackResponseAttributes response = giverResponses.get(responseIndex);

            String questionId = response.feedbackQuestionId;
            FeedbackQuestionAttributes question = bundle.questions.get(questionId);
            String questionText = bundle.getQuestionText(questionId);

            int giverIndex = viewType.isPrimaryGroupingOfGiverType() ? primaryParticipantIndex
                    : secondaryRecipientIndex;
            int recipientIndex = viewType.isPrimaryGroupingOfGiverType() ? secondaryRecipientIndex
                    : primaryParticipantIndex;

            String additionalInfoText =
                    questionToDetailsMap.get(question).getQuestionAdditionalInfoHtml(
                            question.getQuestionNumber(), String.format(
                                    additionalInfoId, giverIndex, recipientIndex));
            String displayableResponse = bundle.getResponseAnswerHtml(response, question);

            String giverName = bundle.getNameForEmail(response.giver);
            String recipientName = bundle.getNameForEmail(response.recipient);

            String giverTeam = bundle.getTeamNameForEmail(response.giver);
            String recipientTeam = bundle.getTeamNameForEmail(response.recipient);

            giverName = bundle.appendTeamNameToName(giverName, giverTeam);
            recipientName = bundle.appendTeamNameToName(recipientName, recipientTeam);

            List<FeedbackResponseCommentRow> comments =
                    buildResponseComments(giverName, recipientName, question, response);
            boolean isAllowedToSubmitSessionsInBothSection =
                    instructor.isAllowedForPrivilege(response.giverSection,
                            response.feedbackSessionName,
                            Const.ParamsNames.INSTRUCTOR_PERMISSION_SUBMIT_SESSION_IN_SECTIONS)
                            && instructor.isAllowedForPrivilege(response.recipientSection,
                            response.feedbackSessionName,
                            Const.ParamsNames.INSTRUCTOR_PERMISSION_SUBMIT_SESSION_IN_SECTIONS);
            boolean isCommentsOnResponsesAllowed = question.getQuestionDetails()
                    .isCommentsOnResponsesAllowed();
            Matcher matcher = sectionIdPattern.matcher(additionalInfoId);
            if (matcher.find()) {
                sectionId = Integer.parseInt(matcher.group(1));
            }

            InstructorFeedbackResultsResponsePanel responsePanel =
                    new InstructorFeedbackResultsResponsePanel(
                            question, response, questionText, sectionId, additionalInfoText, null,
                            displayableResponse, comments, isAllowedToSubmitSessionsInBothSection,
                            isCommentsOnResponsesAllowed);
            responsePanel.setCommentsIndexes(recipientIndex, giverIndex, responseIndex + 1);
            if (isCommentsOnResponsesAllowed) {
                Map<FeedbackParticipantType, Boolean> responseVisibilityMap = getResponseVisibilityMap(question);
                FeedbackResponseCommentRow frcForAdding = buildFeedbackResponseCommentAddForm(question, response,
                        responseVisibilityMap, giverName, recipientName);

                responsePanel.setFrcForAdding(frcForAdding);

            }
            responsePanels.add(responsePanel);
        }

        return responsePanels;
    }

    private InstructorFeedbackResultsGroupByQuestionPanel buildGroupByQuestionPanel(
            String participantIdentifier,
            Entry<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>>
                    recipientToGiverToResponsesMap,
            String additionalInfoId, int participantIndex) {
        List<InstructorFeedbackResultsQuestionTable> questionTables = new ArrayList<>();

        int questionIndex = 0;
        for (Entry<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> responsesForParticipantForQuestion
                : recipientToGiverToResponsesMap.getValue().entrySet()) {
            if (responsesForParticipantForQuestion.getValue().isEmpty()) {
                // participant has no responses for the current question
                continue;
            }

            questionIndex += 1;

            FeedbackQuestionAttributes currentQuestion = responsesForParticipantForQuestion.getKey();
            List<FeedbackResponseAttributes> responsesForQuestion = responsesForParticipantForQuestion.getValue();

            InstructorFeedbackResultsQuestionTable questionTable =
                    buildQuestionTableAndResponseRows(currentQuestion, responsesForQuestion,
                            String.format(additionalInfoId, participantIndex, questionIndex),
                            participantIdentifier, true);
            questionTable.setBoldQuestionNumber(false);
            questionTables.add(questionTable);

        }

        InstructorFeedbackResultsQuestionTable.sortByQuestionNumber(questionTables);
        InstructorFeedbackResultsGroupByQuestionPanel participantPanel;
        // Construct InstructorFeedbackResultsGroupByQuestionPanel for the current giver
        if (viewType.isPrimaryGroupingOfGiverType() && (bundle.isParticipantIdentifierStudent(participantIdentifier)
                || bundle.isParticipantIdentifierInstructor(participantIdentifier))) {
            // Moderation button on the participant panels are only shown is the panel is a giver panel,
            // and if the participant is a student
            InstructorFeedbackResultsModerationButton moderationButton =
                    buildModerationButtonForGiver(null, participantIdentifier, "btn btn-primary btn-xs",
                            MODERATE_RESPONSES_FOR_GIVER);
            participantPanel = new InstructorFeedbackResultsGroupByQuestionPanel(
                    participantIdentifier, bundle.getNameForEmail(participantIdentifier),
                    questionTables,
                    getStudentProfilePictureLink(participantIdentifier, instructor.courseId),
                    true, moderationButton);
        } else {
            participantPanel = new InstructorFeedbackResultsGroupByQuestionPanel(
                    questionTables,
                    getStudentProfilePictureLink(participantIdentifier, instructor.courseId),
                    viewType.isPrimaryGroupingOfGiverType(), participantIdentifier,
                    bundle.getNameForEmail(participantIdentifier));
        }

        return participantPanel;
    }

    private void finalizeBuildingSectionPanelWithoutTeamStats(
            InstructorFeedbackResultsSectionPanel sectionPanel,
            String sectionName) {
        LinkedHashMap<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> emptyResponseMap =
                new LinkedHashMap<>();
        LinkedHashSet<String> emptyTeamList = new LinkedHashSet<>();
        finalizeBuildingSectionPanel(sectionPanel, sectionName, emptyResponseMap, emptyTeamList);
    }

    protected abstract void finalizeBuildingSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel, String sectionName,
            Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> responsesGroupedByTeam,
            Set<String> teamsWithResponses);

    private void buildMissingTeamAndParticipantPanelsForSection(
            InstructorFeedbackResultsSectionPanel sectionPanel, String sectionName,
            Set<String> teamWithResponses) {

        // update the teams for the previous section
        Set<String> teamsInSection = bundle.getTeamsInSectionFromRoster(sectionName);
        Set<String> teamsWithoutResponses = new HashSet<>(teamsInSection);
        teamsWithoutResponses.removeAll(teamWithResponses);

        // create for every remaining team in the section, participantResultsPanels for every team member
        for (String teamWithoutResponses : teamsWithoutResponses) {
            List<String> teamMembers = new ArrayList<>(bundle.getTeamMembersFromRoster(teamWithoutResponses));
            teamMembers.sort(null);
            if (viewType.isPrimaryGroupingOfGiverType()) {
                addMissingParticipantsPanelsWithModerationButtonForTeam(
                        sectionPanel, teamWithoutResponses, teamMembers);
            } else {
                addMissingParticipantsPanelsWithoutModerationButtonForTeam(
                        sectionPanel, teamWithoutResponses, teamMembers);
            }
        }

    }

    private void buildMissingParticipantPanelsForTeam(
            InstructorFeedbackResultsSectionPanel sectionPanel, String teamName,
            Set<String> teamMembersWithResponses) {

        Set<String> teamMembersEmail = new HashSet<>();
        teamMembersEmail.addAll(bundle.getTeamMembersFromRoster(teamName));

        Set<String> teamMembersWithoutResponses = new HashSet<>(teamMembersEmail);
        teamMembersWithoutResponses.removeAll(teamMembersWithResponses);

        // Create missing participants panels for the previous team
        List<String> sortedTeamMembersWithoutResponses = new ArrayList<>(teamMembersWithoutResponses);
        sortedTeamMembersWithoutResponses.sort(null);

        if (viewType.isPrimaryGroupingOfGiverType()) {
            addMissingParticipantsPanelsWithModerationButtonForTeam(sectionPanel,
                    teamName, sortedTeamMembersWithoutResponses);
        } else {
            addMissingParticipantsPanelsWithoutModerationButtonForTeam(sectionPanel,
                    teamName, sortedTeamMembersWithoutResponses);
        }

    }

    private void buildSectionPanelsForForAjaxLoading(
            List<String> sections) {
        sectionPanels = new LinkedHashMap<>();

        if (hasEntitiesInNoSpecificSection()) {
            InstructorFeedbackResultsSectionPanel sectionPanel =
                    new InstructorFeedbackResultsSectionPanel(Const.DEFAULT_SECTION, Const.NO_SPECIFIC_SECTION, true);

            sectionPanels.put(Const.DEFAULT_SECTION, sectionPanel);
        }

        for (String section : sections) {
            InstructorFeedbackResultsSectionPanel sectionPanel =
                    new InstructorFeedbackResultsSectionPanel(section, section, true);
            sectionPanels.put(section, sectionPanel);
        }
    }

    private void buildSectionPanelWithErrorMessage() {
        sectionPanels = new LinkedHashMap<>();

        InstructorFeedbackResultsSectionPanel sectionPanel = new InstructorFeedbackResultsSectionPanel();
        sectionPanel.setSectionName(selectedSection);
        sectionPanel.setSectionNameForDisplay(selectedSection);
        sectionPanel.setAbleToLoadResponses(false);

        sectionPanels.put(selectedSection, sectionPanel);
    }

    protected List<InstructorFeedbackResultsResponseRow> buildResponseRowsForQuestionForSingleParticipant(
            FeedbackQuestionAttributes question,
            List<FeedbackResponseAttributes> responses,
            String participantIdentifier, boolean isFirstGroupedByGiver) {
        List<InstructorFeedbackResultsResponseRow> responseRows = new ArrayList<>();

        List<String> possibleParticipantsWithoutResponses = isFirstGroupedByGiver
                ? bundle.getPossibleRecipients(question, participantIdentifier)
                : bundle.getPossibleGivers(question, participantIdentifier);

        for (FeedbackResponseAttributes response : responses) {
            if (!bundle.isGiverVisible(response) || !bundle.isRecipientVisible(response)) {
                possibleParticipantsWithoutResponses.clear();
            }

            // keep track of possible participant who did not give/receive a response to/from the participantIdentifier
            String participantWithResponse = isFirstGroupedByGiver ? response.recipient : response.giver;
            removeParticipantIdentifierFromList(possibleParticipantsWithoutResponses,
                    participantWithResponse);

            InstructorFeedbackResultsModerationButton moderationButton =
                    buildModerationButtonForExistingResponse(question, response);

            InstructorFeedbackResultsResponseRow responseRow =
                    new InstructorFeedbackResultsResponseRow(
                            bundle.getGiverNameForResponse(response),
                            bundle.getTeamNameForEmail(response.giver),
                            bundle.getRecipientNameForResponse(response),
                            bundle.getTeamNameForEmail(response.recipient),
                            bundle.getResponseAnswerHtml(response, question), moderationButton);

            configureResponseRow(response.giver, response.recipient, responseRow);

            responseRows.add(responseRow);
        }

        if (isMissingResponsesShown) {
            if (isFirstGroupedByGiver) {
                responseRows.addAll(
                        buildMissingResponseRowsBetweenGiverAndPossibleRecipients(
                                question, possibleParticipantsWithoutResponses,
                                participantIdentifier,
                                bundle.getNameForEmail(participantIdentifier),
                                bundle.getTeamNameForEmail(participantIdentifier)));
            } else {
                responseRows.addAll(
                        buildMissingResponseRowsBetweenRecipientAndPossibleGivers(
                                question, possibleParticipantsWithoutResponses,
                                participantIdentifier,
                                bundle.getNameForEmail(participantIdentifier),
                                bundle.getTeamNameForEmail(participantIdentifier)));
            }
        }

        return responseRows;
    }

    /**
     * Construct missing response rows between the recipient identified by {@code recipientIdentifier} and
     * {@code possibleGivers}.
     */
    private List<InstructorFeedbackResultsResponseRow> buildMissingResponseRowsBetweenRecipientAndPossibleGivers(
            FeedbackQuestionAttributes question,
            List<String> possibleGivers, String recipientIdentifier,
            String recipientName, String recipientTeam) {
        List<InstructorFeedbackResultsResponseRow> missingResponses = new ArrayList<>();
        FeedbackQuestionDetails questionDetails = questionToDetailsMap.get(question);

        for (String possibleGiver : possibleGivers) {
            String possibleGiverName = bundle.getFullNameFromRoster(possibleGiver);
            String possibleGiverTeam = bundle.getTeamNameFromRoster(possibleGiver);

            String textToDisplay = questionDetails.getNoResponseTextInHtml(recipientIdentifier, possibleGiver,
                    bundle, question);

            if (questionDetails.shouldShowNoResponseText(question)) {
                InstructorFeedbackResultsModerationButton moderationButton = buildModerationButtonForGiver(
                        question, possibleGiver,
                        "btn btn-default btn-xs",
                        MODERATE_SINGLE_RESPONSE);
                InstructorFeedbackResultsResponseRow missingResponse = new InstructorFeedbackResultsResponseRow(
                        possibleGiverName, possibleGiverTeam,
                        recipientName, recipientTeam,
                        textToDisplay, moderationButton, true);
                missingResponse.setRowAttributes(new ElementTag("class", "pending_response_row"));
                configureResponseRow(possibleGiver, recipientIdentifier, missingResponse);

                missingResponses.add(missingResponse);
            }
        }

        return missingResponses;
    }

    /**
     * Builds participant panels for the specified team, and add to sectionPanel.
     */
    private void addMissingParticipantsPanelsWithModerationButtonForTeam(
            InstructorFeedbackResultsSectionPanel sectionPanel,
            String teamName, List<String> teamMembers) {
        for (String teamMember : teamMembers) {
            InstructorFeedbackResultsModerationButton moderationButton =
                    buildModerationButtonForGiver(null, teamMember, "btn btn-default btn-xs",
                            MODERATE_RESPONSES_FOR_GIVER);
            InstructorFeedbackResultsParticipantPanel giverPanel;

            if (viewType.isSecondaryGroupingOfParticipantType()) {

                String teamMemberNameWithTeamNameAppended = bundle.getFullNameFromRoster(teamMember)
                        + " (" + bundle.getTeamNameFromRoster(teamMember) + ")";
                giverPanel = buildInstructorFeedbackResultsGroupBySecondaryParticipantPanel(
                        teamMember, teamMemberNameWithTeamNameAppended,
                        new ArrayList<>(),
                        moderationButton);
            } else {
                giverPanel = new InstructorFeedbackResultsGroupByQuestionPanel(
                        teamMember, bundle.getFullNameFromRoster(teamMember),
                        new ArrayList<>(),
                        getStudentProfilePictureLink(teamMember, instructor.courseId),
                        viewType.isPrimaryGroupingOfGiverType(), moderationButton);
            }

            giverPanel.setHasResponses(false);
            sectionPanel.addParticipantPanel(teamName, giverPanel);
        }
    }

    private void addMissingParticipantsPanelsWithoutModerationButtonForTeam(
            InstructorFeedbackResultsSectionPanel sectionPanel,
            String teamName, List<String> teamMembers) {
        for (String teamMember : teamMembers) {

            InstructorFeedbackResultsParticipantPanel giverPanel;

            if (viewType.isSecondaryGroupingOfParticipantType()) {
                String teamMemberWithTeamNameAppended = bundle.getFullNameFromRoster(teamMember)
                        + " (" + bundle.getTeamNameFromRoster(teamMember) + ")";
                giverPanel = buildInstructorFeedbackResultsGroupBySecondaryParticipantPanel(
                        teamMember, teamMemberWithTeamNameAppended,
                        new ArrayList<>(),
                        null);

            } else {
                giverPanel = new InstructorFeedbackResultsGroupByQuestionPanel(
                        new ArrayList<>(),
                        getStudentProfilePictureLink(teamMember, instructor.courseId),
                        viewType.isPrimaryGroupingOfGiverType(), teamMember,
                        bundle.getFullNameFromRoster(teamMember));
            }
            giverPanel.setHasResponses(false);

            sectionPanel.addParticipantPanel(teamName, giverPanel);
        }
    }

    /**
     * Constructs InstructorFeedbackResultsQuestionTable containing statistics for each team.
     * The statistics tables are added to the sectionPanel.
     */
    protected void buildTeamsStatisticsTableForSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel,
            Map<String, Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>>> responsesGroupedByTeam,
            Set<String> teamsInSection) {
        Map<String, List<InstructorFeedbackResultsQuestionTable>> teamToStatisticsTables = new HashMap<>();
        for (String team : teamsInSection) {
            // skip team if no responses,
            // or if the team is an anonymous student's team or an anonymous team, or is "-"
            if (!responsesGroupedByTeam.containsKey(team) || !isTeamVisible(team)) {
                continue;
            }

            List<InstructorFeedbackResultsQuestionTable> statisticsTablesForTeam = new ArrayList<>();

            for (FeedbackQuestionAttributes question : bundle.questions.values()) {
                if (!responsesGroupedByTeam.get(team).containsKey(question)) {
                    continue;
                }

                List<FeedbackResponseAttributes> responsesForTeamAndQuestion =
                        responsesGroupedByTeam.get(team).get(question);

                InstructorFeedbackResultsQuestionTable statsTable = buildQuestionTableWithoutResponseRows(
                        question, responsesForTeamAndQuestion,
                        "");
                statsTable.setCollapsible(false);

                if (!statsTable.getQuestionStatisticsTable().isEmpty()) {
                    statisticsTablesForTeam.add(statsTable);
                }
            }

            InstructorFeedbackResultsQuestionTable.sortByQuestionNumber(statisticsTablesForTeam);
            teamToStatisticsTables.put(team, statisticsTablesForTeam);
        }

        sectionPanel.setTeamStatisticsTable(teamToStatisticsTables);
    }

    protected abstract void prepareHeadersForTeamPanelsInSectionPanel(
            InstructorFeedbackResultsSectionPanel sectionPanel);

    @Override
    protected abstract void configureResponseRow(
            String giver, String recipient, InstructorFeedbackResultsResponseRow responseRow);

    private InstructorFeedbackResultsGroupByParticipantPanel
    buildInstructorFeedbackResultsGroupBySecondaryParticipantPanel(
            String participantIdentifier, String participantName,
            List<InstructorFeedbackResultsSecondaryParticipantPanelBody> secondaryParticipantPanels,
            InstructorFeedbackResultsModerationButton moderationButton) {

        InstructorFeedbackResultsGroupByParticipantPanel bySecondaryParticipantPanel =
                new InstructorFeedbackResultsGroupByParticipantPanel(secondaryParticipantPanels);
        bySecondaryParticipantPanel.setParticipantIdentifier(participantIdentifier);
        bySecondaryParticipantPanel.setName(participantName);
        bySecondaryParticipantPanel.setIsGiver(viewType.isPrimaryGroupingOfGiverType());

        boolean isEmailValid = validator.getInvalidityInfoForEmail(participantIdentifier).isEmpty();
        bySecondaryParticipantPanel.setEmailValid(isEmailValid);

        bySecondaryParticipantPanel.setProfilePictureLink(getProfilePictureIfEmailValid(participantIdentifier));

        bySecondaryParticipantPanel.setModerationButton(moderationButton);

        bySecondaryParticipantPanel.setHasResponses(true);

        return bySecondaryParticipantPanel;
    }

    @Override
    protected boolean shouldSetAjaxClass() {
        return false;
    }

    @Override
    protected boolean isQuestionTableCollapsible() {
        return false;
    }

    @Override
    protected abstract List<InstructorFeedbackResultsResponseRow> getResponseRows(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses,
            String participantIdentifier, List<ElementTag> columnTags, Map<String, Boolean> isSortable);

    protected abstract boolean hasEntitiesInNoSpecificSection();
}

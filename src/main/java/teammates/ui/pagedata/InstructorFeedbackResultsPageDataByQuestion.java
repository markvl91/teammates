package teammates.ui.pagedata;

import com.google.appengine.api.datastore.Text;
import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.*;
import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.util.*;
import teammates.ui.datatransfer.InstructorFeedbackResultsPageViewType;
import teammates.ui.template.*;

import java.util.*;

public class InstructorFeedbackResultsPageDataByQuestion extends InstructorFeedbackResultsPageData {

    public InstructorFeedbackResultsPageDataByQuestion(AccountAttributes account, String sessionToken) {
        super(account, sessionToken);
    }

    /**
     * Prepares question tables for viewing.
     *
     * <p>{@code bundle} should be set before this method
     */
    @Override
    public void initialize(
            InstructorAttributes instructor,
            String selectedSection, String showStats,
            String groupByTeam, InstructorFeedbackResultsPageViewType view, boolean isMissingResponsesShown) {
        initCommonVariables(instructor, selectedSection, showStats, groupByTeam, isMissingResponsesShown, view);

        Map<FeedbackQuestionAttributes, List<FeedbackResponseAttributes>> questionToResponseMap =
                bundle.getQuestionResponseMap();
        questionPanels = new ArrayList<>();

        // if there is more than one question, we omit generation of responseRows,
        // and load them by ajax question by question
        boolean isLoadingStructureOnly = questionToResponseMap.size() > 1;

        questionToResponseMap.forEach((question, responses) -> {
            InstructorFeedbackResultsQuestionTable questionPanel;
            if (isLoadingStructureOnly) {
                questionPanel = buildQuestionTableWithoutResponseRows(question, responses, "");
                questionPanel.setHasResponses(false);
            } else {
                questionPanel = buildQuestionTableAndResponseRows(question, responses, "");
            }

            questionPanels.add(questionPanel);
        });
    }

    protected boolean isTeamVisible(String team) {
        return bundle.rosterTeamNameMembersTable.containsKey(team);
    }

    private InstructorFeedbackResultsQuestionTable buildQuestionTableAndResponseRows(
            FeedbackQuestionAttributes question,
            List<FeedbackResponseAttributes> responses,
            String additionalInfoId) {
        return buildQuestionTableAndResponseRows(question, responses, additionalInfoId,
                null, true);
    }

    /**
     * Builds question tables without response rows, but with stats.
     *
     * @param responses responses to compute statistics for
     */
    protected InstructorFeedbackResultsQuestionTable buildQuestionTableWithoutResponseRows(
            FeedbackQuestionAttributes question,
            List<FeedbackResponseAttributes> responses,
            String additionalInfoId) {
        return buildQuestionTableAndResponseRows(question, responses, additionalInfoId,
                null, false);
    }

    /**
     * Builds a question table for given question, and response rows for the given responses.
     *
     * @param participantIdentifier for viewTypes * > Question > *, constructs missing response rows
     *                              only for the given participant
     * @param isShowingResponseRows if false, hides the response rows
     */
    protected InstructorFeedbackResultsQuestionTable buildQuestionTableAndResponseRows(
            FeedbackQuestionAttributes question,
            List<FeedbackResponseAttributes> responses,
            String additionalInfoId,
            String participantIdentifier, boolean isShowingResponseRows) {

        List<ElementTag> columnTags = new ArrayList<>();
        Map<String, Boolean> isSortable = new HashMap<>();
        List<InstructorFeedbackResultsResponseRow> responseRows = null;

        FeedbackQuestionDetails questionDetails = questionToDetailsMap.get(question);
        if (isShowingResponseRows) {
            responseRows = getResponseRows(question, responses, participantIdentifier, columnTags, isSortable);

            // If question specific sorting is not needed, responses are sorted
            // by default order (first by team name, then by display name)
            if (questionDetails.isQuestionSpecificSortingRequired()) {
                responseRows.sort(questionDetails.getResponseRowsSortOrder());
            } else {
                responseRows = InstructorFeedbackResultsResponseRow.sortListWithDefaultOrder(responseRows);
            }

        }

        String studentEmail = student == null ? null : student.email;
        String statisticsTable = questionDetails.getQuestionResultStatisticsHtml(responses, question, studentEmail,
                bundle, viewType.toString());

        String questionText = questionDetails.getQuestionText();
        String additionalInfoText =
                questionDetails.getQuestionAdditionalInfoHtml(question.questionNumber, additionalInfoId);

        InstructorFeedbackResultsQuestionTable questionTable = new InstructorFeedbackResultsQuestionTable(
                !responses.isEmpty(), statisticsTable,
                responseRows, question,
                questionText, additionalInfoText,
                columnTags, isSortable);
        if (shouldSetAjaxClass()) {
            // setup classes, for loading responses by ajax
            // ajax_submit: user needs to click on the panel to load
            // ajax_auto: responses are loaded automatically
            questionTable.setAjaxClass(isLargeNumberOfResponses()
                    ? " ajax_submit"
                    : " ajax_auto");
        }
        questionTable.setShowResponseRows(isShowingResponseRows);
        questionTable.setCollapsible(isQuestionTableCollapsible());

        return questionTable;
    }

    protected List<InstructorFeedbackResultsResponseRow> getResponseRows(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses,
            String participantIdentifier, List<ElementTag> columnTags, Map<String, Boolean> isSortable) {
        List<InstructorFeedbackResultsResponseRow> responseRows;

        buildTableColumnHeaderForQuestionView(columnTags, isSortable);
        responseRows = buildResponseRowsForQuestion(question, responses);

        return responseRows;
    }

    protected boolean isQuestionTableCollapsible() {
        return true;
    }

    protected boolean shouldSetAjaxClass() {
        return true;
    }

    private void buildTableColumnHeaderForQuestionView(
            List<ElementTag> columnTags,
            Map<String, Boolean> isSortable) {
        ElementTag giverTeamElement =
                new ElementTag("Team", "id", "button_sortFromTeam", "class", "button-sort-none toggle-sort",
                        "style", "width: 10%; min-width: 67px;");
        ElementTag giverElement =
                new ElementTag("Giver", "id", "button_sortFromName", "class", "button-sort-none toggle-sort",
                        "style", "width: 10%; min-width: 65px;");
        ElementTag recipientTeamElement =
                new ElementTag("Team", "id", "button_sortToTeam", "class", "button-sort-ascending toggle-sort",
                        "style", "width: 10%; min-width: 67px;");
        ElementTag recipientElement =
                new ElementTag("Recipient", "id", "button_sortToName", "class", "button-sort-none toggle-sort",
                        "style", "width: 10%; min-width: 90px;");
        ElementTag responseElement =
                new ElementTag("Feedback", "id", "button_sortFeedback", "class", "button-sort-none toggle-sort",
                        "style", "width: 45%; min-width: 95px;");
        ElementTag actionElement = new ElementTag("Actions", "class", "action-header",
                "style", "width: 15%; min-width: 75px;");

        columnTags.add(giverTeamElement);
        columnTags.add(giverElement);
        columnTags.add(recipientTeamElement);
        columnTags.add(recipientElement);
        columnTags.add(responseElement);
        columnTags.add(actionElement);

        isSortable.put(giverElement.getContent(), true);
        isSortable.put(giverTeamElement.getContent(), true);
        isSortable.put(recipientElement.getContent(), true);
        isSortable.put(responseElement.getContent(), true);
        isSortable.put(actionElement.getContent(), false);
    }

    /**
     * Builds response rows for a given question. This not only builds response rows for existing responses, but
     * includes
     * the missing responses between pairs of givers and recipients.
     *
     * @param responses existing responses for the question
     */
    private List<InstructorFeedbackResultsResponseRow> buildResponseRowsForQuestion(
            FeedbackQuestionAttributes question, List<FeedbackResponseAttributes> responses) {
        List<InstructorFeedbackResultsResponseRow> responseRows = new ArrayList<>();

        List<String> possibleGiversWithoutResponses = bundle.getPossibleGivers(question);
        List<String> possibleReceiversWithoutResponsesForGiver = new ArrayList<>();

        String prevGiver = "";
        int responseRecipientIndex;
        int responseGiverIndex;
        int userIndex = 0;
        Map<String, Integer> userIndexesForComments = new HashMap<>();
        for (FeedbackResponseAttributes response : responses) {
            if (!bundle.isGiverVisible(response) || !bundle.isRecipientVisible(response)) {
                possibleGiversWithoutResponses.clear();
                possibleReceiversWithoutResponsesForGiver.clear();
            }

            // keep track of possible givers who did not give a response
            removeParticipantIdentifierFromList(possibleGiversWithoutResponses, response.giver);

            boolean isNewGiver = !prevGiver.equals(response.giver);
            if (isNewGiver) {
                if (isMissingResponsesShown) {
                    responseRows.addAll(
                            buildMissingResponseRowsBetweenGiverAndPossibleRecipients(
                                    question, possibleReceiversWithoutResponsesForGiver, prevGiver,
                                    bundle.getNameForEmail(prevGiver),
                                    bundle.getTeamNameForEmail(prevGiver)));
                }
                String giverIdentifier = response.giver;

                possibleReceiversWithoutResponsesForGiver = bundle.getPossibleRecipients(question, giverIdentifier);
            }

            // keep track of possible recipients without a response from the current giver
            removeParticipantIdentifierFromList(possibleReceiversWithoutResponsesForGiver, response.recipient);
            prevGiver = response.giver;

            InstructorFeedbackResultsModerationButton moderationButton = buildModerationButtonForExistingResponse(
                    question, response);
            InstructorFeedbackResultsResponseRow responseRow = new InstructorFeedbackResultsResponseRow(
                    bundle.getGiverNameForResponse(response),
                    bundle.getTeamNameForEmail(response.giver),
                    bundle.getRecipientNameForResponse(response),
                    bundle.getTeamNameForEmail(response.recipient),
                    bundle.getResponseAnswerHtml(response, question),
                    moderationButton);
            configureResponseRow(prevGiver, response.recipient, responseRow);
            String giverName = bundle.getNameForEmail(response.giver);
            String recipientName = bundle.getNameForEmail(response.recipient);

            String giverTeam = bundle.getTeamNameForEmail(response.giver);
            String recipientTeam = bundle.getTeamNameForEmail(response.recipient);

            giverName = bundle.appendTeamNameToName(giverName, giverTeam);
            recipientName = bundle.appendTeamNameToName(recipientName, recipientTeam);

            List<FeedbackResponseCommentRow> comments =
                    buildResponseComments(giverName, recipientName, question, response);
            if (!comments.isEmpty()) {
                responseRow.setCommentsOnResponses(comments);
            }
            Map<FeedbackParticipantType, Boolean> responseVisibilityMap = getResponseVisibilityMap(question);
            boolean isCommentsOnResponsesAllowed =
                    question.getQuestionDetails().isCommentsOnResponsesAllowed();
            if (isCommentsOnResponsesAllowed) {
                FeedbackResponseCommentRow addCommentForm = buildFeedbackResponseCommentAddForm(question, response,
                        responseVisibilityMap, giverName, recipientName);
                responseRow.setAddCommentButton(addCommentForm);
                if (userIndexesForComments.get(response.giver) == null) {
                    userIndex = generateIndexForUser(response.giver, userIndex, userIndexesForComments);
                }
                responseGiverIndex = userIndexesForComments.get(response.giver);
                if (userIndexesForComments.get(response.recipient) == null) {
                    userIndex = generateIndexForUser(response.recipient, userIndex, userIndexesForComments);
                }
                responseRecipientIndex = userIndexesForComments.get(response.recipient);

                responseRow.setResponseRecipientIndex(responseRecipientIndex);
                responseRow.setResponseGiverIndex(responseGiverIndex);
                responseRow.setCommentsOnResponsesAllowed(isCommentsOnResponsesAllowed);
            }
            responseRows.add(responseRow);
        }

        if (!responses.isEmpty()) {
            responseRows.addAll(getRemainingMissingResponseRows(question, possibleGiversWithoutResponses,
                    possibleReceiversWithoutResponsesForGiver,
                    prevGiver));
        }

        return responseRows;
    }

    protected void configureResponseRow(
            String giver, String recipient,
            InstructorFeedbackResultsResponseRow responseRow) {
        responseRow.setGiverProfilePictureLink(getProfilePictureIfEmailValid(giver));
        responseRow.setRecipientProfilePictureLink(getProfilePictureIfEmailValid(recipient));

        responseRow.setActionsDisplayed(true);
    }

    // TODO consider using Url in future
    protected String getProfilePictureIfEmailValid(String email) {
        // TODO the check for determining whether to show a profile picture
        // can be improved to use isStudent
        boolean isEmailValid = validator.getInvalidityInfoForEmail(email).isEmpty();
        return isEmailValid ? getStudentProfilePictureLink(email, instructor.courseId)
                : null;
    }

    /**
     * Construct missing response rows between the giver identified by {@code giverIdentifier} and
     * {@code possibleReceivers}.
     */
    protected List<InstructorFeedbackResultsResponseRow> buildMissingResponseRowsBetweenGiverAndPossibleRecipients(
            FeedbackQuestionAttributes question,
            List<String> possibleReceivers,
            String giverIdentifier,
            String giverName, String giverTeam) {
        List<InstructorFeedbackResultsResponseRow> missingResponses = new ArrayList<>();
        FeedbackQuestionDetails questionDetails = questionToDetailsMap.get(question);

        for (String possibleRecipient : possibleReceivers) {
            if (questionDetails.shouldShowNoResponseText(question)) {
                String textToDisplay = questionDetails.getNoResponseTextInHtml(
                        giverIdentifier, possibleRecipient, bundle, question);
                String possibleRecipientName = bundle.getFullNameFromRoster(possibleRecipient);
                String possibleRecipientTeam = bundle.getTeamNameFromRoster(possibleRecipient);

                InstructorFeedbackResultsModerationButton moderationButton =
                        buildModerationButtonForGiver(question, giverIdentifier, "btn btn-default btn-xs",
                                MODERATE_SINGLE_RESPONSE);
                InstructorFeedbackResultsResponseRow missingResponse =
                        new InstructorFeedbackResultsResponseRow(
                                giverName, giverTeam, possibleRecipientName, possibleRecipientTeam,
                                textToDisplay, moderationButton, true);

                missingResponse.setRowAttributes(new ElementTag("class", "pending_response_row"));
                configureResponseRow(giverIdentifier, possibleRecipient, missingResponse);
                missingResponses.add(missingResponse);
            }
        }

        return missingResponses;
    }

    /**
     * Given a participantIdentifier, remove it from participantIdentifierList.
     */
    protected void removeParticipantIdentifierFromList(
            List<String> participantIdentifierList, String participantIdentifier) {
        participantIdentifierList.remove(participantIdentifier);
    }

    private List<InstructorFeedbackResultsResponseRow> getRemainingMissingResponseRows(
            FeedbackQuestionAttributes question, List<String> remainingPossibleGivers,
            List<String> possibleRecipientsForGiver, String prevGiver) {
        List<InstructorFeedbackResultsResponseRow> responseRows = new ArrayList<>();

        if (possibleRecipientsForGiver != null && isMissingResponsesShown) {
            responseRows.addAll(buildMissingResponseRowsBetweenGiverAndPossibleRecipients(
                    question, possibleRecipientsForGiver,
                    prevGiver, bundle.getNameForEmail(prevGiver),
                    bundle.getTeamNameForEmail(prevGiver)));
        }

        removeParticipantIdentifierFromList(remainingPossibleGivers, prevGiver);

        for (String possibleGiverWithNoResponses : remainingPossibleGivers) {
            if (!isAllSectionsSelected()
                    && !bundle.getSectionFromRoster(possibleGiverWithNoResponses).equals(selectedSection)) {
                continue;
            }
            List<String> possibleRecipientsForRemainingGiver =
                    bundle.getPossibleRecipients(question, possibleGiverWithNoResponses);
            if (isMissingResponsesShown) {
                responseRows.addAll(
                        buildMissingResponseRowsBetweenGiverAndPossibleRecipients(
                                question,
                                possibleRecipientsForRemainingGiver,
                                possibleGiverWithNoResponses,
                                bundle.getFullNameFromRoster(possibleGiverWithNoResponses),
                                bundle.getTeamNameFromRoster(possibleGiverWithNoResponses)));
            }
        }

        return responseRows;
    }

    protected InstructorFeedbackResultsModerationButton buildModerationButtonForExistingResponse(
            FeedbackQuestionAttributes question,
            FeedbackResponseAttributes response) {
        boolean isGiverInstructor = question.giverType == FeedbackParticipantType.INSTRUCTORS;
        boolean isGiverStudentOrTeam = question.giverType == FeedbackParticipantType.STUDENTS
                || question.giverType == FeedbackParticipantType.TEAMS;

        if (isGiverStudentOrTeam || isGiverInstructor) {
            return buildModerationButtonForGiver(question, response.giver, "btn btn-default btn-xs",
                    MODERATE_SINGLE_RESPONSE);
        }
        return null;
    }

    /**
     * Gets moderation button for giver.
     *
     * @return <ul>
     * <li>null if the participant is not visible</li>
     * <li>a disabled moderation button if the instructor does not have sufficient permissions</li>
     * <li>a working moderation button otherwise</li>
     * </ul>
     */
    protected InstructorFeedbackResultsModerationButton buildModerationButtonForGiver(
            FeedbackQuestionAttributes question,
            String giverIdentifier, String className,
            String buttonText) {

        boolean isGiverInstructorOfCourse = bundle.roster.isInstructorOfCourse(giverIdentifier);
        boolean isGiverVisibleStudentOrTeam = isTeamVisible(giverIdentifier)
                || bundle.roster.isStudentInCourse(giverIdentifier);

        if (!isGiverVisibleStudentOrTeam && !isGiverInstructorOfCourse) {
            return null;
        }

        String sectionName = bundle.getSectionFromRoster(giverIdentifier);
        boolean isAllowedToModerate = isAllowedToModerate(instructor, sectionName, getFeedbackSessionName());
        boolean isDisabled = !isAllowedToModerate;
        String moderateFeedbackResponseLink = isGiverInstructorOfCourse
                ? Const.ActionURIs.INSTRUCTOR_EDIT_INSTRUCTOR_FEEDBACK_PAGE
                : Const.ActionURIs.INSTRUCTOR_EDIT_STUDENT_FEEDBACK_PAGE;
        moderateFeedbackResponseLink = addUserIdToUrl(moderateFeedbackResponseLink);

        return new InstructorFeedbackResultsModerationButton(isDisabled, className, giverIdentifier, getCourseId(),
                getFeedbackSessionName(), question, buttonText,
                moderateFeedbackResponseLink);
    }

    protected List<FeedbackResponseCommentRow> buildResponseComments(
            String giverName, String recipientName,
            FeedbackQuestionAttributes question, FeedbackResponseAttributes response) {
        List<FeedbackResponseCommentRow> comments = new ArrayList<>();
        List<FeedbackResponseCommentAttributes> frcAttributesList = bundle.responseComments.get(response.getId());
        if (frcAttributesList != null) {
            for (FeedbackResponseCommentAttributes frcAttributes : frcAttributesList) {
                comments.add(buildResponseComment(giverName, recipientName, question, response, frcAttributes));
            }
        }
        return comments;
    }

    private FeedbackResponseCommentRow buildResponseComment(
            String giverName, String recipientName,
            FeedbackQuestionAttributes question, FeedbackResponseAttributes response,
            FeedbackResponseCommentAttributes frcAttributes) {
        boolean isInstructorGiver = instructor.email.equals(frcAttributes.giverEmail);
        boolean isInstructorWithPrivilegesToModify =
                instructor.isAllowedForPrivilege(
                        response.giverSection, response.feedbackSessionName,
                        Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS)
                        && instructor.isAllowedForPrivilege(
                        response.recipientSection, response.feedbackSessionName,
                        Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
        boolean isInstructorAllowedToEditAndDeleteComment = isInstructorGiver || isInstructorWithPrivilegesToModify;

        Map<FeedbackParticipantType, Boolean> responseVisibilityMap = getResponseVisibilityMap(question);
        String whoCanSeeComment = null;
        boolean isVisibilityIconShown = false;
        if (bundle.feedbackSession.isPublished()) {
            boolean isResponseCommentPublicToRecipient = !frcAttributes.showCommentTo.isEmpty();
            isVisibilityIconShown = isResponseCommentPublicToRecipient;

            if (isVisibilityIconShown) {
                whoCanSeeComment = getTypeOfPeopleCanViewComment(frcAttributes, question);
            }
        }

        FeedbackResponseCommentRow frc = new FeedbackResponseCommentRow(
                frcAttributes, frcAttributes.giverEmail, giverName, recipientName,
                getResponseCommentVisibilityString(frcAttributes, question),
                getResponseCommentGiverNameVisibilityString(frcAttributes, question),
                responseVisibilityMap, bundle.instructorEmailNameTable,
                bundle.getTimeZone());
        frc.setVisibilityIcon(isVisibilityIconShown, whoCanSeeComment);
        if (isInstructorAllowedToEditAndDeleteComment) {
            frc.enableEditDelete();
        }

        return frc;
    }

    protected FeedbackResponseCommentRow buildFeedbackResponseCommentAddForm(
            FeedbackQuestionAttributes question,
            FeedbackResponseAttributes response, Map<FeedbackParticipantType, Boolean> responseVisibilityMap,
            String giverName, String recipientName) {
        FeedbackResponseCommentAttributes frca = FeedbackResponseCommentAttributes
                .builder(question.courseId, question.feedbackSessionName, "", new Text(""))
                .withFeedbackResponseId(response.getId())
                .withFeedbackQuestionId(question.getId())
                .build();

        FeedbackParticipantType[] relevantTypes = {
                FeedbackParticipantType.GIVER,
                FeedbackParticipantType.RECEIVER,
                FeedbackParticipantType.OWN_TEAM_MEMBERS,
                FeedbackParticipantType.RECEIVER_TEAM_MEMBERS,
                FeedbackParticipantType.STUDENTS,
                FeedbackParticipantType.INSTRUCTORS
        };

        frca.showCommentTo = new ArrayList<>();
        frca.showGiverNameTo = new ArrayList<>();
        for (FeedbackParticipantType type : relevantTypes) {
            if (isResponseCommentVisibleTo(question, type)) {
                frca.showCommentTo.add(type);
            }
            if (isResponseCommentGiverNameVisibleTo(question, type)) {
                frca.showGiverNameTo.add(type);
            }
        }

        return new FeedbackResponseCommentRow(frca, giverName, recipientName,
                getResponseCommentVisibilityString(question),
                getResponseCommentGiverNameVisibilityString(question), responseVisibilityMap,
                bundle.getTimeZone());
    }

    protected Map<FeedbackParticipantType, Boolean> getResponseVisibilityMap(FeedbackQuestionAttributes question) {
        Map<FeedbackParticipantType, Boolean> responseVisibilityMap = new HashMap<>();

        FeedbackParticipantType[] relevantTypes = {
                FeedbackParticipantType.GIVER,
                FeedbackParticipantType.RECEIVER,
                FeedbackParticipantType.OWN_TEAM_MEMBERS,
                FeedbackParticipantType.RECEIVER_TEAM_MEMBERS,
                FeedbackParticipantType.STUDENTS,
                FeedbackParticipantType.INSTRUCTORS
        };

        for (FeedbackParticipantType participantType : relevantTypes) {
            responseVisibilityMap.put(participantType, isResponseVisibleTo(participantType, question));
        }

        return responseVisibilityMap;
    }

    //TODO investigate and fix the differences between question.isResponseVisibleTo and this method
    private boolean isResponseVisibleTo(FeedbackParticipantType participantType, FeedbackQuestionAttributes question) {
        switch (participantType) {
            case GIVER:
                return question.isResponseVisibleTo(FeedbackParticipantType.GIVER);
            case INSTRUCTORS:
                return question.isResponseVisibleTo(FeedbackParticipantType.INSTRUCTORS);
            case OWN_TEAM_MEMBERS:
                return question.giverType != FeedbackParticipantType.INSTRUCTORS
                        && question.giverType != FeedbackParticipantType.SELF
                        && question.isResponseVisibleTo(FeedbackParticipantType.OWN_TEAM_MEMBERS);
            case RECEIVER:
                return question.recipientType != FeedbackParticipantType.SELF
                        && question.recipientType != FeedbackParticipantType.NONE
                        && question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER);
            case RECEIVER_TEAM_MEMBERS:
                return question.recipientType != FeedbackParticipantType.INSTRUCTORS
                        && question.recipientType != FeedbackParticipantType.SELF
                        && question.recipientType != FeedbackParticipantType.NONE
                        && question.isResponseVisibleTo(FeedbackParticipantType.RECEIVER_TEAM_MEMBERS);
            case STUDENTS:
                return question.isResponseVisibleTo(FeedbackParticipantType.STUDENTS);
            default:
                Assumption.fail("Invalid participant type");
                return false;
        }
    }

    private int generateIndexForUser(String name, int index, Map<String, Integer> userIndexesForComments) {
        userIndexesForComments.put(name, index + 1);
        return index + 1;
    }
}

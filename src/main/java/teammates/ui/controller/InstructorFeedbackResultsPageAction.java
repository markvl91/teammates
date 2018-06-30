package teammates.ui.controller;

import teammates.common.datatransfer.FeedbackSessionResultsBundle;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.ExceedingRangeException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.StatusMessage;
import teammates.common.util.StatusMessageColor;
import teammates.common.util.StringHelper;
import teammates.ui.pagedata.*;

public class InstructorFeedbackResultsPageAction extends Action {

    private static final String ALL_SECTION_OPTION = "All";
    private static final int DEFAULT_SECTION_QUERY_RANGE = 2500;

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        String showStats = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_SHOWSTATS);

        Assumption.assertPostParamNotNull(Const.ParamsNames.COURSE_ID, courseId);
        Assumption.assertPostParamNotNull(Const.ParamsNames.FEEDBACK_SESSION_NAME, feedbackSessionName);

        statusToAdmin =
                "Show instructor feedback result page<br>Session Name: " + feedbackSessionName + "<br>Course ID: " +
                        courseId;

        InstructorAttributes instructor = logic.getInstructorForGoogleId(courseId, account.googleId);
        FeedbackSessionAttributes session = logic.getFeedbackSession(feedbackSessionName, courseId);

        gateKeeper.verifyAccessible(instructor, session, false);

        String selectedSection = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_GROUPBYSECTION);

        if (selectedSection == null) {
            selectedSection = ALL_SECTION_OPTION;
        }

        boolean isMissingResponsesShown =
                getRequestParamAsBoolean(Const.ParamsNames.FEEDBACK_RESULTS_INDICATE_MISSING_RESPONSES);

        // this is for ajax loading of the html table in the modal
        // "(Non-English characters not displayed properly in the downloaded file? click here)"
        // TODO move into another action and another page data class
        boolean isLoadingCsvResultsAsHtml = getRequestParamAsBoolean(Const.ParamsNames.CSV_TO_HTML_TABLE_NEEDED);
        if (isLoadingCsvResultsAsHtml) {
            return createAjaxResultForCsvTableLoadedInHtml(courseId, feedbackSessionName, instructor, selectedSection,
                    isMissingResponsesShown, Boolean.valueOf(showStats));
        }

        String groupByTeam = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_GROUPBYTEAM);
        String sortType = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_SORTTYPE);
        String startIndex = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_MAIN_INDEX);

        if (sortType == null) {
            // default view: sort by question, statistics shown, grouped by team.
            showStats = "on";
            groupByTeam = "on";
            sortType = Const.FeedbackSessionResults.QUESTION_SORT_TYPE;
            isMissingResponsesShown = true;
        }

        InstructorFeedbackResultsPageData data;
        String viewURI;

        switch (sortType) {
            case Const.FeedbackSessionResults.GRQ_SORT_TYPE:
                data = new InstructorFeedbackResultsPageDataBySectionGiverRecipientQuestion(account, sessionToken);
                viewURI = Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_GIVER_RECIPIENT_QUESTION;
                break;
            case Const.FeedbackSessionResults.RQG_SORT_TYPE:
                data = new InstructorFeedbackResultsPageDataBySectionRecipientQuestionGiver(account, sessionToken);
                viewURI = Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_QUESTION_GIVER;
                break;
            case Const.FeedbackSessionResults.GQR_SORT_TYPE:
                data = new InstructorFeedbackResultsPageDataBySectionGiverQuestionRecipient(account, sessionToken);
                viewURI = Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_GIVER_QUESTION_RECIPIENT;
                break;
            case Const.FeedbackSessionResults.QUESTION_SORT_TYPE:
                data = new InstructorFeedbackResultsPageDataByQuestion(account, sessionToken);
                viewURI = Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_QUESTION;
                break;
            case Const.FeedbackSessionResults.RGQ_SORT_TYPE:
            default:
                data = new InstructorFeedbackResultsPageDataBySectionRecipientGiverQuestion(account, sessionToken);
                viewURI = Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_GIVER_QUESTION;
                break;
        }

        if (startIndex != null) {
            data.setStartIndex(Integer.parseInt(startIndex));
        }

        String questionId = getRequestParamValue(Const.ParamsNames.FEEDBACK_QUESTION_ID);
        String isTestingAjax = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_NEED_AJAX);

        FeedbackSessionResultsBundle bundle =
                getFeedbackSessionResultsBundle(courseId, feedbackSessionName, instructor, data, selectedSection,
                        sortType, questionId, isTestingAjax);

        data.setBundle(bundle);

        // Warning for section wise viewing in case of many responses.
        boolean showSectionWarningForQuestionView =
                data.isLargeNumberOfRespondents() && sortType.equals(Const.FeedbackSessionResults.QUESTION_SORT_TYPE);
        boolean showSectionWarningForParticipantView =
                !data.getBundle().isComplete && !sortType.equals(Const.FeedbackSessionResults.QUESTION_SORT_TYPE);

        // Warning for section wise does not make sense if there are no multiple sections.
        boolean areMultipleSectionsAvailable = data.getBundle().getRosterSectionTeamNameTable().size() > 1;

        if (selectedSection.equals(ALL_SECTION_OPTION) && (showSectionWarningForParticipantView
                || showSectionWarningForQuestionView)) {
            if (areMultipleSectionsAvailable) {
                statusToUser.add(new StatusMessage(Const.StatusMessages.FEEDBACK_RESULTS_SECTIONVIEWWARNING,
                        StatusMessageColor.WARNING));
            } else {
                statusToUser.add(new StatusMessage(Const.StatusMessages.FEEDBACK_RESULTS_QUESTIONVIEWWARNING,
                        StatusMessageColor.WARNING));
            }
            isError = true;
        }

        data.initialize(instructor, selectedSection, showStats, groupByTeam, isMissingResponsesShown);

        return createShowPageResult(viewURI, data);
    }

    private FeedbackSessionResultsBundle getFeedbackSessionResultsBundle(
            String courseId, String feedbackSessionName,
            InstructorAttributes instructor, InstructorFeedbackResultsPageData data, String selectedSection,
            String sortType, String questionId, String isTestingAjax) throws EntityDoesNotExistException {
        FeedbackSessionResultsBundle bundle = null;

        if (selectedSection.equals(ALL_SECTION_OPTION) && questionId == null &&
                !sortType.equals(Const.FeedbackSessionResults.QUESTION_SORT_TYPE)) {
            // bundle for all questions and all sections
            bundle = logic.getFeedbackSessionResultsForInstructorWithinRangeFromView(feedbackSessionName, courseId,
                    instructor.email, DEFAULT_SECTION_QUERY_RANGE, sortType);
        } else {
            switch (sortType) {
                case Const.FeedbackSessionResults.QUESTION_SORT_TYPE:
                    bundle = getFeedbackSessionResultsBundleForQuestionSortType(courseId, feedbackSessionName,
                            instructor, data, selectedSection, sortType, questionId, isTestingAjax);
                    break;
                case Const.FeedbackSessionResults.GQR_SORT_TYPE:
                case Const.FeedbackSessionResults.GRQ_SORT_TYPE:
                    bundle = logic.getFeedbackSessionResultsForInstructorFromSectionWithinRange(feedbackSessionName,
                            courseId, instructor.email, selectedSection, DEFAULT_SECTION_QUERY_RANGE);
                    break;
                case Const.FeedbackSessionResults.RQG_SORT_TYPE:
                case Const.FeedbackSessionResults.RGQ_SORT_TYPE:
                    bundle = logic.getFeedbackSessionResultsForInstructorToSectionWithinRange(feedbackSessionName,
                            courseId, instructor.email, selectedSection, DEFAULT_SECTION_QUERY_RANGE);
                    break;
            }
        }

        if (bundle == null) {
            throw new EntityDoesNotExistException(
                    "Feedback session " + feedbackSessionName + " does not exist in " + courseId + ".");
        }

        return bundle;
    }

    private FeedbackSessionResultsBundle getFeedbackSessionResultsBundleForQuestionSortType(
            String courseId,
            String feedbackSessionName, InstructorAttributes instructor, InstructorFeedbackResultsPageData data,
            String selectedSection, String sortType, String questionId, String isTestingAjax)
            throws EntityDoesNotExistException {
        FeedbackSessionResultsBundle bundle;
        if (questionId == null) {
            if (ALL_SECTION_OPTION.equals(selectedSection)) {
                // load page structure without responses

                data.setLargeNumberOfRespondents(isTestingAjax != null);

                // all sections and all questions for question view
                // set up question tables, responses to load by ajax
                bundle =
                        logic.getFeedbackSessionResultsForInstructorWithinRangeFromView(feedbackSessionName,
                                courseId, instructor.email, 1, sortType);
                // set isComplete to true to prevent behavior when there are too many responses,
                // such as the display of warning messages
                bundle.isComplete = true;
            } else {
                // bundle for all questions, with a selected section
                bundle =
                        logic.getFeedbackSessionResultsForInstructorInSection(feedbackSessionName, courseId,
                                instructor.email, selectedSection);
            }
        } else if (ALL_SECTION_OPTION.equals(selectedSection)) {
            // bundle for a specific question, with all sections
            bundle = logic.getFeedbackSessionResultsForInstructorFromQuestion(feedbackSessionName,
                    courseId, instructor.email, questionId);
        } else {
            // bundle for a specific question and a specific section
            bundle = logic.getFeedbackSessionResultsForInstructorFromQuestionInSection(
                    feedbackSessionName, courseId, instructor.email, questionId, selectedSection);
        }
        return bundle;
    }

    private ActionResult createAjaxResultForCsvTableLoadedInHtml(
            String courseId, String feedbackSessionName,
            InstructorAttributes instructor, String selectedSection, boolean isMissingResponsesShown,
            boolean isStatsShown) throws EntityDoesNotExistException {

        InstructorFeedbackResultsPageData data = new InstructorFeedbackResultsPageData(account, sessionToken);
        try {
            String csvString;

            if (selectedSection.contentEquals(ALL_SECTION_OPTION)) {
                csvString = logic.getFeedbackSessionResultSummaryAsCsv(courseId, feedbackSessionName, instructor.email,
                        isMissingResponsesShown, isStatsShown, null);
            } else {
                csvString = logic.getFeedbackSessionResultSummaryInSectionAsCsv(courseId, feedbackSessionName,
                        instructor.email, selectedSection, null, isMissingResponsesShown, isStatsShown);
            }

            data.setSessionResultsHtmlTableAsString(StringHelper.csvToHtmlTable(csvString));
        } catch (ExceedingRangeException e) {
            // not tested as the test file is not large enough to reach this catch block
            data.setSessionResultsHtmlTableAsString("");
            data.setAjaxStatus("There are too many responses. Please download the feedback results by section.");
        }

        return createAjaxResult(data);
    }

}

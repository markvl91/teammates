package teammates.ui.pagedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import teammates.common.datatransfer.FeedbackSessionResultsBundle;
import teammates.common.datatransfer.attributes.AccountAttributes;
import teammates.common.datatransfer.attributes.FeedbackQuestionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.datatransfer.questions.FeedbackQuestionDetails;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.StringHelper;
import teammates.common.util.Url;
import teammates.ui.datatransfer.InstructorFeedbackResultsPageViewType;
import teammates.ui.template.FeedbackSessionPublishButton;
import teammates.ui.template.InstructorFeedbackResultsFilterPanel;
import teammates.ui.template.InstructorFeedbackResultsModerationButton;
import teammates.ui.template.InstructorFeedbackResultsNoResponsePanel;
import teammates.ui.template.InstructorFeedbackResultsQuestionTable;
import teammates.ui.template.InstructorFeedbackResultsRemindButton;
import teammates.ui.template.InstructorFeedbackResultsSectionPanel;
import teammates.ui.template.InstructorFeedbackResultsSessionPanel;

public class InstructorFeedbackResultsPageData extends PageData {
    protected static final String MODERATE_RESPONSES_FOR_GIVER = "Moderate Responses";
    protected static final String MODERATE_SINGLE_RESPONSE = "Moderate Response";
    private static final String ALL_SECTIONS_SELECTED = "All";

    private static final int RESPONDENTS_LIMIT_FOR_AUTOLOADING = 150;

    protected static int sectionId;
    protected static Pattern sectionIdPattern = Pattern.compile("^section-(\\d+)");

    // isLargeNumberOfRespondents is an attribute used for testing the ui, for ViewType.Question
    private boolean isLargeNumberOfRespondents;

    protected FeedbackSessionResultsBundle bundle;
    protected InstructorAttributes instructor;
    protected List<String> sections;
    protected String selectedSection;
    protected String sortType;
    private String groupByTeam;
    protected String showStats;
    protected boolean isMissingResponsesShown;
    private int startIndex = -1;

    protected FieldValidator validator = new FieldValidator();
    protected String feedbackSessionName;

    private String displayableFsName;
    private String displayableCourseId;

    // used for html table ajax loading
    protected String ajaxStatus;
    protected String sessionResultsHtmlTableAsString;

    // for question view
    protected List<InstructorFeedbackResultsQuestionTable> questionPanels;
    // for giver > question > recipient, recipient > question > giver,
    // giver > recipient > question, recipient > giver > question
    protected Map<String, InstructorFeedbackResultsSectionPanel> sectionPanels;

    protected Map<FeedbackQuestionAttributes, FeedbackQuestionDetails> questionToDetailsMap = new HashMap<>();
    private Map<String, String> profilePictureLinks = new HashMap<>();

    // TODO multiple page data classes inheriting this for each view type,
    // rather than an enum determining behavior in many methods
    protected InstructorFeedbackResultsPageViewType viewType;

    public InstructorFeedbackResultsPageData(AccountAttributes account, String sessionToken) {
        super(account, sessionToken);
    }

    public void initialize(
            InstructorAttributes instructor,
            String selectedSection, String showStats,
            String groupByTeam, InstructorFeedbackResultsPageViewType view,
            boolean isMissingResponsesShown) {
        // Nothing to initialize
    }

    protected void initCommonVariables(
            InstructorAttributes instructor, String selectedSection,
            String showStats, String groupByTeam, boolean isMissingResponsesShown,
            InstructorFeedbackResultsPageViewType viewType) {

        Assumption.assertNotNull(bundle);
        this.viewType = viewType;
        this.sortType = viewType.toString();

        this.instructor = instructor;
        this.selectedSection = selectedSection;
        this.showStats = showStats;
        this.groupByTeam = groupByTeam;
        this.isMissingResponsesShown = isMissingResponsesShown;

        for (FeedbackQuestionAttributes question : bundle.questions.values()) {
            FeedbackQuestionDetails questionDetails = question.getQuestionDetails();
            questionToDetailsMap.put(question, questionDetails);
        }

        this.sections = getSectionsFromBundle();

        displayableFsName = sanitizeForHtml(bundle.feedbackSession.getFeedbackSessionName());
        displayableCourseId = sanitizeForHtml(bundle.feedbackSession.getCourseId());
    }

    private List<String> getSectionsFromBundle() {
        List<String> sectionNames = new ArrayList<>();
        for (String section : bundle.sectionsInCourse()) {
            if (!section.equals(Const.DEFAULT_SECTION)) {
                sectionNames.add(section);
            }
        }

        sectionNames.sort(null);
        return sectionNames;
    }

    protected static String getCurrentTeam(FeedbackSessionResultsBundle bundle, String giverIdentifier) {
        String currentTeam;
        if (bundle.isParticipantIdentifierInstructor(giverIdentifier)) {
            currentTeam = Const.USER_TEAM_FOR_INSTRUCTOR;
        } else {
            currentTeam = bundle.getTeamNameForEmail(giverIdentifier);
            if (currentTeam.isEmpty()) {
                currentTeam = bundle.getNameForEmail(giverIdentifier);
            }
        }

        return currentTeam;
    }

    protected int getSectionPosition(String name) {
        List<String> sections = getSectionsFromBundle();
        sections.add(0, Const.DEFAULT_SECTION);

        return sections.indexOf(name);
    }

    /*
     * getInstructorFeedbackSessionPublishAndUnpublishAction()
     * is not covered in action test, but covered in UI tests.
     */

    private FeedbackSessionPublishButton getInstructorFeedbackSessionPublishAndUnpublishAction() {
        return new FeedbackSessionPublishButton(this,
                bundle.feedbackSession,
                Const.ActionURIs.INSTRUCTOR_FEEDBACK_SESSIONS_PAGE,
                instructor,
                "btn-primary btn-block");
    }

    private Map<String, InstructorFeedbackResultsModerationButton> buildModerateButtonsForNoResponsePanel() {
        Map<String, InstructorFeedbackResultsModerationButton> moderationButtons = new HashMap<>();
        for (String giverIdentifier : bundle.responseStatus.emailNameTable.keySet()) {
            boolean isStudent = bundle.isParticipantIdentifierStudent(giverIdentifier);

            if (!isStudent) {
                continue;
            }

            String sectionName = bundle.getSectionFromRoster(giverIdentifier);
            boolean isAllowedToModerate = isAllowedToModerate(instructor, sectionName, feedbackSessionName);
            String moderateFeedbackLink = addUserIdToUrl(Const.ActionURIs.INSTRUCTOR_EDIT_STUDENT_FEEDBACK_PAGE);

            InstructorFeedbackResultsModerationButton moderationButton =
                    new InstructorFeedbackResultsModerationButton(!isAllowedToModerate, "btn btn-default btn-xs",
                            giverIdentifier,
                            bundle.feedbackSession.getCourseId(),
                            bundle.feedbackSession.getFeedbackSessionName(),
                            null, "Submit Responses", moderateFeedbackLink);
            moderationButtons.put(giverIdentifier, moderationButton);

        }

        return moderationButtons;
    }

    private InstructorFeedbackResultsRemindButton buildRemindButtonForNoResponsePanel() {

        boolean isSessionClosed = bundle.getFeedbackSession().isClosed();
        String actionLink = addUserIdToUrl(Const.ActionURIs.INSTRUCTOR_FEEDBACK_REMIND_PARTICULAR_STUDENTS_PAGE);
        actionLink = Url.addParamToUrl(actionLink, Const.ParamsNames.COURSE_ID, bundle.feedbackSession.getCourseId());
        actionLink = Url.addParamToUrl(actionLink, Const.ParamsNames.FEEDBACK_SESSION_NAME,
                bundle.feedbackSession.getFeedbackSessionName());
        actionLink = addSessionTokenToUrl(actionLink);

        return new InstructorFeedbackResultsRemindButton(isSessionClosed,
                "btn btn-default btn-xs btn-tm-actions remind-btn-no-response",
                bundle.feedbackSession.getCourseId(),
                bundle.feedbackSession.getFeedbackSessionName(),
                "Remind All", actionLink);
    }

    private String getRemindParticularStudentsLink() {
        String remindParticularStudentsLink = Const.ActionURIs.INSTRUCTOR_FEEDBACK_REMIND_PARTICULAR_STUDENTS;

        String nextUrl = addUserIdToUrl(Const.ActionURIs.INSTRUCTOR_FEEDBACK_RESULTS_PAGE);
        nextUrl = Url.addParamToUrl(nextUrl, Const.ParamsNames.COURSE_ID, bundle.feedbackSession.getCourseId());
        nextUrl = Url.addParamToUrl(nextUrl, Const.ParamsNames.FEEDBACK_SESSION_NAME,
                bundle.feedbackSession.getFeedbackSessionName());
        remindParticularStudentsLink = Url.addParamToUrl(remindParticularStudentsLink, Const.ParamsNames.NEXT_URL,
                nextUrl);
        remindParticularStudentsLink = addSessionTokenToUrl(remindParticularStudentsLink);
        return remindParticularStudentsLink;
    }

    @Override
    public String getStudentProfilePictureLink(String studentEmail, String courseId) {
        return profilePictureLinks.computeIfAbsent(studentEmail,
                key -> super.getStudentProfilePictureLink(StringHelper.encrypt(key),
                        StringHelper.encrypt(courseId)));
    }

    public void setBundle(FeedbackSessionResultsBundle bundle) {
        this.bundle = bundle;
    }

    public FeedbackSessionResultsBundle getBundle() {
        return bundle;
    }

    public InstructorAttributes getInstructor() {
        return instructor;
    }

    public List<String> getSections() {
        return sections;
    }

    public String getSelectedSection() {
        return selectedSection;
    }

    public String getSortType() {
        return sortType;
    }

    @Deprecated
    public String getGroupByTeam() {
        return groupByTeam == null ? "null" : groupByTeam;
    }

    // TODO: swap groupByTeam to a normal boolean
    public boolean isGroupedByTeam() {
        return "on".equals(groupByTeam);
    }

    // TODO: swap showStats to a normal boolean
    private boolean isStatsShown() {
        return showStats != null;
    }

    public boolean isMissingResponsesShown() {
        return isMissingResponsesShown;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public String getCourseId() {
        return displayableCourseId;
    }

    public String getFeedbackSessionName() {
        return displayableFsName;
    }

    public String getAjaxStatus() {
        return ajaxStatus;
    }

    public String getSessionResultsHtmlTableAsString() {
        return sessionResultsHtmlTableAsString;
    }

    public List<InstructorFeedbackResultsQuestionTable> getQuestionPanels() {
        return questionPanels;
    }

    public Map<String, InstructorFeedbackResultsSectionPanel> getSectionPanels() {
        return sectionPanels;
    }

    private String getInstructorFeedbackSessionEditLink() {
        return instructor.isAllowedForPrivilege(Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION)
                ? getInstructorFeedbackEditLink(bundle.feedbackSession.getCourseId(),
                bundle.feedbackSession.getFeedbackSessionName(), true)
                : null;
    }

    private String getInstructorFeedbackSessionResultsLink() {
        return getInstructorFeedbackResultsLink(bundle.feedbackSession.getCourseId(),
                bundle.feedbackSession.getFeedbackSessionName());
    }

    protected boolean isAllowedToModerate(
            InstructorAttributes instructor, String sectionName, String feedbackSessionName) {
        return instructor.isAllowedForPrivilege(sectionName, feedbackSessionName,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
    }

    public boolean isAllSectionsSelected() {
        return ALL_SECTIONS_SELECTED.equals(selectedSection);
    }

    // TODO: place below getter methods for template objects in some init method common to all views
    public InstructorFeedbackResultsSessionPanel getSessionPanel() {
        return new InstructorFeedbackResultsSessionPanel(
                bundle.feedbackSession, getInstructorFeedbackSessionEditLink(),
                getInstructorFeedbackSessionPublishAndUnpublishAction(), selectedSection,
                isMissingResponsesShown, isStatsShown());
    }

    public InstructorFeedbackResultsFilterPanel getFilterPanel() {
        return new InstructorFeedbackResultsFilterPanel(
                isStatsShown(), bundle.feedbackSession, isAllSectionsSelected(), selectedSection,
                isGroupedByTeam(), sortType, getInstructorFeedbackSessionResultsLink(),
                getSections(), isMissingResponsesShown);
    }

    public InstructorFeedbackResultsNoResponsePanel getNoResponsePanel() {
        return new InstructorFeedbackResultsNoResponsePanel(bundle.responseStatus,
                buildModerateButtonsForNoResponsePanel(),
                buildRemindButtonForNoResponsePanel(),
                getRemindParticularStudentsLink());
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setAjaxStatus(String ajaxStatus) {
        this.ajaxStatus = ajaxStatus;
    }

    public void setSessionResultsHtmlTableAsString(String sessionResultsHtmlTableAsString) {
        this.sessionResultsHtmlTableAsString = sessionResultsHtmlTableAsString;
    }

    public boolean isLargeNumberOfResponses() {
        boolean isQuestionViewType = viewType == InstructorFeedbackResultsPageViewType.QUESTION;
        return isQuestionViewType && isLargeNumberOfRespondents() && isAllSectionsSelected()
                || !bundle.isComplete;
    }

    public boolean isLargeNumberOfRespondents() {
        int numRespondents = bundle.feedbackSession.getRespondingInstructorList().size()
                + bundle.feedbackSession.getRespondingStudentList().size();
        return isLargeNumberOfRespondents
                || numRespondents > RESPONDENTS_LIMIT_FOR_AUTOLOADING;
    }

    // Only used for testing the ui
    public void setLargeNumberOfRespondents(boolean needAjax) {
        this.isLargeNumberOfRespondents = needAjax;
    }
}

package org.msf.records.ui.chart;

import android.content.res.Resources;

import com.google.android.apps.common.testing.ui.espresso.Espresso;

import android.support.annotation.Nullable;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.apps.common.testing.ui.espresso.IdlingResource;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.FetchXformSucceededEvent;
import org.msf.records.events.SubmitXformSucceededEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.net.model.Patient;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.ui.sync.EventBusIdlingResource;
import org.msf.records.utils.Logger;
import org.msf.records.widget.FastDataGridView;
import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers.isDialog;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasDescendant;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasSibling;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isAssignableFrom;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDescendantOfA;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withParent;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.msf.records.ui.matchers.AppPatientMatchers.isPatientWithId;
import static org.msf.records.ui.matchers.ViewMatchers.hasBackground;
import static org.msf.records.ui.matchers.ViewMatchers.inRow;

/**
 * Functional test for {@link PatientChartActivity}.
 */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final int ROW_HEIGHT = 84;

    public PatientChartActivityTest() {
        super();
    }

    // TODO: Use proper demo data.
    protected final AppPatientDelta mDemoPatient = new AppPatientDelta();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        populateDemoPatient();
    }

    /** Selects a patient by id from the patient list. */
    private void selectPatient(String id) {
        onData(isPatientWithId(equalToIgnoringCase(id)))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .perform(click());
    }

    /** Tests that the vital views are displayed in patient chart. */
    public void testPatientChart_VitalViewsDisplayed() {
        initWithDemoPatient();
        onView(withText(equalToIgnoringCase("GENERAL CONDITION"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the chart views are displayed in patient chart. */
    public void testPatientChart_ChartViewsDisplayed() {
        initWithDemoPatient();
        onView(withText(equalToIgnoringCase("Weight (kg)"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the general condition dialog successfully changes general condition. */
    public void testGeneralConditionDialog_AppliesGeneralConditionChange() {
        initWithDemoPatient();
        onView(withId(R.id.patient_chart_vital_general_parent)).perform(click());
        screenshot("General Condition Dialog");

        onView(withText(R.string.status_convalescent)).perform(click());
        // Wait for a sync operation to update the chart.
        EventBusIdlingResource<SyncFinishedEvent> syncFinishedIdlingResource =
                new EventBusIdlingResource<SyncFinishedEvent>(
                        UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncFinishedIdlingResource);

        // Check for updated vital view.
        onView(withText(R.string.status_convalescent)).check(matches(isDisplayed()));

        // Check for updated chart view.
        onView(allOf(
                withText(R.string.status_short_desc_convalescent),
                not(withId(R.id.patient_chart_vital_general_condition_number))))
                .check(matches(isDisplayed()));
    }

    /** Tests that the encounter form can be opened more than once. */
    public void testPatientChart_CanOpenEncounterFormMultipleTimes() {
        initWithDemoPatient();
        // Load the chart once
        openEncounterForm();

        // Dismiss
        onView(withText("Discard")).perform(click());

        // Load the chart again
        openEncounterForm();

        // Dismiss
        onView(withText("Discard")).perform(click());
    }

    /**
     * Tests that the admission date is correctly displayed in the header.
     * TODO: Currently disabled. Re-enable once date picker selection works.
     */
    /*public void testPatientChart_ShowsCorrectAdmissionDate() {
        mDemoPatient.admissionDate = Optional.of(DateTime.now().minusDays(5));
        initWithDemoPatient();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_admission_days)),
                withText("Day 6")))
                .check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows the correct symptoms onset date.
     * TODO: Currently disabled. Re-enable once date picker selection works.
     */
    /*public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
        initWithDemoPatient();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_symptoms_onset_days)),
                withText("Day 8")))
                .check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows all days, even when no observations are present.
     * TODO: Currently disabled. Re-enable once date picker selection works.
     */
     /*public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        initWithDemoPatient();
        onView(withText(containsString("Today (Day 6)"))).check(matchesWithin(isDisplayed(), 5000));
        screenshot("Patient Chart");
    }*/

    /** Tests that encounter time can be set to a date in the past and still displayed correctly. */
    public void testCanSubmitObservationsInThePast() {
        initWithDemoPatient();
        openEncounterForm();
        selectDateFromDatePicker("2015", "Jan", null);
        answerVisibleTextQuestion("Temperature", "29.1");
        saveForm();
        checkObservationValueEquals(0, "29.1", "1 Jan"); // Temperature
    }

    /** Tests that encounter times in the future are not allowed. */
    public void testFutureObservationsAreNotSubmittable() {
        initWithDemoPatient();
        openEncounterForm();
        selectDateFromDatePicker("2016", "Jan", null);
        answerVisibleTextQuestion("Temperature", "29.1");
        onView(withText("Save")).perform(click());

        // Saving form should not work (can't check for a Toast within Espresso)
        onView(withText(R.string.form_entry_save)).check(matches(isDisplayed()));
    }

    /** Tests that dismissing a form immediately closes it if no changes have been made. */
    public void testDismissButtonReturnsImmediatelyWithNoChanges() {
        initWithDemoPatient();
        openEncounterForm();
        discardForm();
        onView(withText(R.string.last_observation_none)).check(matches(isDisplayed()));
    }

    /** Tests that dismissing a form results in a dialog if changes have been made. */
    public void testDismissButtonShowsDialogWithChanges() {
        initWithDemoPatient();
        openEncounterForm();
        answerVisibleTextQuestion("Temperature", "29.1");

        // Try to discard and give up.
        discardForm();
        onView(withText(R.string.title_discard_observations)).check(matches(isDisplayed()));
        onView(withText(R.string.no)).perform(click());

        // Try to discard and actually go back.
        discardForm();
        onView(withText(R.string.title_discard_observations)).check(matches(isDisplayed()));
        onView(withText(R.string.yes)).perform(click());
        onView(withText(R.string.last_observation_none)).check(matches(isDisplayed()));
    }

    /** Tests that PCR submission does not occur without confirmation being specified. */
    public void testPcr_requiresConfirmation() {
        initWithDemoPatient();
        openPcrForm();
        answerVisibleTextQuestion("Ebola L gene", "38");
        answerVisibleTextQuestion("Ebola Np gene", "35");

        onView(withText("Save")).perform(click());

        // Saving form should not work (can't check for a Toast within Espresso)
        onView(withText(R.string.form_entry_save)).check(matches(isDisplayed()));

        // Try again with confirmation
        answerVisibleOnOffQuestion("confirm this lab test result", "Confirm Lab Test Results");
        saveForm();

        // Check that new values displayed.
        onView(withText(containsString("38.0 / 35.0"))).check(matches(isDisplayed()));
    }

    /** Tests that PCR displays 'NEG' in place of numbers when 40.0 is specified. */
    public void testPcr_showsNegFor40() {
        initWithDemoPatient();
        openPcrForm();
        answerVisibleTextQuestion("Ebola L gene", "40");
        answerVisibleTextQuestion("Ebola Np gene", "40");
        answerVisibleOnOffQuestion("confirm this lab test result", "Confirm Lab Test Results");
        saveForm();
        onView(withText(containsString("NEG / NEG"))).check(matches(isDisplayed()));
    }

    /** Exercises all fields in the encounter form, except for encounter time. */
    public void testEncounter_allFieldsWorkOtherThanEncounterTime() {
        initWithDemoPatient();
        openEncounterForm();
        answerVisibleTextQuestion("Pulse", "80");
        answerVisibleTextQuestion("Respiratory rate", "20");
        answerVisibleTextQuestion("Temperature", "31");
        answerVisibleTextQuestion("Weight", "90");
        answerVisibleOnOffQuestion("Signs and Symptoms", "Nausea");
        answerVisibleTextQuestion("Vomiting", "4");
        answerVisibleTextQuestion("Diarrhoea", "6");
        answerVisibleOnOffQuestion("Pain level", "Severe");
        answerVisibleOnOffQuestion("Pain (Detail)", "Headache");
        answerVisibleOnOffQuestion("Pain (Detail)", "Back pain");
        answerVisibleOnOffQuestion("Bleeding", "Yes");
        answerVisibleOnOffQuestion("Bleeding (Detail)", "Nosebleed");
        answerVisibleOnOffQuestion("Weakness", "Moderate");
        answerVisibleOnOffQuestion("Other Symptoms", "Red eyes");
        answerVisibleOnOffQuestion("Other Symptoms", "Hiccups");
        answerVisibleOnOffQuestion("Consciousness", "Responds to voice");
        answerVisibleOnOffQuestion("Mobility", "Assisted");
        answerVisibleOnOffQuestion("Diet", "Fluids");
        answerVisibleOnOffQuestion("Hydration", "Needs ORS");
        answerVisibleOnOffQuestion("Condition", "5");
        answerVisibleOnOffQuestion("Additional Details", "Pregnant");
        answerVisibleOnOffQuestion("Additional Details", "IV access present");
        answerVisibleTextQuestion("Notes", "possible malaria");
        saveForm();

        checkVitalValueContains("Pulse", "80");
        checkVitalValueContains("Respiration", "20");
        checkVitalValueContains("Consciousness", "Responds to voice");
        checkVitalValueContains("Mobility", "Assisted");
        checkVitalValueContains("Diet", "Fluids");
        checkVitalValueContains("Hydration", "Needs ORS");
        checkVitalValueContains("Condition", "5");
        checkVitalValueContains("Pain Level", "Severe");

        checkObservationValueEquals(0, "31.0", "Today"); // Temp
        checkObservationValueEquals(1, "90", "Today"); // Weight
        checkObservationValueEquals(2, "5", "Today"); // Condition
        checkObservationValueEquals(3, "V", "Today"); // Consciousness
        checkObservationValueEquals(4, "As", "Today"); // Mobility
        checkObservationSet(5, "Today"); // Nausea
        checkObservationValueEquals(6, "4", "Today"); // Vomiting
        checkObservationValueEquals(7, "6", "Today"); // Diarrhoea
        checkObservationValueEquals(8, "3", "Today"); // Pain level
        checkObservationValueEquals(9, "1", "Today"); // Bleeding
        checkObservationValueEquals(10, "2", "Today"); // Weakness
        checkObservationSet(13, "Today"); // Hiccups
        checkObservationSet(14, "Today"); // Red eyes
        checkObservationSet(15, "Today"); // Headache
        checkObservationSet(21, "Today"); // Back pain
        checkObservationSet(24, "Today"); // Nosebleed

        onView(withText(containsString("Pregnant"))).check(matches(isDisplayed()));
        onView(withText(containsString("IV Fitted"))).check(matches(isDisplayed()));

        // TODO: check notes
    }

    // TODO: Replace with more extensive, externalized demo data.

    /**
     * Creates a patient matching mDemoPatient on the server and navigates to that patient's chart.
     * Note: this function will not work during {@link #setUp()} as it relies on
     * {@link #waitForProgressFragment()}.
     */
    protected void initWithDemoPatient() {
        waitForProgressFragment(); // Wait for tent selection screen to load.

        LOG.i("Adding patient: %s", mDemoPatient.toContentValues().toString());

        onView(withId(R.id.action_add)).perform(click());
        onView(withText("New Patient")).check(matches(isDisplayed()));
        if (mDemoPatient.id.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_id))
                    .perform(typeText(mDemoPatient.id.get()));
        }
        if (mDemoPatient.givenName.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_given_name))
                    .perform(typeText(mDemoPatient.givenName.get()));
        }
        if (mDemoPatient.familyName.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_family_name))
                    .perform(typeText(mDemoPatient.familyName.get()));
        }
        if (mDemoPatient.birthdate.isPresent()) {
            Period age = new Period(mDemoPatient.birthdate.get().toLocalDate(), LocalDate.now());
            if (age.getYears() < 1) {
                onView(withId(R.id.patient_creation_text_age))
                        .perform(typeText(Integer.toString(age.getMonths())));
                onView(withId(R.id.patient_creation_radiogroup_age_units_months)).perform(click());
            } else {
                onView(withId(R.id.patient_creation_text_age))
                        .perform(typeText(Integer.toString(age.getYears())));
                onView(withId(R.id.patient_creation_radiogroup_age_units_years)).perform(click());
            }
        }
        if (mDemoPatient.gender.isPresent()) {
            if (mDemoPatient.gender.get() == AppPatient.GENDER_MALE) {
                onView(withId(R.id.patient_creation_radiogroup_age_sex_male)).perform(click());
            } else if (mDemoPatient.gender.get() == AppPatient.GENDER_FEMALE) {
                onView(withId(R.id.patient_creation_radiogroup_age_sex_female)).perform(click());
            }
        }
        if (mDemoPatient.admissionDate.isPresent()) {
            // TODO: Currently broken -- hopefully fixed by Espresso 2.0.
            // onView(withId(R.id.patient_creation_admission_date)).perform(click());
            // selectDateFromDatePicker(mDemoPatient.admissionDate.get());
        }
        if (mDemoPatient.firstSymptomDate.isPresent()) {
            // TODO: Currently broken -- hopefully fixed by Espresso 2.0.
            // onView(withId(R.id.patient_creation_symptoms_onset_date)).perform(click());
            // selectDateFromDatePicker(mDemoPatient.firstSymptomDate.get());
        }
        if (mDemoPatient.assignedLocationUuid.isPresent()) {
            // TODO: Add support. A little tricky as we need to select by UUID.
            // onView(withId(R.id.patient_creation_button_change_location)).perform(click());
        }

        EventBusIdlingResource<SingleItemCreatedEvent<AppPatient>> resource =
                new EventBusIdlingResource<SingleItemCreatedEvent<AppPatient>>(
                        UUID.randomUUID().toString(), mEventBus
                );

        onView(withId(R.id.patient_creation_button_create)).perform(click());

        // Wait for patient to be created.
        Espresso.registerIdlingResources(resource);

        // Open patient list.
        waitForProgressFragment();
        onView(withId(R.id.action_search)).perform(click());
        waitForProgressFragment();

        // Select the patient.
        selectPatient(mDemoPatient.id.get());
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    private void selectDateFromDatePickerDialog(DateTime dateTime) {
        onView(withText("Set"))
                .inRoot(isDialog())
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void selectDateFromDatePicker(
            @Nullable String year,
            @Nullable String monthOfYear,
            @Nullable String dayOfMonth) {
        LOG.e("Year: %s, Month: %s, Day: %s", year, monthOfYear, dayOfMonth);

        if (year != null) {
            setDateSpinner("year", year);
        }
        if (monthOfYear != null) {
            setDateSpinner("month", monthOfYear);
        }
        if (dayOfMonth != null) {
            setDateSpinner("day", dayOfMonth);
        }
    }

    protected void selectDateFromDatePicker(DateTime dateTime) {
        String year = dateTime.toString("yyyy");
        String monthOfYear = dateTime.toString("MMM");
        String dayOfMonth = dateTime.toString("dd");

        selectDateFromDatePicker(year, monthOfYear, dayOfMonth);
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    protected void setDateSpinner(String spinnerName, String value) {
        int numberPickerId =
                Resources.getSystem().getIdentifier("numberpicker_input", "id", "android");
        int spinnerId =
                Resources.getSystem().getIdentifier(spinnerName, "id", "android");
        LOG.i("%s: %s", spinnerName, value);
        LOG.i("numberPickerId: %d", numberPickerId);
        LOG.i("spinnerId: %d", spinnerId);
        onView(allOf(withId(numberPickerId), withParent(withId(spinnerId))))
                .check(matches(isDisplayed()))
                .perform(typeText(value));
    }

    private void populateDemoPatient() {
        // Setting assigned location during this test is currently unsupported.
        // mDemoPatient.assignedLocationUuid = Optional.of(Zone.TRIAGE_ZONE_UUID);
        mDemoPatient.familyName = Optional.of("ChartActivity");
        mDemoPatient.givenName = Optional.of("TestPatientFor");
        mDemoPatient.firstSymptomDate = Optional.of(LocalDate.now().minusMonths(7));
        mDemoPatient.gender = Optional.of(Patient.GENDER_FEMALE);
        mDemoPatient.id = Optional.of(UUID.randomUUID().toString().substring(30));
        mDemoPatient.birthdate = Optional.of(DateTime.now().minusYears(12).minusMonths(3));
    }

    protected void openEncounterForm() {
        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
                new EventBusIdlingResource<FetchXformSucceededEvent>(
                        UUID.randomUUID().toString(),
                        mEventBus);
        onView(withId(R.id.action_update_chart)).perform(click());
        Espresso.registerIdlingResources(xformIdlingResource);
        //onView(withText("Encounter")).check(matches(isDisplayed()));
    }

    protected void openPcrForm() {
        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
                new EventBusIdlingResource<FetchXformSucceededEvent>(
                        UUID.randomUUID().toString(),
                        mEventBus);
        onView(withId(R.id.action_add_test_result)).perform(click());
        Espresso.registerIdlingResources(xformIdlingResource);
        onView(withText("Encounter")).check(matches(isDisplayed()));
    }

    private void discardForm() {
        onView(withText("Discard")).perform(click());
    }

    private void saveForm() {
        IdlingResource xformWaiter = getXformSubmissionIdlingResource();
        onView(withText("Save")).perform(click());
        Espresso.registerIdlingResources(xformWaiter);
    }

    private void answerVisibleTextQuestion(String questionText, String answerText) {
        onView(allOf(
                isAssignableFrom(EditText.class),
                hasSibling(allOf(
                        isAssignableFrom(MediaLayout.class),
                        hasDescendant(allOf(
                                isAssignableFrom(TextView.class),
                                withText(containsString(questionText))))))))
                .perform(scrollTo(), typeText(answerText));
    }

    private void answerVisibleOnOffQuestion(String questionText, String answerText) {
        onView(allOf(
                anyOf(isAssignableFrom(CheckBox.class), isAssignableFrom(RadioButton.class)),
                isDescendantOfA(allOf(
                        anyOf(
                                isAssignableFrom(ButtonsSelectOneWidget.class),
                                isAssignableFrom(TableWidgetGroup.class),
                                isAssignableFrom(ODKView.class)),
                        hasDescendant(withText(containsString(questionText))))),
                withText(containsString(answerText))))
                .perform(scrollTo(), click());
    }

    private void checkObservationValueEquals(int row, String value, String dateKey) {
        // TODO: actually check dateKey

        onView(allOf(
                withText(value),
                isDescendantOfA(inRow(row, ROW_HEIGHT)),
                isDescendantOfA(isAssignableFrom(FastDataGridView.LinkableRecyclerView.class))))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    private void checkObservationSet(int row, String dateKey) {
        // TODO: actually check dateKey
        onView(allOf(
                isDescendantOfA(inRow(row, ROW_HEIGHT)),
                hasBackground(
                        getActivity().getResources().getDrawable(R.drawable.chart_cell_active)),
                isDescendantOfA(isAssignableFrom(FastDataGridView.LinkableRecyclerView.class))))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    private void checkVitalValueContains(String vitalName, String vitalValue) {
        // Check for updated vital view.
        onView(allOf(
                withText(containsString(vitalValue)),
                hasSibling(withText(containsString(vitalName)))))
                .check(matches(isDisplayed()));
    }

    private IdlingResource getXformSubmissionIdlingResource() {
        return new EventBusIdlingResource<SubmitXformSucceededEvent>(
                UUID.randomUUID().toString(),
                mEventBus);
    }
}

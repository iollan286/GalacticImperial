package com.galacticimperial;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GameSettingsActivity extends AppCompatActivity {

    // Maps each field key → its EditText, so we can read/write all values for import/export.
    private final Map<String, EditText> fieldMap = new HashMap<>();
    private final List<SettingDef> settings = new ArrayList<>();

    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<String>   exportLauncher;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) importSettings(uri); }
        );

        exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> { if (uri != null) exportSettings(uri); }
        );

        // Root scroll view
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.background));

        // Main container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(20), dp(16), dp(48));
        scroll.addView(container, matchWrap());

        setContentView(scroll);

        defineSettings();
        buildUI(container);
    }

    // ── Settings definition ──────────────────────────────────────────────────

    private void defineSettings() {

        // ── RESOURCE SLOTS PER PLANET ────────────────────────────────────────
        settings.add(SettingDef.header("RESOURCE SLOTS PER PLANET",
            "This section determines the minimum and maximum number of slots a planet can have "
            "for each basic resource. The total number of factories and/or extractors a planet "
            "can have producing a given resource can never be greater than the total number of "
            "slots the planet has for that resource."));

        settings.add(SettingDef.minMax("supplies", "Supplies",
            "Each population unit will try to consume a certain number of units of supplies per turn. " +
            "If they consume enough per turn, their numbers may grow, and any unrest may decline. " +
            "If they don't consume enough per turn, that can lead to starvation, growing unrest, " +
            "and a collapse in the birth rate.", 2, 2));

        settings.add(SettingDef.minMax("metals", "Metals",
            "You need metals to build things out of.", 2, 2));

        settings.add(SettingDef.minMax("chemicals", "Chemicals",
            "You also need chemicals to build things out of.", 2, 2));

        settings.add(SettingDef.minMax("gold", "Gold",
            "Building things with gold is optional, but it can make them last longer.", 2, 2));

        settings.add(SettingDef.minMax("chronium", "Chronium",
            "Chronium is a powerful but volatile fuel. It has to be mixed with Q-ide before use " +
            "to prevent a catastrophic explosion.", 2, 2));

        settings.add(SettingDef.minMax("qide", "Q-ide",
            "Q-ide is a less powerful fuel than Chronium, but it has to be mixed with the Chronium " +
            "for safety reasons.", 2, 2));

        // ── DEMOGRAPHIC VARIABLES ────────────────────────────────────────────
        settings.add(SettingDef.header("DEMOGRAPHIC VARIABLES",
            "These are baseline demographic variables for the game you're creating. They "
            "influence population growth and decline."));

        settings.add(SettingDef.single("turns_maturity", "Turns To Maturity",
            "This is the baseline number of turns required for a child population unit to become " +
            "an adult population unit.", 2));

        settings.add(SettingDef.single("turns_death", "Turns To Death",
            "This is the baseline number of turns an adult population unit lasts before it dies.", 5));

        settings.add(SettingDef.single("max_consumption", "Max Consumption",
            "This is the baseline number of units of supplies a population unit will consume each turn " +
            "if there's no limit to how much they can consume.", 2));

        settings.add(SettingDef.single("consumption_fertility_threshold", "Consumption Fertility Threshold",
            "This is the baseline number of units of supplies a population unit has to consume per turn " +
            "in order to have replacement level fertility.", 2));

        settings.add(SettingDef.single("hours_threshold", "Hours Threshold",
            "This is the baseline number of working hours per turn a population unit can tolerate while " +
            "still achieving replacement level fertility. More hours per turn and fertility will be lower. " +
            "Fewer hours per turn and fertility may be higher.", 2));

        settings.add(SettingDef.single("starvation_threshold", "Starvation Threshold",
            "This is the number of units of supplies a population unit has to consume per turn in order " +
            "not to die of starvation. It is the same for all population units regardless of race and age.", 2));

        // ── ECONOMIC VARIABLES ───────────────────────────────────────────────
        settings.add(SettingDef.header("ECONOMIC VARIABLES",
            "These are baseline economic variables for the game you're creating. They "
            "influence how productive each population unit is."));

        settings.add(SettingDef.single("labor_points_per_hour", "Labor Points Per Hour",
            "This determines the baseline number of labor points a population unit produces per " +
            "working hour.", 2));

        settings.add(SettingDef.single("labor_mult_factor", "Labor Multiplication Factor",
            "This determines how much the number of labor points a population unit produces per " +
            "working hour is multiplied with each successive technological upgrade.", 5));

        settings.add(SettingDef.single("childhood_penalty", "Childhood Penalty",
            "Child population units produce fewer labor points than adult population units do. " +
            "The baseline number of labor points a child population unit produces per working hour " +
            "is equal to this factor times the baseline number of labor points produced by an adult " +
            "population unit per working hour.", 5)
            .withConstraint("Must be > 0 and ≤ 1"));

        settings.add(SettingDef.single("riots_penalty", "Riots Penalty",
            "It's hard to work when an angry mob sets your workplace on fire. When riots are " +
            "occurring on any planet, the number of labor points produced per working hour by each " +
            "population unit on that planet is multiplied by this factor.", 5)
            .withConstraint("Must be > 0 and ≤ 1"));

        // ── UNREST VARIABLES ─────────────────────────────────────────────────
        settings.add(SettingDef.header("UNREST VARIABLES",
            "These are baseline variables affecting the domestic political situation on "
            "each inhabited planet."));

        settings.add(SettingDef.single("consumption_unrest_threshold", "Consumption Unrest Threshold",
            "This is the baseline number of units of supplies each population unit on a planet has " +
            "to consume per turn in order to avoid accruing unrest points.", 2));

        settings.add(SettingDef.single("consumption_penalty", "Consumption Penalty",
            "This is the baseline number of unrest points which a planet accrues for each turn the " +
            "number of units of supplies it consumes is below the consumption unrest threshold.", 2));

        settings.add(SettingDef.single("starvation_penalty", "Starvation Penalty",
            "This is the baseline number of unrest points which will accrue on any inhabited planet " +
            "on any given turn in which the number of units of supplies consumed by each population " +
            "unit on that planet is below the starvation threshold. This penalty is cumulative with " +
            "the consumption penalty.", 2));

        settings.add(SettingDef.single("labor_threshold", "Labor Threshold",
            "This is the baseline number of working hours a population unit can tolerate per turn " +
            "without getting mad. If in any given turn the number of working hours per turn, per " +
            "population unit, on any given planet, is greater than this number, that planet will " +
            "accrue unrest points on that turn.", 2));

        settings.add(SettingDef.single("labor_penalty", "Labor Penalty",
            "This is the baseline number of unrest points which will accrue on any inhabited planet " +
            "on any given turn in which the number of hours worked by each population unit on that " +
            "planet is above the labor threshold.", 2));

        // DISABLED — not applicable in Tutorial 1
        settings.add(SettingDef.single("diversity_penalty", "Diversity Penalty",
            "Do they really have to live with us? For each unit of enemy population present on one " +
            "of your planets on any given turn, that planet will accrue this many unrest points that " +
            "turn, divided by the number of units of your own population present on that planet in " +
            "that same turn.", 2).disabled());

        settings.add(SettingDef.single("war_penalty", "War Penalty",
            "Just because you want to start a war, doesn't mean your citizens want it. Every time " +
            "you start a war, all of your inhabited planets will accrue this number of unrest points " +
            "unless the number is modified by other factors.", 5).disabled());

        settings.add(SettingDef.single("slavery_penalty", "Slavery Penalty",
            "I know, I know, maybe you're into the whole 'evil empire' thing, but that doesn't mean " +
            "your citizens approve of it. For each unit of enemy population you keep enslaved in any " +
            "given turn, all your inhabited planets will accrue this number of unrest points per turn, " +
            "divided by the total number of population units of your own population, unless the number " +
            "is modified by other factors.", 5).disabled());

        settings.add(SettingDef.single("genocide_penalty", "Genocide Penalty",
            "Oh come on! Did you really think your citizens would condone that? For each unit of " +
            "enemy population you kill on any turn, all of your inhabited planets will accrue this " +
            "number of unrest points on the same turn, divided by the total number of population " +
            "units of your own population.", 10).disabled());

        settings.add(SettingDef.single("sympathy_penalty", "Sympathy Penalty",
            "Whenever any of your inhabited planets accrues unrest due to either the consumption " +
            "penalty, the starvation penalty, the labor penalty, the diversity penalty, or any " +
            "combination thereof, each of your other inhabited planets has the potential to accrue " +
            "a share of the same unrest points on the same turn in response. This factor determines " +
            "the baseline share.", 5)
            .withConstraint("Must be ≤ 0.5").disabled());

        settings.add(SettingDef.single("pacification_rate", "Pacification Rate",
            "This is the baseline number of unrest points which will be subtracted from the unrest " +
            "level on any planet on any turn in which its unrest level is above 0.", 2));

        settings.add(SettingDef.single("protests_threshold", "Protests Threshold",
            "This is the unrest level above which a planet will experience protests.", 5));

        settings.add(SettingDef.single("riots_threshold", "Riots Threshold",
            "This is the unrest level above which a planet will experience riots.", 5));

        settings.add(SettingDef.single("terrorism_threshold", "Terrorism Threshold",
            "This is the unrest level above which each of a planet's buildings has a risk of " +
            "randomly exploding on any given turn, beyond merely the normal accident risk.", 5));

        settings.add(SettingDef.single("revolution_threshold", "Revolution Threshold",
            "This is the unrest level above which a planet's government is at risk of being " +
            "overthrown on any given turn.", 5));

        settings.add(SettingDef.oneInPerTurn("revolution_odds", "Revolution Odds",
            "This is the baseline chance of a planet's government being overthrown on any given " +
            "turn in which the unrest level is above the revolution threshold.", 5));

        // ── INFRASTRUCTURE VARIABLES ─────────────────────────────────────────
        settings.add(SettingDef.header("INFRASTRUCTURE VARIABLES",
            "These variables affect the building of infrastructure, the risks to "
            "infrastructure, and the power requirements of that infrastructure."));

        settings.add(SettingDef.single("metals_required", "Metals Required",
            "This is the baseline number of units of metals which will be required to build " +
            "each building.", 10));

        settings.add(SettingDef.single("chemicals_required", "Chemicals Required",
            "This is the baseline number of units of chemicals which will be required to build " +
            "each building.", 10));

        settings.add(SettingDef.single("labor_units_required", "Labor Units Required",
            "This is the baseline number of labor units which will be required to build " +
            "each building.", 10));

        settings.add(SettingDef.single("energy_units_required", "Energy Units Required",
            "This is how many energy units each building needs per working hour in order to " +
            "operate.", 2));

        settings.add(SettingDef.single("degradation_time", "Degradation Time",
            "This is the baseline number of turns required for any given building to accrue one " +
            "degradation point.", 5));

        settings.add(SettingDef.oneInPerTurn("accident_odds", "Accident Odds",
            "This is the baseline chance each building has of randomly exploding on any given turn. " +
            "The actual chance increases with increased degradation points, and is also affected by " +
            "racial traits.", 5));

        settings.add(SettingDef.oneInPerTurn("terrorism_odds", "Terrorism Odds",
            "This is the baseline chance each building has of randomly exploding due to a terrorist " +
            "act on any given turn in which the unrest level on that planet is above the terrorism " +
            "threshold.", 5));

        settings.add(SettingDef.oneIn("collateral_damage_odds", "Collateral Damage Odds",
            "Any time a building randomly explodes, whether due to accident, terrorism or fire, this " +
            "variable determines the odds for another building on the same planet to catch fire. Each " +
            "time a building catches fire, this variable also determines the odds of another building " +
            "on the same planet catching fire on the same turn in response.", 5));

        settings.add(SettingDef.single("building_burn_time", "Building Burn Time",
            "When a building catches fire, it will explode after this many turns unless the fire is " +
            "suppressed.", 2));

        settings.add(SettingDef.single("labor_fire_suppression", "Labor Units For Fire Suppression",
            "When a building is burning, it takes this many labor units from damage control to " +
            "suppress the fire.", 10));

        settings.add(SettingDef.single("degradation_bonus", "Degradation Bonus",
            "When a building has a degradation bonus from being built with gold, the number of turns " +
            "required for it to accrue 1 degradation point is multiplied by this factor.", 5));

        settings.add(SettingDef.single("gold_for_degradation", "Gold Required For Degradation Bonus",
            "This is the baseline number of units of gold required, in addition to the metals and " +
            "chemicals, in order to build a building which has a degradation bonus.", 10));

        // ── GENERATOR VARIABLES ──────────────────────────────────────────────
        settings.add(SettingDef.header("GENERATOR VARIABLES",
            "These variables affect the performance of generators."));

        settings.add(SettingDef.single("capacity_per_metals", "Capacity Units Per Metals Unit",
            "This is the number of units of Chronium and/or Q-ide a generator can process relative " +
            "to the number of metals units used to build it.", 5));

        settings.add(SettingDef.single("spool_up_rate", "Spool Up Rate",
            "This is how many turns are required for the rate at which the generator processes " +
            "Chronium or Q-ide, to increase by 10% as the generator is spooling up, and to decrease " +
            "by 10% as the generator is spooling down.", 2));

        settings.add(SettingDef.single("chronium_energy_ratio", "Chronium To Energy Conversion Ratio",
            "This is the baseline number of units of energy a generator produces each hour for each " +
            "unit of Chronium it processes that hour.", 5)
            .withConstraint("Must be higher than the Q-ide to energy conversion ratio"));

        settings.add(SettingDef.single("qide_energy_ratio", "Q-ide To Energy Conversion Ratio",
            "This is the baseline number of units of energy a generator produces each hour for each " +
            "unit of Q-ide it processes that hour.", 5));

        settings.add(SettingDef.colonRatio("chronium_qide_ratio", "Chronium To Q-ide Ratio",
            "This is the maximum ratio of Chronium to Q-ide a generator can process each hour without " +
            "a risk of exploding due to an excessive concentration of Chronium.", 2, 2));

        // ── CAPACITOR VARIABLES ──────────────────────────────────────────────
        settings.add(SettingDef.header("CAPACITOR VARIABLES"));

        settings.add(SettingDef.single("capacitor_energy_per_metals", "Energy Units Per Metals Unit",
            "This is how many units of energy a capacitor can store safely, relative to the number " +
            "of units of metals the capacitor is made out of.", 5));

        // ── DISCHARGER VARIABLES ─────────────────────────────────────────────
        settings.add(SettingDef.header("DISCHARGER VARIABLES"));

        settings.add(SettingDef.single("discharger_energy_per_metals", "Energy Units Per Metals Unit",
            "This is how many units of energy a discharger can discharge per hour.", 5));
    }

    // ── UI construction ──────────────────────────────────────────────────────

    private void buildUI(LinearLayout container) {

        // ── BACK button ──────────────────────────────────────────────────────
        TextView btnBack = makeMenuButton("◀  BACK");
        btnBack.setOnClickListener(v -> finish());
        container.addView(btnBack, fullWidthMargin(0, 0, 0, dp(10)));

        // ── Page title ───────────────────────────────────────────────────────
        String title = getIntent().getStringExtra("tutorial_title");
        if (title != null && !title.isEmpty()) {
            TextView titleView = new TextView(this);
            titleView.setText(title);
            titleView.setTextColor(color(R.color.title_color));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            titleView.setTypeface(android.graphics.Typeface.MONOSPACE);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(0, dp(6), 0, dp(14));
            container.addView(titleView, fullWidthMargin(0, 0, 0, 0));
        }

        // ── Divider ──────────────────────────────────────────────────────────
        container.addView(makeDivider(), fullWidthMargin(0, 0, 0, dp(12)));

        // ── IMPORT button ────────────────────────────────────────────────────
        TextView btnImport = makeMenuButton("IMPORT GAME SETTINGS");
        btnImport.setOnClickListener(v ->
            importLauncher.launch(new String[]{"*/*"})
        );
        container.addView(btnImport, fullWidthMargin(0, 0, 0, dp(20)));

        // ── Setting rows ─────────────────────────────────────────────────────
        for (SettingDef s : settings) {
            if (s.type == SettingDef.Type.HEADER) {
                container.addView(makeSectionHeader(s), fullWidthMargin(0, dp(16), 0, dp(8)));
            } else {
                container.addView(makeSettingRow(s), fullWidthMargin(0, 0, 0, dp(12)));
            }
        }

        // ── EXPORT button ────────────────────────────────────────────────────
        container.addView(makeDivider(), fullWidthMargin(0, dp(8), 0, dp(16)));
        TextView btnExport = makeMenuButton("EXPORT GAME SETTINGS");
        btnExport.setOnClickListener(v ->
            exportLauncher.launch("game_settings.giegs")
        );
        container.addView(btnExport, fullWidthMargin(0, 0, 0, 0));
    }

    // ── Row builders ─────────────────────────────────────────────────────────

    private View makeSectionHeader(SettingDef s) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(4));

        // ? button — only shown when help text is available
        if (s.help != null && !s.help.isEmpty()) {
            TextView qBtn = makeQButton(s.help);
            row.addView(qBtn, wrapWrapMargin(0, 0, dp(8), 0));
        }

        TextView tv = new TextView(this);
        tv.setText(s.label);
        tv.setTextColor(color(R.color.section_header_color));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.12f);
        row.addView(tv, wrapWrap());

        return row;
    }

    private View makeSettingRow(SettingDef s) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);

        // Row 1: [?] label
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        TextView qBtn = makeQButton(s.help);
        row1.addView(qBtn, wrapWrapMargin(0, 0, dp(8), 0));

        LinearLayout labelCol = new LinearLayout(this);
        labelCol.setOrientation(LinearLayout.VERTICAL);

        TextView labelView = new TextView(this);
        labelView.setText(s.label);
        labelView.setTextColor(color(s.disabled ? R.color.btn_disabled_text : R.color.btn_active_text));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        labelView.setTypeface(android.graphics.Typeface.MONOSPACE);
        labelCol.addView(labelView, wrapWrap());

        if (s.constraint != null) {
            TextView cView = new TextView(this);
            cView.setText("(" + s.constraint + ")");
            cView.setTextColor(color(R.color.constraint_color));
            cView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            cView.setTypeface(android.graphics.Typeface.MONOSPACE);
            labelCol.addView(cView, wrapWrap());
        }

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row1.addView(labelCol, labelParams);
        outer.addView(row1, fullWidth());

        // Row 2: the input field(s), indented to align under the label
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        row2.setPadding(dp(38), dp(6), 0, 0); // align under label (? button width + margin)

        buildFieldRow(row2, s);

        outer.addView(row2, fullWidth());
        return outer;
    }

    private void buildFieldRow(LinearLayout row, SettingDef s) {
        switch (s.type) {
            case SINGLE: {
                EditText et = makeEditText(s.width1, s.disabled);
                fieldMap.put(s.key, et);
                row.addView(et, wrapWrap());
                break;
            }
            case MIN_MAX: {
                row.addView(makeInlineLabel("min"), wrapWrapMargin(0, 0, dp(4), 0));
                EditText etMin = makeEditText(s.width1, s.disabled);
                fieldMap.put(s.key + "_min", etMin);
                row.addView(etMin, wrapWrapMargin(0, 0, dp(10), 0));

                row.addView(makeInlineLabel("max"), wrapWrapMargin(0, 0, dp(4), 0));
                EditText etMax = makeEditText(s.width2, s.disabled);
                fieldMap.put(s.key + "_max", etMax);
                row.addView(etMax, wrapWrap());
                break;
            }
            case ONE_IN_PER_TURN: {
                row.addView(makeInlineLabel("1 in"), wrapWrapMargin(0, 0, dp(4), 0));
                EditText et = makeEditText(s.width1, s.disabled);
                fieldMap.put(s.key, et);
                row.addView(et, wrapWrapMargin(0, 0, dp(4), 0));
                row.addView(makeInlineLabel("per turn"), wrapWrap());
                break;
            }
            case ONE_IN: {
                row.addView(makeInlineLabel("1 in"), wrapWrapMargin(0, 0, dp(4), 0));
                EditText et = makeEditText(s.width1, s.disabled);
                fieldMap.put(s.key, et);
                row.addView(et, wrapWrap());
                break;
            }
            case COLON_RATIO: {
                EditText etA = makeEditText(s.width1, s.disabled);
                fieldMap.put(s.key + "_a", etA);
                row.addView(etA, wrapWrapMargin(0, 0, dp(4), 0));
                row.addView(makeInlineLabel(":"), wrapWrapMargin(0, 0, dp(4), 0));
                EditText etB = makeEditText(s.width2, s.disabled);
                fieldMap.put(s.key + "_b", etB);
                row.addView(etB, wrapWrap());
                break;
            }
            default:
                break;
        }
    }

    // ── Widget factories ─────────────────────────────────────────────────────

    /** Active-styled full-width menu button (BACK, IMPORT, EXPORT). */
    private TextView makeMenuButton(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color(R.color.btn_active_text));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setLetterSpacing(0.10f);
        tv.setAllCaps(true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(12), dp(16), dp(12));
        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_active_bg));
        tv.setClickable(true);
        tv.setFocusable(true);
        return tv;
    }

    /** Small "?" button that opens a help dialog. */
    private TextView makeQButton(String helpText) {
        TextView tv = new TextView(this);
        tv.setText("?");
        tv.setTextColor(color(R.color.btn_active_text));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_active_bg));
        int p = dp(5);
        tv.setPadding(p, p, p, p);
        tv.setMinWidth(dp(26));
        tv.setMinHeight(dp(26));
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setOnClickListener(v -> showHelp(helpText));
        return tv;
    }

    /** Small non-interactive label for field prefixes/suffixes ("min", "max", "1 in", etc.). */
    private TextView makeInlineLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color(R.color.btn_active_text));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        return tv;
    }

    /**
     * Creates an EditText that accepts only digits and decimal points,
     * limited to the given character count, styled to match the dark theme.
     */
    private EditText makeEditText(int maxChars, boolean disabled) {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(maxChars) });
        et.setTextColor(color(disabled ? R.color.btn_disabled_text : R.color.edit_field_text));
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        et.setTypeface(android.graphics.Typeface.MONOSPACE);
        et.setGravity(Gravity.CENTER);
        et.setBackground(ContextCompat.getDrawable(this,
            disabled ? R.drawable.edit_field_disabled_bg : R.drawable.edit_field_bg));
        et.setEnabled(!disabled);
        et.setFocusable(!disabled);

        // Width based on character count (~13dp per character, 8dp min padding each side)
        int widthDp = Math.max(maxChars * 13, 30);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(widthDp), dp(34));
        et.setLayoutParams(lp);
        et.setPadding(dp(4), dp(4), dp(4), dp(4));
        return et;
    }

    private View makeDivider() {
        View v = new View(this);
        v.setBackgroundColor(color(R.color.btn_disabled_border));
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        return v;
    }

    private void showHelp(String text) {
        new AlertDialog.Builder(this)
            .setMessage(text)
            .setPositiveButton("OK", null)
            .show();
    }

    // ── Import / Export ──────────────────────────────────────────────────────

    private void importSettings(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) { toast("Could not open file."); return; }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject json = new JSONObject(sb.toString());
            applyJson(json);
            toast("Settings imported.");
        } catch (IOException | JSONException e) {
            toast("Import failed: " + e.getMessage());
        }
    }

    private void exportSettings(Uri uri) {
        try {
            JSONObject json = collectJson();
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) { toast("Could not open file for writing."); return; }
            os.write(json.toString(2).getBytes("UTF-8"));
            os.close();
            toast("Settings exported as .giegs file.");
        } catch (IOException | JSONException e) {
            toast("Export failed: " + e.getMessage());
        }
    }

    /** Reads all EditText values into a JSONObject. */
    private JSONObject collectJson() throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, EditText> entry : fieldMap.entrySet()) {
            String val = entry.getValue().getText().toString().trim();
            json.put(entry.getKey(), val);
        }
        return json;
    }

    /** Applies a JSONObject's values back into the EditText fields. */
    private void applyJson(JSONObject json) {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            EditText et = fieldMap.get(k);
            if (et != null) {
                try { et.setText(json.getString(k)); } catch (JSONException ignored) {}
            }
        }
    }

    // ── Layout helpers ───────────────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private int color(int resId) {
        return ContextCompat.getColor(this, resId);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fullWidthMargin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = fullWidth();
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrapMargin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = wrapWrap();
        lp.setMargins(l, t, r, b);
        return lp;
    }
}

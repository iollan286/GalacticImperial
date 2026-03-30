package com.galacticimperial;

/**
 * Describes one row in the Game Settings screen.
 * Factory methods build the different row types.
 */
public class SettingDef {

    public enum Type {
        HEADER,          // section title — no fields
        SINGLE,          // label + one input field
        MIN_MAX,         // label + "min" field + "max" field
        ONE_IN_PER_TURN, // label + "1 in" + field + "per turn"
        ONE_IN,          // label + "1 in" + field  (no "per turn")
        COLON_RATIO      // label + field + ":" + field
    }

    /** Unique key used for JSON import/export. MIN_MAX appends _min/_max automatically. */
    public String key;
    /** Text shown as the row label. */
    public String label;
    /** Text shown in the ? popup. */
    public String help;
    public Type type;
    /** Max character count for the first (or only) input field. */
    public int width1;
    /** Max character count for the second input field (MIN_MAX and COLON_RATIO only). */
    public int width2;
    /** If true, input fields are greyed out and not editable. */
    public boolean disabled;
    /** Optional note shown in small text below the label, e.g. "(Must be > 0 and ≤ 1)". */
    public String constraint;

    private SettingDef() {}

    // ── Factory methods ──────────────────────────────────────────────────────

    public static SettingDef header(String label) {
        SettingDef d = new SettingDef();
        d.type = Type.HEADER;
        d.label = label;
        return d;
    }

    public static SettingDef single(String key, String label, String help, int width) {
        SettingDef d = new SettingDef();
        d.type = Type.SINGLE;
        d.key = key; d.label = label; d.help = help; d.width1 = width;
        return d;
    }

    public static SettingDef minMax(String key, String label, String help, int w1, int w2) {
        SettingDef d = new SettingDef();
        d.type = Type.MIN_MAX;
        d.key = key; d.label = label; d.help = help; d.width1 = w1; d.width2 = w2;
        return d;
    }

    public static SettingDef oneInPerTurn(String key, String label, String help, int width) {
        SettingDef d = new SettingDef();
        d.type = Type.ONE_IN_PER_TURN;
        d.key = key; d.label = label; d.help = help; d.width1 = width;
        return d;
    }

    public static SettingDef oneIn(String key, String label, String help, int width) {
        SettingDef d = new SettingDef();
        d.type = Type.ONE_IN;
        d.key = key; d.label = label; d.help = help; d.width1 = width;
        return d;
    }

    public static SettingDef colonRatio(String key, String label, String help, int w1, int w2) {
        SettingDef d = new SettingDef();
        d.type = Type.COLON_RATIO;
        d.key = key; d.label = label; d.help = help; d.width1 = w1; d.width2 = w2;
        return d;
    }

    /** Chains a constraint note onto any SettingDef. */
    public SettingDef withConstraint(String c) {
        this.constraint = c;
        return this;
    }

    /** Marks this setting's input fields as disabled (greyed out). */
    public SettingDef disabled() {
        this.disabled = true;
        return this;
    }
}

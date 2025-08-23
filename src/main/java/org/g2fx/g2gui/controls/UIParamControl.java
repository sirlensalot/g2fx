package org.g2fx.g2gui.controls;

public sealed interface UIParamControl extends UIElement permits
        UIElements.ButtonFlat,
        UIElements.ButtonIncDec,
        UIElements.ButtonRadio,
        UIElements.ButtonRadioEdit,
        UIElements.ButtonText,
        UIElements.Knob,
        UIElements.LevelShift,
        UIElements.TextEdit {
    Integer CodeRef();
    String Control();
}

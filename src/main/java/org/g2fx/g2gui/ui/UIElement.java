package org.g2fx.g2gui.ui;

public sealed interface UIElement permits
        UIElements.Bitmap
        , UIElements.ButtonFlat
        , UIElements.ButtonIncDec
        , UIElements.ButtonRadio
        , UIElements.ButtonRadioEdit
        , UIElements.ButtonText
        , UIElements.Graph
        , UIElements.Input
        , UIElements.Knob
        , UIElements.Led
        , UIElements.LevelShift
        , UIElements.Line
        , UIElements.MiniVU
        , UIElements.Output
        , UIElements.PartSelector
        , UIElements.Symbol
        , UIElements.Text
        , UIElements.TextEdit
        , UIElements.TextField
        , UIParamControl
{

    Integer ID();
    Integer XPos();
    Integer YPos();
    UIElements.ElementType elementType();
}

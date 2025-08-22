package org.g2fx.g2gui.controls;

import java.util.List;

public class UIElements {

    public enum ButtonType {
        Push, Check
    }

    public enum Orientation {
        Horizontal, Vertical
    }

    public enum KnobType {
        Big(true,false),
        Medium(true,false),
        Reset(true,true),
        ResetMedium(true,true),
        SeqSlider(false,false),
        Slider(false,false),
        Small(true,false);
        public final boolean isKnob;
        public final boolean isReset;
        KnobType(boolean isKnob, boolean isReset) {
            this.isKnob = isKnob;
            this.isReset = isReset;
        }
    }

    public enum ElementType {
        Bitmap { @Override public Class<? extends UIElement> getType() { return Bitmap.class; } }
      , ButtonFlat { @Override public Class<? extends UIElement> getType() { return ButtonFlat.class; } }
      , ButtonIncDec { @Override public Class<? extends UIElement> getType() { return ButtonIncDec.class; } }
      , ButtonRadio { @Override public Class<? extends UIElement> getType() { return ButtonRadio.class; } }
      , ButtonRadioEdit { @Override public Class<? extends UIElement> getType() { return ButtonRadioEdit.class; } }
      , ButtonText { @Override public Class<? extends UIElement> getType() { return ButtonText.class; } }
      , Input { @Override public Class<? extends UIElement> getType() { return Input.class; } }
      , Knob { @Override public Class<? extends UIElement> getType() { return Knob.class; } }
      , Led { @Override public Class<? extends UIElement> getType() { return Led.class; } }
      , LevelShift { @Override public Class<? extends UIElement> getType() { return LevelShift.class; } }
      , Line { @Override public Class<? extends UIElement> getType() { return Line.class; } }
      , MiniVU { @Override public Class<? extends UIElement> getType() { return MiniVU.class; } }
      , Output { @Override public Class<? extends UIElement> getType() { return Output.class; } }
      , PartSelector { @Override public Class<? extends UIElement> getType() { return PartSelector.class; } }
      , Symbol { @Override public Class<? extends UIElement> getType() { return Symbol.class; } }
      , Text { @Override public Class<? extends UIElement> getType() { return Text.class; } }
      , TextEdit { @Override public Class<? extends UIElement> getType() { return TextEdit.class; } }
      //dependencies sort last
      , TextField { @Override public Class<? extends UIElement> getType() { return TextField.class; } }
      , Graph { @Override public Class<? extends UIElement> getType() { return Graph.class; } }
      ;
        public abstract Class<? extends UIElement> getType();
    }


    public record Bitmap (
        Boolean CustomText
      , Integer Height
      , Integer ID
      , String ImageFile
      , String Text
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement {
        @Override public ElementType elementType() { return ElementType.Bitmap; }
    }

    public record ButtonFlat (
        Integer CodeRef
      , String Control
      , Integer ID
      , Integer ImageCount
      , Integer ImageWidth
      , List<String> Images
      , Integer InfoFunc
      , String Text
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.ButtonFlat; }
    }

    public record ButtonIncDec (
        Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , String Type
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.ButtonIncDec; }
    }

    public record ButtonRadio (
        Integer ButtonCount
      , Integer ButtonWidth
      , Integer CodeRef
      , String Control
      , Integer ID
      , Integer ImageWidth
      , List<String> Images
      , Integer InfoFunc
      , Orientation Orientation
      , String Text
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.ButtonRadio; }
    }

    public record ButtonRadioEdit (
        Integer ButtonColumns
      , Integer ButtonRows
      , Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , String Text
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.ButtonRadioEdit; }
    }

    public record ButtonText (
        Integer CodeRef
      , String Control
      , Integer ID
      , Integer ImageWidth
      , List<String> Images
      , Integer InfoFunc
      , String Text
      , ButtonType Type
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.ButtonText; }
    }

    public record Graph (
        String Dependencies
      , Integer GraphFunc
      , Integer Height
      , Integer ID
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement {
        @Override public ElementType elementType() { return ElementType.Graph; }
    }

    public record Input (
        String Bandwidth
      , Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , String Type
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.Input; }
    }

    public record Knob (
        Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , KnobType Type
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.Knob; }
    }

    public record Led (
        Integer CodeRef
      , String Control
      , Integer GroupId
      , Integer ID
      , Integer InfoFunc
      , Boolean LedGroup
      , String Type
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.Led; }
    }

    public record LevelShift (
        Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.LevelShift; }
    }

    public record Line (
        Integer ID
      , Integer Length
      , Orientation Orientation
      , String Weight
      , Integer XPos
      , Integer YPos
    ) implements UIElement {
        @Override public ElementType elementType() { return ElementType.Line; }
    }

    public record MiniVU (
        Integer CodeRef
      , String Control
      , Integer GroupId
      , Integer ID
      , Integer InfoFunc
      , Orientation Orientation
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.MiniVU; }
    }

    public record Output (
        String Bandwidth
      , Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , String Type
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.Output; }
    }

    public record PartSelector (
        Integer CodeRef
      , String Control
      , Integer Height
      , Integer ID
      , Integer ImageCount
      , Integer ImageWidth
      , List<String> Images
      , Integer InfoFunc
      , Integer MenuOffset
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.PartSelector; }
    }

    public record Symbol (
        Integer Height
      , Integer ID
      , String Type
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement {
        @Override public ElementType elementType() { return ElementType.Symbol; }
    }

    public record Text (
        Integer ID
      , String Text
      , Integer XPos
      , Integer YPos
    ) implements UIElement {
        @Override public ElementType elementType() { return ElementType.Text; }
    }

    public record TextEdit (
        Integer CodeRef
      , String Control
      , Integer ID
      , Integer InfoFunc
      , String Text
      , ButtonType Type
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement, UIControl {
        @Override public ElementType elementType() { return ElementType.TextEdit; }
    }

    public record TextField (
        String Dependencies
      , Integer ID
      , Integer MasterRef
      , Integer TextFunc
      , Integer Width
      , Integer XPos
      , Integer YPos
    ) implements UIElement {
        @Override public ElementType elementType() { return ElementType.TextField; }
    }
}

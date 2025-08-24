package org.g2fx.g2gui.controls;

import javafx.scene.image.ImageView;
import org.g2fx.g2gui.FXUtil;

import java.io.File;
import java.util.List;

public sealed interface TextOrImage permits TextOrImage.IsText, TextOrImage.IsImage {

    default boolean isText() { return false; }
    default boolean isImage() { return false; }

    record IsText(String text) implements TextOrImage {
        @Override
        public boolean isText() {
            return true;
        }
    }

    record IsImage(ImageView image) implements TextOrImage {
        @Override
        public boolean isImage() {
            return true;
        }
    }

    static List<TextOrImage> mkTexts(List<String> ss) {
        return ss.stream().map(s -> (TextOrImage) new IsText(s)).toList();
    }
    static List<TextOrImage> mkImages(List<String> files) {
        return files.stream().map(s -> (TextOrImage) new IsImage(FXUtil.getImageResource(
             "img" + File.separator + s))).toList();
    }

}

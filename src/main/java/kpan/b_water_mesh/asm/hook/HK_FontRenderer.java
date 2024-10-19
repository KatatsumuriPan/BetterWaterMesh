package kpan.b_water_mesh.asm.hook;

import kpan.b_water_mesh.asm.acc.ACC_FontRenderer;
import net.minecraft.client.gui.FontRenderer;

public class HK_FontRenderer {

    public static char onRenderChar(FontRenderer fontRenderer, char ch) {
        if (ch >= '0' && ch <= '9') {
            int color_index = ch - '0';
            //影の色まで書き換えるからすごく見づらい
            ((ACC_FontRenderer) fontRenderer).setColor(color_index);
        }
        if (ACC_FontRenderer.isFormatSpecial(ch))
            return Character.toUpperCase(ch);
        return ch;
    }
}

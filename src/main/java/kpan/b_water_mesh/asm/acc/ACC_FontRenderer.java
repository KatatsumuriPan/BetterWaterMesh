package kpan.b_water_mesh.asm.acc;

import kpan.b_water_mesh.asm.core.adapters.MixinAccessorAdapter.NewMethod;

public interface ACC_FontRenderer {

    // メソッドアクセスの使用例

    // インスタンスメソッドへのアクセス
    // この例ではsrg名が無いメソッドを対象にしている
    void setColor(float r, float g, float b, float a);

    // staticメソッドへのアクセス
    static boolean isFormatSpecial(char formatChar) { throw new AssertionError(); }

    // インスタンスメソッドの追加
    // オーバーライドしたければReplaceRefMethodAdapterを使って上書き
    @NewMethod
    default void setColor(int colorIndex) {
        int i = (colorIndex >> 3 & 1) * 85;
        int r = (colorIndex >> 2 & 1) * 170 + i;
        int g = (colorIndex >> 1 & 1) * 170 + i;
        int b = (colorIndex >> 0 & 1) * 170 + i;
        setColor(r / 255f, g / 255f, b / 255f, 1); // これ自体は外から呼んでも良い
    }

    // staticメソッドの追加はできない
    // そもそもこのクラスにstaticメソッドを書けばよい
}

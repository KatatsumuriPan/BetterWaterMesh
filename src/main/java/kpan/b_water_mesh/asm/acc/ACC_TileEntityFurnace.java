package kpan.b_water_mesh.asm.acc;

import kpan.b_water_mesh.asm.core.adapters.MixinAccessorAdapter.NewField;

public interface ACC_TileEntityFurnace {

    // フィールドアクセスの使用例

    // インスタンスフィールドのgetter
    int get_cookTime();

    // インスタンスフィールドのsetter
    void set_furnaceCustomName(String value);

    // 新しいインスタンスフィールドの追加&getter作成
    // getterとsetterの両方を作成する必要はないが、初期化する方法が無いので両方作るのが基本
    //@NewFieldはgetterとsetterの両方に必要
    @NewField
    int get_openCount();

    // 新しいインスタンスフィールドの追加&setter作成
    // 説明はgetter同様
    @NewField
    void set_openCount(int value);

    // staticフィールドのgetter
    // インスタンスフィールドの場合と同様
    // setterも同様にして作れる
    static int[] get_SLOTS_BOTTOM() { throw new AssertionError(); }

    // 新しいstaticフィールドの追加&getter作成
    // インスタンスフィールドの場合と同様
    @NewField
    static int get_openCountStatic() { throw new AssertionError(); }

    // 新しいstaticフィールドの追加&setter作成
    // インスタンスフィールドの場合と同様
    @NewField
    static void set_openCountStatic(int value) { throw new AssertionError(); }

}

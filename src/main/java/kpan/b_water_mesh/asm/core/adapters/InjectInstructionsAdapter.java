package kpan.b_water_mesh.asm.core.adapters;

import java.util.ArrayList;
import java.util.function.Function;
import kpan.b_water_mesh.asm.core.adapters.Instructions.Instr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@SuppressWarnings("unused")
public class InjectInstructionsAdapter extends ReplaceInstructionsAdapter {

    private final int injectIndex;

    public InjectInstructionsAdapter(MethodVisitor mv, String nameForDebug, Instructions targets, Instructions instructions, int injectIndex) {
        super(mv, nameForDebug, targets, instructions);
        if (injectIndex < 0)
            injectIndex = targets.size() + injectIndex + 1;
        this.injectIndex = injectIndex;
    }

    public InjectInstructionsAdapter(MethodVisitor mv, String nameForDebug, Instructions targets, Function<ArrayList<Instr>, Instructions> instructionFactory, int injectIndex) {
        super(mv, nameForDebug, targets, instructionFactory);
        if (injectIndex < 0)
            injectIndex = targets.size() + injectIndex + 1;
        this.injectIndex = injectIndex;
    }

    @Override
    protected void visitAllInstructions() {
        for (int i = 0; i < holds.size(); i++) {
            if (i == injectIndex)
                super.visitAllInstructions();
            holds.get(i).visit(mv, this);
        }
        if (injectIndex >= holds.size())
            super.visitAllInstructions();
    }

    public static InjectInstructionsAdapter before(MethodVisitor mv, String nameForDebug, Instructions targets, Instructions instructions) {
        return new InjectInstructionsAdapter(mv, nameForDebug, targets, instructions, 0);
    }
    public static InjectInstructionsAdapter before(MethodVisitor mv, String nameForDebug, Instructions targets, Function<ArrayList<Instr>, Instructions> instructionFactory) {
        return new InjectInstructionsAdapter(mv, nameForDebug, targets, instructionFactory, 0);
    }

    public static InjectInstructionsAdapter after(MethodVisitor mv, String nameForDebug, Instructions targets, Instructions instructions) {
        return new InjectInstructionsAdapter(mv, nameForDebug, targets, instructions, -1);
    }
    public static InjectInstructionsAdapter after(MethodVisitor mv, String nameForDebug, Instructions targets, Function<ArrayList<Instr>, Instructions> instructionFactory) {
        return new InjectInstructionsAdapter(mv, nameForDebug, targets, instructionFactory, -1);
    }

    public static ReplaceInstructionsAdapter beforeAfter(MethodVisitor mv, String nameForDebug, Instructions targets, Instructions before, Instructions after) {
        return new InjectInstructionsAdapter(mv, nameForDebug, targets, before, 0) {
            @Override
            protected void visitAllInstructions() {
                super.visitAllInstructions();
                for (Instr instruction : after) {
                    instruction.visit(mv, this);
                }

            }
        };
    }
    public static MethodVisitor injectFirst(MethodVisitor mv, String nameForDebug, final Instructions instructions) {
        return new MyMethodVisitor(mv, nameForDebug) {
            @Override
            public void visitCode() {
                super.visitCode();
                for (Instr instruction : instructions) {
                    instruction.visit(mv, this);
                }
                success();
            }
        };
    }
    public static MethodVisitor injectBeforeReturns(MethodVisitor mv, String nameForDebug, final Instructions instructions) {
        return new MyMethodVisitor(mv, nameForDebug) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
                    for (Instr instruction : instructions) {
                        instruction.visit(mv, this);
                    }
                    success();
                }
                super.visitInsn(opcode);
            }

            @Override
            public void visitEnd() {
                if (mv != null) {
                    mv.visitEnd();
                }
            }
        };
    }
}

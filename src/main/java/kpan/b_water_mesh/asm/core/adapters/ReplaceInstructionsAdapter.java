package kpan.b_water_mesh.asm.core.adapters;

import java.util.ArrayList;
import java.util.function.Function;
import javax.annotation.Nonnull;
import kpan.b_water_mesh.asm.core.AsmNameRemapper;
import kpan.b_water_mesh.asm.core.adapters.Instructions.Instr;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class ReplaceInstructionsAdapter extends MyMethodVisitor {

    protected final Instructions targets;
    protected final Function<ArrayList<Instr>, Instructions> instructionFactory;
    protected final ArrayList<Instr> holds = new ArrayList<>();

    private int maxMatched = 0;

    public ReplaceInstructionsAdapter(@Nonnull MethodVisitor mv, String nameForDebug, Instructions targets, Instructions instructions) {
        this(mv, nameForDebug, targets, instrs -> instructions);
    }

    public ReplaceInstructionsAdapter(@Nonnull MethodVisitor mv, String nameForDebug, Instructions targets, Function<ArrayList<Instr>, Instructions> instructionFactory) {
        super(mv, nameForDebug);
        this.targets = targets;
        this.instructionFactory = instructionFactory;
    }

    protected final boolean check(Instr instr) {
        Instr instr2 = targets.get(holds.size());
        instr2.solveLabel(this);
        holds.add(instr);
        if (instr.equals(instr2)) {
            if (holds.size() == targets.size()) {
                visitAllInstructions();
                holds.clear();
                success();
            }
            return true;
        } else {
            for (int start = 1; start < holds.size(); start++) {
                if (extracted(start)) {
                    for (int i = 0; i < start; i++) {
                        holds.get(i).visit(mv, this);
                    }
                    holds.subList(0, start).clear();
                    return true;
                }
            }
            holds.remove(holds.size() - 1);
            flushVisits();
            return false;
        }
    }

    private boolean extracted(int start) {
        for (int i = 0; i < holds.size() - start; i++) {
            Instr instr = holds.get(start + i);
            if (!instr.equals(targets.get(i)))
                return false;
        }
        return true;
    }

    protected void visitAllInstructions() {
        for (Instr instruction : instructionFactory.apply(holds)) {
            instruction.visit(mv, this);
        }
    }

    protected final void flushVisits() {
        maxMatched = Math.max(maxMatched, holds.size());
        for (Instr element : holds) {
            element.visit(mv, this);
        }
        holds.clear();
    }

    @Override
    public void visitEnd() {
        flushVisits();
        try {
            super.visitEnd();
        } catch (Exception e) {
            System.out.println("maxMatch:" + maxMatched);
            throw e;
        }
    }
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!check(Instr.fieldInsn(opcode, owner, AsmNameRemapper.runtime2McpFieldName(name), desc)))
            super.visitFieldInsn(opcode, owner, name, desc);
    }
    @Override
    public void visitIincInsn(int var, int increment) {
        if (!check(Instr.iincInsn(var, increment)))
            super.visitIincInsn(var, increment);
    }
    @Override
    public void visitInsn(int opcode) {
        if (!check(Instr.insn(opcode)))
            super.visitInsn(opcode);
    }
    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (!check(Instr.intInsn(opcode, operand)))
            super.visitIntInsn(opcode, operand);
    }
    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        if (!check(Instr.dynamicInsn(name, desc, bsm, bsmArgs)))
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }
    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateLabels(label);
        if (!check(Instr.jumpInsn(opcode, label))) {
            if (mv != null)
                mv.visitJumpInsn(opcode, label);
        }
    }
    @Override
    public void visitLabel(Label label) {
        updateLabels(label);
        if (!check(Instr.label(label))) {
            if (mv != null)
                super.visitLabel(label);
        }
    }
    @Override
    public void visitLdcInsn(Object cst) {
        if (!check(Instr.ldcInsn(cst)))
            super.visitLdcInsn(cst);
    }
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        // これは最後のLOCALVARIABLEで呼ばれる
        flushVisits();
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }
    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        if (!check(Instr.lookupSwicthInsn(dflt, keys, labels)))
            super.visitLookupSwitchInsn(dflt, keys, labels);
    }
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (!check(Instr.methodInsn(opcode, owner, name, desc, itf)))
            super.visitMethodInsn(opcode, owner, name, desc, itf);
    }
    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        flushVisits();
        super.visitMultiANewArrayInsn(desc, dims);
    }
    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        flushVisits();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }
    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        flushVisits();
        super.visitTryCatchBlock(start, end, handler, type);
    }
    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (!check(Instr.typeInsn(opcode, type)))
            super.visitTypeInsn(opcode, type);
    }
    @Override
    public void visitVarInsn(int opcode, int var) {
        if (!check(Instr.varInsn(opcode, var)))
            super.visitVarInsn(opcode, var);
    }

}

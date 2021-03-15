package org.jdiscript.example;

import static org.jdiscript.util.Utils.unchecked;

import org.jdiscript.util.VMSocketAttacher;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.IntegerValue;
import org.jdiscript.JDIScript;
import org.jdiscript.handlers.OnBreakpoint;
import org.jdiscript.handlers.OnVMStart;

/*
 * Usage: start lr4j_replay on a recording of the Cache demo:
 *      lr4j_replay --input runCacheTest.undo --port 9000
 * then:
 *      java -cp example/build/libs/example-0.9.0.jar:jdiscript/build/libs/jdiscript-0.9.0.jar:$JAVA_HOME/../lib/tools.jar org.jdiscript.example.CacheTest
 */

public class CacheTest {
    static int numChecked;

    public static void main(String[] args) throws Exception {
        VirtualMachine vm = new VMSocketAttacher(9000).attach();
        JDIScript j = new JDIScript(vm);

        OnBreakpoint breakpoint = be -> {
            unchecked(() -> {
                ThreadReference thread = be.thread();
                StackFrame f = thread.frame(0);
                int number = ((IntegerValue)f.getValue(f.visibleVariableByName("number"))).value();
                int sqroot = ((IntegerValue)f.getValue(f.visibleVariableByName("sqroot"))).value();
                int sqrootCorrect = (int) Math.sqrt(number);
                if (sqroot != sqrootCorrect) {
                    System.out.println("number = " + number + ", sqroot = " + sqroot + ", sqrootCorrect = " + sqrootCorrect);
                }
                if ((++numChecked % 32) == 0) {
                    System.out.println("number checked = " + numChecked);
                }
            });
        };

        OnVMStart start = se -> {
            j.onClassPrep("io.undo.test.Cache", p -> {
                if (p.referenceType().name().equals("io.undo.test.Cache")) {
                    unchecked(() -> {
                        p.referenceType().locationsOfLine(41).forEach(l -> {
                            j.breakpointRequest(l, breakpoint).enable();
                        });                
                    });
                }
            });
        };         

        j.run(start);
    }
}

package com.github.open96.jypm.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ProcessWrapperTest {
    private static final Runtime RUNTIME = Runtime.getRuntime();

    @Test
    public void testGetProcessOutput() {
        //Create simple process and check it's output
        String command[] = {"echo", "This is a test"};
        checkIfOutputMatchesInput(command);
    }

    @Test
    public void testGetProcessOutputWithMostOfUTF8Characters() {
        //Create simple process and check it's output
        String command[] = {"echo", "“ ” ‘ ’ « » … ° © ® ™ • ½ ¼ ¾ ⅓ ⅔ † ‡ µ ¢ £ € ♠ ♣ ♥ ♦ ✓ ✨ � × ÷ ± ∞ π ∅ ≤ ≥ ≠ ≈" +
                " ∧ ∨ ∩ ∪ ∈ ∀ ∃ ∄ ∑ ∏ ← ↑ → ↓ ↔ ↕ ↖ ↗ ↘ ↙ ↺ ↻ ⇒ ⇔ ⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁽ ⁾ ⁿ ⁱ ₀ ₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈" +
                " ₉ ₊ ₋ ₌ ₍ ₎ Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω α β γ δ ε ζ η θ ι κ λ μ ν ξ ο π ρ σ τ " +
                "υ φ χ ψ ω — – ‐ ‒ −"};
        checkIfOutputMatchesInput(command);
    }

    private void checkIfOutputMatchesInput(String input[]) {
        try {
            Process simpleProcessWithSimpleOutput = RUNTIME.exec(input);
            ProcessWrapper processWrapper = new ProcessWrapper(simpleProcessWithSimpleOutput);
            String output = processWrapper.getProcessOutput();
            assertEquals(input[1], output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

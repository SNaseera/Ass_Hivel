import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern; // Used for simple hex detection

public class PolynomialSolver {

    // Regex pattern to detect non-digit characters a-f, suggesting a hex exponent.
    private static final Pattern HEX_PATTERN = Pattern.compile(".*[a-fA-F].*");

    // --- Utility Function for BigInt Power ---

    /**
     * Calculates base^exponent using BigInteger.
     * This iteration-based method ensures safety even for massive BigInteger exponents.
     * @param base The BigInteger base.
     * @param exponent The BigInteger exponent.
     * @return The result (base^exponent) as a BigInteger.
     */
    private static BigInteger bigIntPower(BigInteger base, BigInteger exponent) {
        BigInteger result = BigInteger.ONE;
        BigInteger one = BigInteger.ONE;
        BigInteger zero = BigInteger.ZERO;
        
        // Loop 'exponent' number of times
        for (BigInteger i = zero; i.compareTo(exponent) < 0; i = i.add(one)) {
            result = result.multiply(base);
        }
        return result;
    }

    // --- Step 1 & 2: Read Input and Decode Roots ---

    /**
     * Reads the JSON input, processes the roots, and decodes the Y values (Base^Value).
     * Importantly, it checks if the 'value' string is hexadecimal (Base 16) before conversion.
     * @param filePath Path to the input.json file.
     * @return A list of Root objects containing decoded (x, y) pairs.
     * @throws IOException if file reading/parsing fails.
     */
    private static List<Root> decodeRoots(String filePath) throws IOException {
        System.out.println("--- Step 1 & 2: Reading Input and Decoding Y Values ---");
        
        ObjectMapper mapper = new ObjectMapper();
        // Read the entire JSON file into a Map structure to avoid hard-coding keys.
        Map<String, Object> jsonData = mapper.readValue(new File(filePath), Map.class);
        
        List<Root> roots = new ArrayList<>();

        for (Map.Entry<String, Object> entry : jsonData.entrySet()) {
            String key = entry.getKey();
            
            // Skip the metadata block
            if (key.equals("keys")) continue;

            // X value is the map key, converted to BigInteger
            BigInteger x = new BigInteger(key);
            
            @SuppressWarnings("unchecked")
            Map<String, String> encodedMap = (Map<String, String>) entry.getValue();
            
            String baseStr = encodedMap.get("base").replace("^", "");
            String valueStr = encodedMap.get("value");

            BigInteger base = new BigInteger(baseStr);
            BigInteger value;
            
            // Critical: Check for hexadecimal characters (a-f) in the exponent/value string.
            if (HEX_PATTERN.matcher(valueStr).matches()) {
                // If hex detected, convert using Base 16.
                value = new BigInteger(valueStr, 16); 
                System.out.println(String.format("Decoding Root X=%s: Hex value '%s' converted to exponent %s", x, valueStr, value.toString()));
            } else {
                // Otherwise, assume standard Base 10.
                value = new BigInteger(valueStr);
            }

            // Decode Y: Y = Base^Value
            BigInteger y = bigIntPower(base, value);

            System.out.println(String.format("Decoded Point: (%s, %s)", x, y.toString()));
            roots.add(new Root(x, y));
        }
        return roots;
    }

    // --- Step 3: Compute Constant C (f(0)) ---

    /**
     * Computes the constant term C (which is f(0)) using Lagrange Interpolation.
     * Formula: C = f(0) = Sum [ y_j * Product_{i!=j} (-x_i) / Product_{i!=j} (x_j - x_i) ]
     * @param roots The list of decoded roots (x, y).
     * @return The constant term C as a BigInteger.
     */
    private static BigInteger computeConstantC(List<Root> roots) {
        System.out.println("\n--- Step 3: Calculating Constant C (f(0)) via Lagrange Interpolation ---");
        
        BigInteger C = BigInteger.ZERO;

        for (int j = 0; j < roots.size(); j++) {
            Root root_j = roots.get(j);
            BigInteger x_j = root_j.x;
            BigInteger y_j = root_j.y;
            
            BigInteger L_j_of_0_numerator = BigInteger.ONE;
            BigInteger L_j_of_0_denominator = BigInteger.ONE;

            // Calculate the numerator and denominator for the Lagrange basis polynomial L_j(0)
            for (int i = 0; i < roots.size(); i++) {
                if (i != j) {
                    BigInteger x_i = roots.get(i).x;
                    
                    // Numerator: Product of (-x_i)
                    L_j_of_0_numerator = L_j_of_0_numerator.multiply(x_i.negate()); 
                    
                    // Denominator: Product of (x_j - x_i)
                    BigInteger difference = x_j.subtract(x_i);
                    L_j_of_0_denominator = L_j_of_0_denominator.multiply(difference);
                }
            }

            // Calculate the full term for the sum: (y_j * Numerator) / Denominator
            BigInteger termNumerator = y_j.multiply(L_j_of_0_numerator);
            
            // Perform the BigInteger division
            BigInteger term = termNumerator.divide(L_j_of_0_denominator);
            
            C = C.add(term); // Accumulate the result for C
        }
        
        return C;
    }

    // --- Main Execution ---

    public static void main(String[] args) {
        // The JSON file must be in the same directory as the compiled classes.
        final String filePath = "input.json"; 

        try {
            List<Root> decodedRoots = decodeRoots(filePath);
            
            if (decodedRoots.isEmpty()) {
                System.out.println("No roots found. Exiting.");
                return;
            }

            // Compute the constant term C
            BigInteger constant_C = computeConstantC(decodedRoots);
            
            // Final Step: Print C
            System.out.println("\n--- Final Answer ---");
            System.out.println("The Constant value C (f(0)) is:");
            // Print the final answer, as requested by the assignment.
            System.out.println(constant_C.toString()); 
            
        } catch (IOException e) {
            System.err.println("\nERROR: Could not read or parse JSON file.");
            System.err.println("Ensure 'input.json' exists and the Jackson library is correctly included.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\nAn unexpected error occurred during computation:");
            e.printStackTrace();
        }
    }
}
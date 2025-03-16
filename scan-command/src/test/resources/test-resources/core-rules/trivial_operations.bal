function testOperators() {
    int x = 1;
    float y = 2.3f;
    boolean result;

    // Arithmetic operators
    result = x < int:MIN_VALUE; // warning
    result = x >= int:MIN_VALUE; // warning

    result = x <= int:MAX_VALUE; // warning
    result = x > int:MAX_VALUE; // warning

    result = y <= float:Infinity; // warning
    result = y > float:Infinity; // warning

    // Logical operators
    boolean b = true;
    result = b && false; // warning
    result = b || true; // warning
    result = b && true; // warning
    result = b || false; // warning

    // Bitwise operators
    int result2 = x | 0;// warning
    result2 = x & -1; // warning
    result2 = x | -1;// warning
    result2 = x & 0; // warning
}

function testRelationaOperatorsWithoutWarnings() {
    string x = "1";
    string y = "2.3f";
    boolean result;

    // Arithmetic operators
    result = x < "int:MIN_VALUE";
    result = x >= "int:MIN_VALUE";

    result = x <= "int:MAX_VALUE";
    result = x > "int:MAX_VALUE";

    result = y <= "float:Infinity";
    result = y > "float:Infinity";

    // Logical operators
    boolean 'false = false;
    boolean 'true = true;
    boolean b = true;

    result = b && 'false;
    result = b || 'true;
    result = b && 'true;
    result = b || 'false;

    // Bitwise operators
    int '0 = 0;
    int \-1 = -1;
    int x2 = 1;

    int result2 = x2 | '0;
    result2 = x2 & \-1;
    result2 = x2 | \-1;
    result2 = x2 & '0;
    result2 = x2 | 1;
    result2 = x2 & 1;
}

function testRelationaOperatorsWithoutWarnings2() {
    int x = 1;
    float y = 2.3f;
    boolean result;

    // Arithmetic operators
    result = x > int:MIN_VALUE;
    result = x <= int:MIN_VALUE;

    result = x >= int:MAX_VALUE;
    result = x < int:MAX_VALUE;

    result = y >= float:Infinity;
    result = y < float:Infinity;

    // Bitwise operators
    int result2 = x | 1;
    result2 = x & --1;
}

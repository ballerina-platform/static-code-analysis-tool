function testRangeOperator() {
    foreach int i in 0...10 {
        // code
    }

    foreach int i in -5...-2 {
        // code
    }

    foreach int i in 0...-1 { // warning
        // code
    }

    foreach int i in 10...9 { // warning
            // code
    }

    foreach int i in -2...-3 { // warning
            // code
    }

    foreach int i in 0...0 {
        // code
    }

    foreach int i in 10...10 {
        // code
    }

    foreach int i in -2...-2 {
        // code
    }

    foreach int i in 0..<10 {
        // code
    }

    foreach int i in -5..<-2 {
        // code
    }

    foreach int i in 0..<-1 { // warning
        // code
    }

    foreach int i in 10..<9 { // warning
            // code
    }

    foreach int i in -2..<-3 { // warning
            // code
    }

    foreach int i in 0..<0 { // warning
        // code
    }

    foreach int i in 10..<10 { // warning
        // code
    }

    foreach int i in -2..<-2 { // warning
        // code
    }
}

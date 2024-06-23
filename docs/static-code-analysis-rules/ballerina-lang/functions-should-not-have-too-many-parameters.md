# Rule definition

- Rule Title: `Functions should not have too many parameters`
- Rule ID: `undefined`
- Rule Kind: `CODE_SMELL`
- Affecting Modules: `['Ballerina/lang']`

### Why is this an issue?

Functions with lengthy parameter lists can be challenging to utilize, potentially compromising code readability and increasing the likelihood of errors.

### Non-compliant Code Example:

```ballerina
// Noncompliant
public function setCoordinates(int x1, int y1, int x2, int y2, int x3, int y3, int width, int height, int depth) returns int {
    // ...
}

public function main() {
    int result = setCoordinates(1, 1, 2, 2, 1, 2, 3, 3, 3);
    // ...
}
```

### The solution can be to:

- Split the function into smaller ones.

```ballerina
public function setOrigin1(int x1, int y1) returns int {
    // ...
}

public function setOrigin2(int x2, int y2) returns int {
    // ...
}

public function setOrigin3(int x3, int y3) returns int {
    // ...
}

public function setSize(int shapeID, int width, int height, int depth) returns int {
    // ...
}
```

- Use record-typed parameters that group data in a way that makes sense for the specific application domain.

```ballerina
public type Point record {|
    int x;
    int y;
|};

public type ShapeProperties record {|
    int width;
    int height;
    int depth;
|};

public function setCoordinates(Point p1, Point p2, Point p3) returns int {
    // ...
}

public function setSize(int shapeID, ShapeProperties properties) returns int {
    // ...
}

public function main() {
    Point p1 = {x: 1, y: 1};
    Point p2 = {x: 2, y: 2};
    Point p3 = {x: 1, y: 2};

    int shapeID = setCoordinates(p1, p2, p3);

    ShapeProperties properties = {width: 3, height: 3, depth: 3};
    int result = setSize(shapeID, properties);
    // ...
}
```

- Use included record parameters that allow grouping and passing by name.

```ballerina
public type Point record {|
    int x;
    int y;
|};

public type ShapeProperties record {|
    int width;
    int height;
    int depth;
|};

public type Coordinates record {|
    Point p1;
    Point p2;
    Point p3;
|};

public function setCoordinates2(*Coordinates coordinates) returns int {
    // ...
}

public function setSize2(int shapeID, *ShapeProperties properties) returns int {
    // ...
}

public function compliantSolution2() {
    int shapeID = setCoordinates(p1 = {x: 1, y: 2},
        p2 = {x: 1, y: 2},
        p3 = {x: 1, y: 2});

    int result = setSize2(shapeID, {width: 3, height: 3, depth: 3});
    // ...
}
```

### Additional information
package com.github.kright

enum class MovementAxes(val x: Boolean, val y: Boolean, val z: Boolean) {
    NOTHING(false, false, false),
    X(true, false, false),
    Y(false, true, false),
    Z(false, false, true),
    XY(true, true, false),
    XZ(true, false, true),
    YZ(false, true, true),
    XYZ(true, true, true),
}

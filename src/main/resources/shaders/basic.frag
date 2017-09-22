#version 330

in float normalDot;
out vec4 fragColor;

uniform vec4 WorldAmbient;
uniform vec4 WorldLightColor;

void main() {
    fragColor = WorldAmbient + normalDot * WorldLightColor;
}
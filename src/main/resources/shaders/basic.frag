#version 330

in float normalDot;
out vec4 fragColor;

uniform vec4 WorldAmbient;
uniform vec4 WorldLightColor;

uniform vec4 ModelAmbient;

void main() {
    fragColor = WorldAmbient * ModelAmbient + normalDot * WorldLightColor;
}
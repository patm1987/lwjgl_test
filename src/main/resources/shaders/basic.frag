#version 330

in float normalDot;
out vec4 fragColor;

uniform vec4 worldAmbient;
uniform vec4 worldLightColor;

void main() {
    fragColor = worldAmbient + normalDot * worldLightColor;
}
#version 330

out vec4 fragColor;
uniform vec4 WorldAmbient;
uniform vec4 ModelAmbient;

void main() {
    fragColor = WorldAmbient * ModelAmbient;
}
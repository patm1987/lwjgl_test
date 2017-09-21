#version 330

in vec3 outNormal;
out vec4 fragColor;

void main() {
    fragColor.rgb = (outNormal + vec3(1.0, 1.0, 1.0)) / 2.0;
    fragColor.a = 1.0;
}
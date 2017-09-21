#version 330

in vec3 outNormal;
out vec4 fragColor;

void main() {
    fragColor.rgb = outNormal;
    fragColor.a = 1.0;
}
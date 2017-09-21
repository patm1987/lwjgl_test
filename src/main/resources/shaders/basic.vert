#version 330

layout(location=0)in vec3 position;
layout(location=1)in vec3 normal;
uniform mat4 ModelViewMatrix;

out vec3 outNormal;

void main() {
    gl_Position = ModelViewMatrix * vec4(position, 1.0);
    outNormal = normal;
}
#version 330

layout (location=0) in vec3 position;
uniform mat4 ModelViewMatrix;

void main() {
    gl_Position = ModelViewMatrix * vec4(position, 1.0);
}
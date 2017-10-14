#version 330

layout(location=0) in vec3 position;

uniform mat4 ViewProjectionMatrix;
uniform mat4 ModelMatrix;

void main() {
    mat4 modelViewProjectionMatrix = ViewProjectionMatrix * ModelMatrix;
    gl_Position = modelViewProjectionMatrix * vec4(position, 1.0);
}
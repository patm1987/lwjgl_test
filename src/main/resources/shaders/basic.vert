#version 330

layout(location=0)in vec3 position;
layout(location=1)in vec3 normal;
uniform mat4 ViewProjectionMatrix;
uniform mat4 ModelMatrix;
uniform vec3 WorldLightDirection;

out float normalDot;

void main() {
    mat4 modelViewProjectionMatrix = ViewProjectionMatrix * ModelMatrix;
    gl_Position = modelViewProjectionMatrix * vec4(position, 1.0);
    mat3 modelView3 = mat3(modelViewProjectionMatrix);
    mat3 lightMatrix = transpose(inverse(modelView3));
    vec3 transformedNormal = lightMatrix * normal;
    vec3 transformedLightDirection = modelView3 * WorldLightDirection;
    normalDot = dot(transformedNormal, transformedLightDirection);
}
#version 330

layout(location=0)in vec3 position;
layout(location=1)in vec3 normal;
uniform mat4 ModelViewMatrix;
uniform vec3 worldLightDirection;

out float normalDot;

void main() {
    gl_Position = ModelViewMatrix * vec4(position, 1.0);
    mat3 lightMatrix = transpose(inverse(mat3(ModelViewMatrix)));
    vec3 transformedNormal = lightMatrix * normal;
    normalDot = dot(transformedNormal, worldLightDirection);
}
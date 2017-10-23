#version 330

layout(triangles_adjacency) in;
layout(triangle_strip, max_vertices = 12) out;

uniform mat4 ViewProjectionMatrix;
uniform mat4 ModelMatrix;

uniform vec4 LightDirection;

/**
 * A triangle is "front facing" if it's pointing the opposite direction of the light vector
 */
bool frontFacing (vec3 v0, vec3 v1, vec3 v2) {
    vec3 e0 = v1 - v0;
    vec3 e1 = v2 - v0;

    vec3 dir = cross(e0, e1);
    float proj = dot(dir, LightDirection.xyz);

    return proj < 0;
}

/*
 * Projects a vertex from the light
 */
vec4 projectVertex(vec4 original) {
    return vec4(
        LightDirection.w * original.x - LightDirection.x,
        LightDirection.w * original.y - LightDirection.y,
        LightDirection.w * original.z - LightDirection.z,
        0);
}

/**
 * generates a thick edge from a pair of given points
 */
void generateEdge(vec4 v0, vec4 v1) {
    mat4 modelViewProjectionMatrix = ViewProjectionMatrix * ModelMatrix;

    gl_Position = modelViewProjectionMatrix * v0;
    EmitVertex();

    gl_Position = modelViewProjectionMatrix * v1;
    EmitVertex();

    gl_Position = modelViewProjectionMatrix * projectVertex(v0);
    EmitVertex();

    gl_Position = modelViewProjectionMatrix * projectVertex(v1);
    EmitVertex();

    EndPrimitive();
}

void main() {
    vec4 v0 = gl_in[0].gl_Position;
    vec4 v1 = gl_in[1].gl_Position;
    vec4 v2 = gl_in[2].gl_Position;
    vec4 v3 = gl_in[3].gl_Position;
    vec4 v4 = gl_in[4].gl_Position;
    vec4 v5 = gl_in[5].gl_Position;

    // make sure we're front facing
    if (frontFacing(v0.xyz, v2.xyz, v4.xyz)) {
        // generate an edge for every back facing neighbor
        if (!frontFacing(v0.xyz, v1.xyz, v2.xyz)) {
            generateEdge(v0, v2);
        }
        if (!frontFacing(v2.xyz, v3.xyz, v4.xyz)) {
            generateEdge(v2, v4);
        }
        if (!frontFacing(v4.xyz, v5.xyz, v0.xyz)) {
            generateEdge(v4, v0);
        }
    }
}

#version 330

layout(triangles_adjacency) in;
layout(triangle_strip, max_vertices = 12) out;

uniform float EdgeThickness;

/**
 * Figure out if a triangle is front facing. This is just the cross product ignoring Z
 */
bool frontFacing (vec2 v0, vec2 v1, vec2 v2) {
    return (v0.x * v1.y - v1.x * v0.y) + (v1.x * v2.y - v2.x * v1.y) + (v2.x * v0.y - v0.x * v2.y) > 0;
}

/**
 * generates a thick edge from a pair of given points
 */
void generateEdge(vec2 v0, vec2 v1) {
    vec2 edgeDir = normalize(v1-v0);
    vec2 edgeHeight = vec2(-edgeDir.y, edgeDir.x);

    gl_Position = vec4(v0, 0, 1);
    EmitVertex();

    gl_Position = vec4(v0 + edgeHeight, 0, 1);
    EmitVertex();

    gl_Position = vec4(v1, 0, 1);
    EmitVertex();

    gl_Position = vec4(v1 + edgeHeight, 0, 1);
    EmitVertex();

    EndPrimitive();
}

void main() {
    vec2 v0 = gl_in[0].gl_Position.xy;
    vec2 v1 = gl_in[1].gl_Position.xy;
    vec2 v2 = gl_in[2].gl_Position.xy;
    vec2 v3 = gl_in[3].gl_Position.xy;
    vec2 v4 = gl_in[4].gl_Position.xy;
    vec2 v5 = gl_in[5].gl_Position.xy;

    // make sure we're front facing
    if (frontFacing(v0, v2, v4)) {
        // generate an edge for every back facing neighbor
        if (!frontFacing(v0, v1, v2)) {
            generateEdge(v0, v2);
        }
        if (!frontFacing(v2, v3, v4)) {
            generateEdge(v2, v4);
        }
        if (!frontFacing(v4, v5, v0)) {
            generateEdge(v4, v0);
        }
    }
}

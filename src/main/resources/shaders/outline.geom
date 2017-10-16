#version 330

layout(triangles_adjacency) in;
layout(triangle_strip, max_vertices = 12) out;

uniform float EdgeThickness;

/**
 * Determine which way the cross product is pointing. This only consider's the z component
 */
bool frontFacing (vec2 v0, vec2 v1, vec2 v2) {
    vec2 e0 = v1 - v0;
    vec2 e1 = v2 - v0;

    return (e0.x * e1.y - e0.y * e1.x) > 0;
}

/**
 * generates a thick edge from a pair of given points
 */
void generateEdge(vec4 v0, vec4 v1) {
    vec2 edgeDir = normalize(v1.xy-v0.xy);
    vec2 edgeHeight = vec2(edgeDir.y, -edgeDir.x) * EdgeThickness;

    gl_Position = vec4(v0);
    EmitVertex();

    gl_Position = vec4(v0.xy + edgeHeight, v0.zw);
    EmitVertex();

    gl_Position = v1;
    EmitVertex();

    gl_Position = vec4(v1.xy + edgeHeight, v1.zw);
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
    if (frontFacing(v0.xy, v2.xy, v4.xy)) {
//        gl_Position = vec4(v0);
//        EmitVertex();
//        gl_Position = vec4(v2);
//        EmitVertex();
//        gl_Position = vec4(v4);
//        EmitVertex();

        // generate an edge for every back facing neighbor
        if (!frontFacing(v0.xy, v1.xy, v2.xy)) {
            generateEdge(v0, v2);
        }
        if (!frontFacing(v2.xy, v3.xy, v4.xy)) {
            generateEdge(v2, v4);
        }
        if (!frontFacing(v4.xy, v5.xy, v0.xy)) {
            generateEdge(v4, v0);
        }
    }
}
